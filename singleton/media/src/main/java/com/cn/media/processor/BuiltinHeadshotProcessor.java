package com.cn.media.processor;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 内置大头照处理：人像感知居中裁剪 + 缩放到标准尺寸。
 */
@Component
@Slf4j
public class BuiltinHeadshotProcessor {

    private static final int OUTPUT_SIZE = 512;

    public HeadshotResult process(InputStream imageStream) {
        try {
            BufferedImage source = ImageIO.read(imageStream);
            if (source == null) {
                return HeadshotResult.failed("无法解析图片");
            }
            int width = source.getWidth();
            int height = source.getHeight();
            if (width < 32 || height < 32) {
                return HeadshotResult.failed("图片尺寸过小");
            }

            int cropSize = Math.min(width, height);
            int cropX;
            int cropY;
            if (height > width) {
                cropX = (width - cropSize) / 2;
                cropY = Math.min((int) (height * 0.12), height - cropSize);
            } else {
                cropX = (width - cropSize) / 2;
                cropY = (height - cropSize) / 2;
            }

            BufferedImage cropped = source.getSubimage(cropX, cropY, cropSize, cropSize);
            BufferedImage output = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = output.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(cropped, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(output, "jpg", baos);

            JSONObject meta = new JSONObject();
            meta.put("cropX", cropX);
            meta.put("cropY", cropY);
            meta.put("cropSize", cropSize);
            meta.put("outputSize", OUTPUT_SIZE);
            meta.put("sourceWidth", width);
            meta.put("sourceHeight", height);

            return HeadshotResult.succeeded(baos.toByteArray(), meta);
        } catch (Exception e) {
            log.warn("内置大头照处理失败", e);
            return HeadshotResult.failed("处理失败: " + e.getMessage());
        }
    }

    public HeadshotResult process(byte[] imageBytes) {
        return process(new ByteArrayInputStream(imageBytes));
    }

    public record HeadshotResult(boolean success, byte[] imageBytes, JSONObject meta, String errorMessage) {
        static HeadshotResult succeeded(byte[] bytes, JSONObject meta) {
            return new HeadshotResult(true, bytes, meta, null);
        }

        static HeadshotResult failed(String message) {
            return new HeadshotResult(false, null, null, message);
        }
    }
}
