package com.cn.oss.service.impl;

import com.cn.common.configuration.CoverMediaProperties;
import com.cn.common.enums.FilePathEnum;
import com.cn.common.utils.MediaTranscodeUtil;
import com.cn.common.utils.UploadUtil;
import com.cn.oss.exceptions.UploadException;
import com.cn.oss.service.CoverUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 工作流封面上传：静态图直传，GIF/视频按需转码为 MP4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverUploadServiceImpl implements CoverUploadService {

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "webp", Set.of("image/webp"),
            "gif", Set.of("image/gif"),
            "mp4", Set.of("video/mp4"),
            "avi", Set.of("video/x-msvideo", "video/avi"),
            "mov", Set.of("video/quicktime", "video/mp4"),
            "webm", Set.of("video/webm")
    );

    private static final Set<String> STATIC_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ANIMATED_EXTENSIONS = Set.of("gif", "mp4", "avi", "mov", "webm");

    private final UploadUtil uploadUtil;
    private final MediaTranscodeUtil mediaTranscodeUtil;
    private final CoverMediaProperties coverMediaProperties;

    @Override
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadException("上传文件不能为空");
        }

        long size = file.getSize();
        if (size > coverMediaProperties.getMaxUploadBytes()) {
            throw new UploadException("文件大小超过限制. 最大允许: "
                    + coverMediaProperties.getMaxUploadBytes() / (1024 * 1024) + "MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        validateCoverType(extension, contentType);

        if (STATIC_IMAGE_EXTENSIONS.contains(extension)) {
            return uploadUtil.uploadFile(file, FilePathEnum.TEMP.getDec());
        }

        if (!ANIMATED_EXTENSIONS.contains(extension)) {
            throw new UploadException("不支持的封面媒体类型");
        }

        Path inputTemp = null;
        Path outputTemp = null;
        try {
            inputTemp = Files.createTempFile("cover-upload-", "." + extension);
            file.transferTo(inputTemp);

            MediaTranscodeUtil.MediaProbeResult probe = mediaTranscodeUtil.probe(inputTemp);
            if (!mediaTranscodeUtil.needsTranscode(probe, size)) {
                String mime = uploadUtil.resolveMimeType("file." + extension);
                return uploadUtil.uploadLocalFile(inputTemp, FilePathEnum.TEMP.getDec(), extension, mime);
            }

            outputTemp = Files.createTempFile("cover-output-", ".mp4");
            mediaTranscodeUtil.transcodeToCoverMp4(inputTemp, outputTemp);
            return uploadUtil.uploadLocalFile(outputTemp, FilePathEnum.TEMP.getDec(), "mp4", "video/mp4");
        } catch (UploadException e) {
            throw e;
        } catch (IOException e) {
            log.error("封面媒体处理失败", e);
            throw new UploadException(e.getMessage() != null ? e.getMessage() : "封面媒体处理失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UploadException("封面媒体处理被中断");
        } finally {
            deleteQuietly(inputTemp);
            deleteQuietly(outputTemp);
        }
    }

    private void validateCoverType(String extension, String contentType) {
        Set<String> allowedMimes = ALLOWED_TYPES.get(extension);
        if (allowedMimes == null) {
            throw new UploadException("不支持的文件类型: " + extension);
        }
        if (StringUtils.hasText(contentType)) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            boolean mimeOk = allowedMimes.stream().anyMatch(m -> m.equalsIgnoreCase(normalized));
            if (!mimeOk && !normalized.startsWith("image/") && !normalized.startsWith("video/")) {
                throw new UploadException("不支持的文件类型: " + contentType + " (扩展名: " + extension + ")");
            }
        }
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new UploadException("文件名缺少扩展名");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", path, e);
        }
    }
}
