package com.cn.oss.controller;


import com.cn.common.exceptions.OssException;
import com.cn.common.msg.Result;
import com.cn.oss.exceptions.UploadException;
import com.cn.oss.service.CoverUploadService;
import com.cn.oss.service.OssMediaService;
import com.cn.oss.service.OssService;
import com.cn.common.annotations.RateLimit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * OSS控制器
 *
 * @author 时间海 @github dulaiduwang003
 * @email 2074055628@qq.com
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;
    private final CoverUploadService coverUploadService;
    private final OssMediaService ossMediaService;


    /**
     * Upload file result.
     *
     * @param file the file
     * @return the result
     */
    @PostMapping(value = "/upload/file", consumes = "multipart/form-data")
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.USER, message = "文件上传过于频繁，请稍后再试")
    public Result uploadFile(@Valid @NotNull(message = "上传文件不能为空") final MultipartFile file) {
        try {
            return Result.data(ossService.uploadFile(file));
        } catch (OssException | UploadException ex) {
            return Result.error(ex.getMessage());
        }

    }

    /**
     * 工作流封面上传（支持图片/GIF/视频，超大或超长媒体自动转码）
     */
    @PostMapping(value = "/upload/cover", consumes = "multipart/form-data")
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.USER, message = "文件上传过于频繁，请稍后再试")
    public Result uploadCover(@Valid @NotNull(message = "上传文件不能为空") final MultipartFile file) {
        try {
            return Result.data(coverUploadService.uploadCover(file));
        } catch (UploadException ex) {
            return Result.error(ex.getMessage());
        }
    }

    /**
     * 同源媒体代理（主要用于视频封面，避免 OSS CORS + Range 问题）
     */
    @GetMapping("/media")
    public ResponseEntity<byte[]> streamMedia(@RequestParam("key") final String key) {
        try {
            return ossMediaService.streamMedia(key);
        } catch (UploadException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
