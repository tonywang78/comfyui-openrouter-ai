package com.cn.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.cn.common.configuration.AliConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * 上传本地文件到 OSS，返回签名 URL。
     */
    public String uploadLocalFile(Path localFile, String path, String extension, String contentType) {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        try (InputStream inputStream = Files.newInputStream(localFile)) {
            String objectKey = path + "/" + UUID.randomUUID() + "." + extension;
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            objectMetadata.setContentLength(Files.size(localFile));
            ossClient.putObject(oss.getBucketName(), objectKey, inputStream, objectMetadata);
            return signObjectKey(ossClient, oss.getBucketName(), objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload local file", e);
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

    /**
     * 打开 OSS 对象流，调用方须在 try-with-resources 中关闭。
     */
    public OssObjectStream openObject(String objectKey) {
        AliConfiguration.Oss oss = aliConfiguration.getOss();
        OSS ossClient = createOssClient();
        OSSObject object = ossClient.getObject(oss.getBucketName(), objectKey);
        ObjectMetadata metadata = object.getObjectMetadata();
        String contentType = metadata.getContentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = getMimeType(objectKey);
        }
        return new OssObjectStream(ossClient, object, contentType, metadata.getContentLength());
    }

    public static final class OssObjectStream implements AutoCloseable {
        private final OSS ossClient;
        private final OSSObject object;
        private final String contentType;
        private final long contentLength;

        private OssObjectStream(OSS ossClient, OSSObject object, String contentType, long contentLength) {
            this.ossClient = ossClient;
            this.object = object;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public InputStream getInputStream() {
            return object.getObjectContent();
        }

        public String getContentType() {
            return contentType;
        }

        public long getContentLength() {
            return contentLength;
        }

        @Override
        public void close() throws IOException {
            try {
                object.close();
            } finally {
                ossClient.shutdown();
            }
        }
    }

    public boolean isAllowedMediaObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey) || objectKey.contains("..")) {
            return false;
        }
        return objectKey.startsWith("TEMP/")
                || objectKey.startsWith("COMFYUI/")
                || objectKey.startsWith("USER/");
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

    public String resolveMimeType(String fileName) {
        return getMimeType(fileName);
    }

    private String getMimeType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lower.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (lower.endsWith(".mov")) {
            return "video/quicktime";
        } else if (lower.endsWith(".webm")) {
            return "video/webm";
        } else if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lower.endsWith(".wav")) {
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
