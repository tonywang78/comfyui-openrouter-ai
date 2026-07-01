package com.cn.media.service;

import com.cn.common.base.BasePage;
import com.cn.media.dto.*;
import com.cn.media.vo.MediaPickerItemVo;
import com.cn.media.vo.MediaVariantVo;
import com.cn.media.vo.UserMediaVo;
import org.springframework.web.multipart.MultipartFile;

public interface MediaLibraryService {

    UserMediaVo upload(MultipartFile file, String name);

    BasePage<UserMediaVo> getPage(Long pageNum, String mediaType, String keyword);

    UserMediaVo getDetail(Long mediaId);

    void update(UpdateMediaDto dto);

    void delete(Long mediaId);

    UserMediaVo importFromWork(ImportFromWorkDto dto);

    MediaVariantVo createBuiltinVariant(Long mediaId);

    MediaVariantVo createComfyuiVariant(ComfyuiVariantDto dto);

    MediaVariantVo getVariantStatus(Long variantId);

    BasePage<MediaPickerItemVo> getPicker(Long pageNum, String mediaType);

    String createTaskFromMedia(CreateTaskFromMediaDto dto);
}
