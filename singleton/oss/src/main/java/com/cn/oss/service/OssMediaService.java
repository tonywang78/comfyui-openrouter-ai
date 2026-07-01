package com.cn.oss.service;

import org.springframework.http.ResponseEntity;

public interface OssMediaService {

    ResponseEntity<byte[]> streamMedia(String objectKey);
}
