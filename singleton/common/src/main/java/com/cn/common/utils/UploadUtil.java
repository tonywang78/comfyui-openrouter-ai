package com.cn.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.cn.common.configuration.AliConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * OSS 上传工具：对象私有存储，通过签名 URL 访问。
 */
@Component
@SuppressWarnings("all")
@Slf4j
@RequiredArgsConstructor
public class UploadUtil {

    private static final long DEFAULT_SIGNED_URL_EXPIRE_SECONDS = 3600L;

    private final AliConfiguration aliConfiguration;

    public String uploadFile(final MultipartFile file, final String path) {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        try (InputStream inputStream = file.getInputStream()) {
            String originalFileName = file.getOriginalFilename();
            assert originalFileName != null;
            String fileName = UUID.randomUUID() + originalFileName.substring(originalFileName.lastIndexOf('.'));
            String objectKey = path + "/" + fileName;

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(file.getContentType());
            ossClient.putObject(oss.getBucketName(), objectKey, inputStream, objectMetadata);
            return signObjectKey(ossClient, oss.getBucketName(), objectKey);
        } catch (IOException e) {
            throw new OSSException();
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 从外部 URL 拉取并上传到 OSS，返回 objectKey（用于持久化存储）。
     */
    public String uploadUrl(String imageUrl, String path) {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        try {
            URL url = new URL(imageUrl);
            String pathWithoutQuery = url.getPath();
            String fileNameWithExt = null;

            String query = url.getQuery();
            if (query != null && query.contains("filename=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("filename=")) {
                        fileNameWithExt = URLDecoder.decode(param.substring("filename=".length()), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (fileNameWithExt == null) {
                fileNameWithExt = pathWithoutQuery.substring(pathWithoutQuery.lastIndexOf('/') + 1);
            }

            String objectKey = path + "/" + UUID.randomUUID() + "." + getFileExtension(fileNameWithExt);

            try (InputStream inputStream = url.openStream()) {
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentType(getMimeType(fileNameWithExt));
                ossClient.putObject(oss.getBucketName(), objectKey, inputStream, objectMetadata);
            }
            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload media from URL: " + imageUrl, e);
        } finally {
            ossClient.shutdown();
        }
    }

    public String uploadBase64Image(String base64OrDataUri, String path) {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        try {
            String base64Data = base64OrDataUri;
            if (base64OrDataUri != null && base64OrDataUri.startsWith("data:")) {
                int commaIdx = base64OrDataUri.indexOf(',');
                if (commaIdx > 0) {
                    String meta = base64OrDataUri.substring(5, commaIdx);
                    String contentType = meta.contains(";") ? meta.substring(0, meta.indexOf(';')) : meta;
                    if (!"image/png".equalsIgnoreCase(contentType)) {
                        throw new IllegalArgumentException("Only PNG base64 is supported");
                    }
                    base64Data = base64OrDataUri.substring(commaIdx + 1);
                }
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String objectKey = path + "/" + UUID.randomUUID() + ".png";

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType("image/png");
            objectMetadata.setContentLength(decodedBytes.length);

            ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
            ossClient.putObject(oss.getBucketName(), objectKey, bais, objectMetadata);
            return signObjectKey(ossClient, oss.getBucketName(), objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload base64 image (PNG only)", e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 将本 bucket 内的 objectKey 或历史 OSS URL 转为新的签名 URL；外部 URL 原样返回。
     */
    public String toSignedUrl(String urlOrObjectKey) {
        if (!StringUtils.hasText(urlOrObjectKey) || !isOwnOssResource(urlOrObjectKey)) {
            return urlOrObjectKey;
        }
        String objectKey = extractObjectKey(urlOrObjectKey);
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        try {
            return signObjectKey(ossClient, oss.getBucketName(), objectKey);
        } finally {
            ossClient.shutdown();
        }
    }

    public boolean isOwnOssResource(String urlOrObjectKey) {
        if (!StringUtils.hasText(urlOrObjectKey)) {
            return false;
        }
        if (!urlOrObjectKey.startsWith("http://") && !urlOrObjectKey.startsWith("https://")) {
            return urlOrObjectKey.startsWith("TEMP/")
                    || urlOrObjectKey.startsWith("COMFYUI/")
                    || urlOrObjectKey.startsWith("USER/");
        }
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        if (StringUtils.hasText(oss.getDomain())) {
            String domain = oss.getDomain().replaceAll("/$", "");
            if (urlOrObjectKey.startsWith(domain)) {
                return true;
            }
        }
        return StringUtils.hasText(oss.getBucketName()) && urlOrObjectKey.contains(oss.getBucketName() + ".");
    }

    public String extractObjectKey(String urlOrObjectKey) {
        if (!StringUtils.hasText(urlOrObjectKey)) {
            return urlOrObjectKey;
        }
        if (!urlOrObjectKey.startsWith("http://") && !urlOrObjectKey.startsWith("https://")) {
            return urlOrObjectKey;
        }
        try {
            URL parsed = new URL(urlOrObjectKey);
            String path = parsed.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("无法解析 OSS URL，原样返回: {}", urlOrObjectKey);
            return urlOrObjectKey;
        }
    }

    private OSS createOssClient() {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        AliConfiguration.Certified certified = aliConfiguration.getCertified();
        return new OSSClientBuilder().build(oss.getEndpoint(), certified.getAccessKey(), certified.getSecretKey());
    }

    private String signObjectKey(OSS ossClient, String bucketName, String objectKey) {
        Date expiration = new Date(System.currentTimeMillis() + signedUrlExpireSeconds() * 1000L);
        URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
        return signedUrl.toString();
    }

    private long signedUrlExpireSeconds() {
        Long configured = aliConfiguration.getOss().getSignedUrlExpireSeconds();
        return configured != null && configured > 0 ? configured : DEFAULT_SIGNED_URL_EXPIRE_SECONDS;
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (fileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (fileName.endsWith(".wav")) {
            return "audio/x-wav";
        }
        return "application/octet-stream";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return "";
        }
        return fileName.substring(lastIndexOfDot + 1);
    }
}
