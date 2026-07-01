package com.cn.media.controller;

import com.cn.common.annotations.RateLimit;
import com.cn.common.msg.Result;
import com.cn.media.dto.*;
import com.cn.media.exceptions.MediaException;
import com.cn.media.service.MediaLibraryService;
import com.cn.oss.exceptions.UploadException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Validated
public class MediaController {

    private final MediaLibraryService mediaLibraryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.USER, message = "上传过于频繁，请稍后再试")
    public Result upload(@RequestParam("file") @NotNull MultipartFile file,
                         @RequestParam(value = "name", required = false) String name) {
        try {
            return Result.data(mediaLibraryService.upload(file, name));
        } catch (UploadException | MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/page")
    public Result getPage(@RequestParam(defaultValue = "1") @Min(1) Long page,
                          @RequestParam(required = false) String mediaType,
                          @RequestParam(required = false) String keyword) {
        return Result.data(mediaLibraryService.getPage(page, mediaType, keyword));
    }

    @GetMapping("/detail")
    public Result getDetail(@RequestParam @NotNull Long mediaId) {
        try {
            return Result.data(mediaLibraryService.getDetail(mediaId));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Valid UpdateMediaDto dto) {
        try {
            mediaLibraryService.update(dto);
            return Result.ok("更新成功");
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.USER, message = "删除过于频繁，请稍后再试")
    public Result delete(@RequestBody @Valid MediaIdDto dto) {
        try {
            mediaLibraryService.delete(dto.getMediaId());
            return Result.ok("删除成功");
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/import-from-work")
    public Result importFromWork(@RequestBody @Valid ImportFromWorkDto dto) {
        try {
            return Result.data(mediaLibraryService.importFromWork(dto));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/variant/builtin")
    public Result createBuiltinVariant(@RequestBody @Valid MediaIdDto dto) {
        try {
            return Result.data(mediaLibraryService.createBuiltinVariant(dto.getMediaId()));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/variant/comfyui")
    public Result createComfyuiVariant(@RequestBody @Valid ComfyuiVariantDto dto) {
        try {
            return Result.data(mediaLibraryService.createComfyuiVariant(dto));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/variant/status")
    public Result getVariantStatus(@RequestParam @NotNull Long variantId) {
        try {
            return Result.data(mediaLibraryService.getVariantStatus(variantId));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/picker")
    public Result getPicker(@RequestParam(defaultValue = "1") @Min(1) Long page,
                            @RequestParam(required = false) String mediaType) {
        return Result.data(mediaLibraryService.getPicker(page, mediaType));
    }

    @PostMapping("/create-task")
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.USER, message = "任务提交过于频繁，请稍后再试")
    public Result createTask(@RequestBody @Valid CreateTaskFromMediaDto dto) {
        try {
            return Result.data(mediaLibraryService.createTaskFromMedia(dto));
        } catch (MediaException ex) {
            return Result.error(ex.getMessage());
        }
    }
}
