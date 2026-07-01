package com.cn.oss.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 工作流封面上传服务
 */
public interface CoverUploadService {

    String uploadCover(MultipartFile file);
}
