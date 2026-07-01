package com.cn.oss.service.impl;

import com.cn.common.utils.UploadUtil;
import com.cn.oss.exceptions.UploadException;
import com.cn.oss.service.OssMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 同源媒体代理：避免浏览器直连 OSS 视频时的 CORS / Range 问题。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OssMediaServiceImpl implements OssMediaService {

    /** 封面视频经转码后体积较小，限制代理读取上限防止滥用 */
    private static final long MAX_PROXY_BYTES = 20L * 1024 * 1024;

    private final UploadUtil uploadUtil;

    @Override
    public ResponseEntity<byte[]> streamMedia(String objectKey) {
        if (!uploadUtil.isAllowedMediaObjectKey(objectKey)) {
            throw new UploadException("不允许访问该媒体资源");
        }

        try (UploadUtil.OssObjectStream objectStream = uploadUtil.openObject(objectKey)) {
            byte[] data = objectStream.getInputStream().readAllBytes();
            if (data.length > MAX_PROXY_BYTES) {
                throw new UploadException("媒体文件过大，无法通过代理访问");
            }

            String contentType = objectStream.getContentType();
            if (!StringUtils.hasText(contentType)) {
                contentType = uploadUtil.resolveMimeType(objectKey);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(data.length)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(data);
        } catch (UploadException e) {
            throw e;
        } catch (IOException e) {
            log.warn("读取 OSS 媒体失败: {}", objectKey, e);
            throw new UploadException("媒体资源不存在或无法读取");
        } catch (Exception e) {
            log.warn("打开 OSS 媒体失败: {}", objectKey, e);
            throw new UploadException("媒体资源不存在或无法读取");
        }
    }
}
