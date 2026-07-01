package com.cn.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cn.common.configuration.CoverMediaProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 封面媒体 ffprobe/ffmpeg 转码工具
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MediaTranscodeUtil {

    private final CoverMediaProperties coverMediaProperties;

    public MediaProbeResult probe(Path inputFile) throws IOException, InterruptedException {
        ensureFfmpegAvailable();

        List<String> command = List.of(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_streams",
                "-show_format",
                inputFile.toAbsolutePath().toString()
        );

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().reduce("", (a, b) -> a + b);
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("ffprobe 超时");
        }
        if (process.exitValue() != 0) {
            throw new IOException("ffprobe 失败: " + output);
        }

        JSONObject root = JSON.parseObject(output);
        JSONObject format = root.getJSONObject("format");
        double duration = format != null ? format.getDoubleValue("duration") : 0D;

        int width = 0;
        int height = 0;
        if (root.getJSONArray("streams") != null) {
            for (Object streamObj : root.getJSONArray("streams")) {
                JSONObject stream = (JSONObject) streamObj;
                if ("video".equalsIgnoreCase(stream.getString("codec_type"))) {
                    width = stream.getIntValue("width");
                    height = stream.getIntValue("height");
                    break;
                }
            }
        }

        return new MediaProbeResult(duration, width, height);
    }

    public boolean needsTranscode(MediaProbeResult probe, long fileSizeBytes) {
        CoverMediaProperties config = coverMediaProperties;
        if (fileSizeBytes > config.getTranscodeThresholdBytes()) {
            return true;
        }
        if (probe.getDuration() > config.getMaxDurationSeconds()) {
            return true;
        }
        return Math.max(probe.getWidth(), probe.getHeight()) > config.getMaxLongEdge();
    }

    public void transcodeToCoverMp4(Path inputFile, Path outputFile) throws IOException, InterruptedException {
        ensureFfmpegAvailable();

        CoverMediaProperties config = coverMediaProperties;
        int maxEdge = config.getMaxLongEdge();
        int maxDuration = config.getMaxDurationSeconds();

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(inputFile.toAbsolutePath().toString());
        command.add("-t");
        command.add(String.valueOf(maxDuration));
        command.add("-vf");
        command.add("scale='min(" + maxEdge + ",iw)':-2");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("28");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-an");
        command.add("-movflags");
        command.add("+faststart");
        command.add(outputFile.toAbsolutePath().toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().reduce((a, b) -> a + System.lineSeparator() + b).orElse("");
        }

        if (!process.waitFor(config.getTranscodeTimeoutSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("ffmpeg 转码超时");
        }
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg 转码失败: " + output);
        }
        if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
            throw new IOException("ffmpeg 未生成有效输出文件");
        }
    }

    public void ensureFfmpegAvailable() throws IOException {
        if (!isCommandAvailable("ffmpeg") || !isCommandAvailable("ffprobe")) {
            throw new IOException("服务器未安装 ffmpeg/ffprobe，无法处理封面媒体");
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Getter
    public static class MediaProbeResult {
        private final double duration;
        private final int width;
        private final int height;

        public MediaProbeResult(double duration, int width, int height) {
            this.duration = duration;
            this.width = width;
            this.height = height;
        }
    }
}
