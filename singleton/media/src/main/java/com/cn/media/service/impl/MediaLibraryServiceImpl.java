package com.cn.media.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn.comfyui.dto.SubmitTaskDto;
import com.cn.comfyui.model.TaskNodeContainer;
import com.cn.comfyui.service.WorkflowService;
import com.cn.comfyui.vo.WorkflowInterfaceVo;
import com.cn.common.base.BasePage;
import com.cn.common.configuration.AliConfiguration;
import com.cn.common.entity.MediaStandardizationConfig;
import com.cn.common.entity.UserMedia;
import com.cn.common.entity.UserMediaVariant;
import com.cn.common.entity.WorkflowResult;
import com.cn.common.enums.*;
import com.cn.common.mapper.MediaStandardizationConfigMapper;
import com.cn.common.mapper.UserMediaMapper;
import com.cn.common.mapper.UserMediaVariantMapper;
import com.cn.common.mapper.WorkflowResultMapper;
import com.cn.common.utils.UploadUtil;
import com.cn.common.utils.UserUtils;
import com.cn.media.dto.*;
import com.cn.media.exceptions.MediaException;
import com.cn.media.processor.BuiltinHeadshotProcessor;
import com.cn.media.service.MediaLibraryService;
import com.cn.media.util.MediaObjectKeyUtil;
import com.cn.media.vo.MediaPickerItemVo;
import com.cn.media.vo.MediaVariantVo;
import com.cn.media.vo.UserMediaVo;
import com.cn.oss.exceptions.UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaLibraryServiceImpl implements MediaLibraryService {

    private final UserMediaMapper userMediaMapper;
    private final UserMediaVariantMapper variantMapper;
    private final MediaStandardizationConfigMapper configMapper;
    private final WorkflowResultMapper workflowResultMapper;
    private final UploadUtil uploadUtil;
    private final AliConfiguration aliConfiguration;
    private final BuiltinHeadshotProcessor headshotProcessor;
    private final WorkflowService workflowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMediaVo upload(MultipartFile file, String name) {
        Long userId = UserUtils.getCurrentLoginId();
        validateUploadFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String contentHash = computeHash(file);
        String displayName = StringUtils.hasText(name) ? name : stripExtension(originalFilename);

        UserMedia media = new UserMedia()
                .setUserId(userId)
                .setName(displayName)
                .setMimeType(file.getContentType())
                .setFileSize(file.getSize())
                .setSource(UserMediaSourceEnum.UPLOAD.getDec())
                .setStatus(1)
                .setContentHash(contentHash);
        media.setMediaType(resolveMediaType(file.getContentType(), extension));
        media.setObjectKey(pendingObjectKey(userId));
        userMediaMapper.insert(media);

        String objectKey = MediaObjectKeyUtil.originalKey(userId, media.getId(), extension);
        uploadUtil.uploadFileToKey(file, objectKey);
        media.setObjectKey(objectKey);

        fillImageDimensions(file, media);
        userMediaMapper.updateById(media);

        return toMediaVo(media, List.of());
    }

    @Override
    public BasePage<UserMediaVo> getPage(Long pageNum, String mediaType, String keyword) {
        Long userId = UserUtils.getCurrentLoginId();
        LambdaQueryWrapper<UserMedia> wrapper = new LambdaQueryWrapper<UserMedia>()
                .eq(UserMedia::getUserId, userId)
                .eq(UserMedia::getStatus, 1)
                .orderByDesc(UserMedia::getCreateTime);
        if (StringUtils.hasText(mediaType)) {
            wrapper.eq(UserMedia::getMediaType, mediaType.toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserMedia::getName, keyword);
        }

        IPage<UserMedia> page = userMediaMapper.selectPage(new Page<>(pageNum, 20), wrapper);
        List<UserMediaVo> items = page.getRecords().stream()
                .map(m -> toMediaVo(m, loadVariants(m.getId(), userId)))
                .toList();
        return new BasePage<UserMediaVo>().setItems(items).setTotal(page.getTotal());
    }

    @Override
    public UserMediaVo getDetail(Long mediaId) {
        UserMedia media = requireOwnedMedia(mediaId);
        return toMediaVo(media, loadVariants(mediaId, media.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateMediaDto dto) {
        UserMedia media = requireOwnedMedia(dto.getMediaId());
        if (StringUtils.hasText(dto.getName())) {
            media.setName(dto.getName());
        }
        if (dto.getTags() != null) {
            media.setTags(dto.getTags());
        }
        userMediaMapper.updateById(media);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long mediaId) {
        UserMedia media = requireOwnedMedia(mediaId);
        media.setStatus(0);
        userMediaMapper.updateById(media);

        List<UserMediaVariant> variants = variantMapper.selectList(
                new LambdaQueryWrapper<UserMediaVariant>().eq(UserMediaVariant::getMediaId, mediaId));
        for (UserMediaVariant variant : variants) {
            uploadUtil.deleteObject(variant.getObjectKey());
        }
        uploadUtil.deleteObject(media.getObjectKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMediaVo importFromWork(ImportFromWorkDto dto) {
        Long userId = UserUtils.getCurrentLoginId();
        WorkflowResult work = workflowResultMapper.selectOne(new LambdaQueryWrapper<WorkflowResult>()
                .eq(WorkflowResult::getId, dto.getWorkflowResultId())
                .eq(WorkflowResult::getUserId, userId));
        if (work == null || !StringUtils.hasText(work.getUrl())) {
            throw new MediaException("作品不存在或无可导入文件");
        }
        if (!UserMediaTypeEnum.IMAGE.getDec().equals(work.getType())
                && !UserMediaTypeEnum.VIDEO.getDec().equals(work.getType())) {
            throw new MediaException("仅支持导入图片或视频作品");
        }

        String sourceKey = uploadUtil.extractObjectKey(work.getUrl());
        String extension = extractExtension(sourceKey);
        String name = StringUtils.hasText(dto.getName()) ? dto.getName() : work.getWorkflowName();

        UserMedia media = new UserMedia()
                .setUserId(userId)
                .setName(name)
                .setMediaType(work.getType())
                .setMimeType(uploadUtil.resolveMimeType(sourceKey))
                .setSource(UserMediaSourceEnum.FROM_WORK.getDec())
                .setSourceRefId(work.getId())
                .setStatus(1)
                .setObjectKey(pendingObjectKey(userId));
        userMediaMapper.insert(media);

        String destKey = MediaObjectKeyUtil.originalKey(userId, media.getId(), extension);
        uploadUtil.copyObject(sourceKey, destKey);
        media.setObjectKey(destKey);
        media.setFileSize(0L);
        userMediaMapper.updateById(media);

        return toMediaVo(media, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaVariantVo createBuiltinVariant(Long mediaId) {
        UserMedia media = requireOwnedMedia(mediaId);
        if (!UserMediaTypeEnum.IMAGE.getDec().equals(media.getMediaType())) {
            throw new MediaException("仅图片素材支持内置大头照处理");
        }

        UserMediaVariant existing = findVariant(mediaId, MediaVariantTypeEnum.HEADSHOT_BUILTIN.getDec(),
                MediaVariantProcessorEnum.BUILTIN.getDec());
        if (existing != null) {
            if (MediaVariantStatusEnum.SUCCEEDED.getDec().equals(existing.getStatus())) {
                return toVariantVo(existing);
            }
            if (MediaVariantStatusEnum.PROCESSING.getDec().equals(existing.getStatus())) {
                return toVariantVo(existing);
            }
            if (MediaVariantStatusEnum.FAILED.getDec().equals(existing.getStatus())) {
                variantMapper.deleteById(existing.getId());
            }
        }

        UserMediaVariant variant = new UserMediaVariant()
                .setMediaId(mediaId)
                .setUserId(media.getUserId())
                .setVariantType(MediaVariantTypeEnum.HEADSHOT_BUILTIN.getDec())
                .setProcessor(MediaVariantProcessorEnum.BUILTIN.getDec())
                .setStatus(MediaVariantStatusEnum.PROCESSING.getDec());
        variantMapper.insert(variant);

        try (UploadUtil.OssObjectStream stream = uploadUtil.openObject(media.getObjectKey())) {
            BuiltinHeadshotProcessor.HeadshotResult result = headshotProcessor.process(stream.getInputStream());
            if (!result.success()) {
                variant.setStatus(MediaVariantStatusEnum.FAILED.getDec());
                variant.setMeta(new JSONObject().fluentPut("error", result.errorMessage()));
                variantMapper.updateById(variant);
                throw new MediaException(result.errorMessage());
            }
            String variantKey = MediaObjectKeyUtil.variantKey(media.getUserId(), mediaId,
                    MediaVariantTypeEnum.HEADSHOT_BUILTIN.getDec(), "jpg");
            uploadUtil.uploadBytes(result.imageBytes(), variantKey, "image/jpeg");
            variant.setObjectKey(variantKey);
            variant.setStatus(MediaVariantStatusEnum.SUCCEEDED.getDec());
            variant.setMeta(result.meta());
            variantMapper.updateById(variant);
            return toVariantVo(variant);
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            variant.setStatus(MediaVariantStatusEnum.FAILED.getDec());
            variant.setMeta(new JSONObject().fluentPut("error", e.getMessage()));
            variantMapper.updateById(variant);
            throw new MediaException("内置大头照处理失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaVariantVo createComfyuiVariant(ComfyuiVariantDto dto) {
        UserMedia media = requireOwnedMedia(dto.getMediaId());
        if (!UserMediaTypeEnum.IMAGE.getDec().equals(media.getMediaType())) {
            throw new MediaException("仅图片素材支持 ComfyUI 精修");
        }

        Long workflowId = dto.getWorkflowId();
        if (workflowId == null) {
            workflowId = resolveComfyuiWorkflowId(MediaVariantTypeEnum.HEADSHOT_COMFYUI.getDec());
        }
        if (workflowId == null) {
            throw new MediaException("未配置精修大头照工作流，请联系管理员");
        }

        UserMediaVariant existing = findVariant(dto.getMediaId(), MediaVariantTypeEnum.HEADSHOT_COMFYUI.getDec(),
                MediaVariantProcessorEnum.COMFYUI.getDec());
        if (existing != null) {
            if (MediaVariantStatusEnum.SUCCEEDED.getDec().equals(existing.getStatus())) {
                return toVariantVo(existing);
            }
            if (MediaVariantStatusEnum.PROCESSING.getDec().equals(existing.getStatus())) {
                return toVariantVo(existing);
            }
            if (MediaVariantStatusEnum.FAILED.getDec().equals(existing.getStatus())) {
                variantMapper.deleteById(existing.getId());
            }
        }

        String inputUrl = resolveInputUrlForComfyui(media, dto.getVariantId());
        WorkflowInterfaceVo workflowInterface = workflowService.getWorkflowInterface(workflowId);
        TaskNodeContainer imageNode = findFirstUploadNode(workflowInterface, true);
        if (imageNode == null) {
            throw new MediaException("该工作流没有图片上传字段，无法用于精修");
        }
        imageNode.setNodeValue(inputUrl);

        UserMediaVariant variant = new UserMediaVariant()
                .setMediaId(dto.getMediaId())
                .setUserId(media.getUserId())
                .setVariantType(MediaVariantTypeEnum.HEADSHOT_COMFYUI.getDec())
                .setProcessor(MediaVariantProcessorEnum.COMFYUI.getDec())
                .setWorkflowId(workflowId)
                .setStatus(MediaVariantStatusEnum.PROCESSING.getDec());
        variantMapper.insert(variant);

        SubmitTaskDto submitDto = new SubmitTaskDto()
                .setWorkflowId(workflowId)
                .setNodeContainer(List.of(imageNode))
                .setMediaVariantId(variant.getId());
        String taskId = workflowService.submitTask(submitDto);

        variant.setTaskId(taskId);
        variantMapper.updateById(variant);
        return toVariantVo(variant);
    }

    @Override
    public MediaVariantVo getVariantStatus(Long variantId) {
        Long userId = UserUtils.getCurrentLoginId();
        UserMediaVariant variant = variantMapper.selectOne(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getId, variantId)
                .eq(UserMediaVariant::getUserId, userId));
        if (variant == null) {
            throw new MediaException("衍生记录不存在");
        }
        return toVariantVo(variant);
    }

    @Override
    public BasePage<MediaPickerItemVo> getPicker(Long pageNum, String mediaType) {
        Long userId = UserUtils.getCurrentLoginId();
        LambdaQueryWrapper<UserMedia> wrapper = new LambdaQueryWrapper<UserMedia>()
                .eq(UserMedia::getUserId, userId)
                .eq(UserMedia::getStatus, 1)
                .orderByDesc(UserMedia::getCreateTime);
        if (StringUtils.hasText(mediaType)) {
            wrapper.eq(UserMedia::getMediaType, mediaType.toUpperCase(Locale.ROOT));
        } else {
            wrapper.in(UserMedia::getMediaType, UserMediaTypeEnum.IMAGE.getDec(), UserMediaTypeEnum.VIDEO.getDec());
        }

        IPage<UserMedia> page = userMediaMapper.selectPage(new Page<>(pageNum, 30), wrapper);
        List<MediaPickerItemVo> items = new ArrayList<>();
        for (UserMedia media : page.getRecords()) {
            items.add(new MediaPickerItemVo()
                    .setMediaId(media.getId())
                    .setName(media.getName())
                    .setMediaType(media.getMediaType())
                    .setUrl(uploadUtil.toSignedUrl(media.getObjectKey()))
                    .setVariantLabel("原图"));

            List<UserMediaVariant> variants = loadVariants(media.getId(), userId);
            for (UserMediaVariant variant : variants) {
                if (!MediaVariantStatusEnum.SUCCEEDED.getDec().equals(variant.getStatus())
                        || !StringUtils.hasText(variant.getObjectKey())) {
                    continue;
                }
                items.add(new MediaPickerItemVo()
                        .setMediaId(media.getId())
                        .setName(media.getName() + " - " + variantLabel(variant.getVariantType()))
                        .setMediaType(media.getMediaType())
                        .setUrl(uploadUtil.toSignedUrl(variant.getObjectKey()))
                        .setVariantId(variant.getId())
                        .setVariantType(variant.getVariantType())
                        .setVariantLabel(variantLabel(variant.getVariantType())));
            }
        }
        return new BasePage<MediaPickerItemVo>().setItems(items).setTotal(page.getTotal());
    }

    @Override
    public String createTaskFromMedia(CreateTaskFromMediaDto dto) {
        UserMedia media = requireOwnedMedia(dto.getMediaId());
        String inputUrl;
        if (dto.getVariantId() != null) {
            UserMediaVariant variant = variantMapper.selectOne(new LambdaQueryWrapper<UserMediaVariant>()
                    .eq(UserMediaVariant::getId, dto.getVariantId())
                    .eq(UserMediaVariant::getMediaId, dto.getMediaId())
                    .eq(UserMediaVariant::getUserId, media.getUserId()));
            if (variant == null || !MediaVariantStatusEnum.SUCCEEDED.getDec().equals(variant.getStatus())) {
                throw new MediaException("所选衍生版本不可用");
            }
            inputUrl = uploadUtil.toSignedUrl(variant.getObjectKey());
        } else {
            inputUrl = uploadUtil.toSignedUrl(media.getObjectKey());
        }

        WorkflowInterfaceVo workflowInterface = workflowService.getWorkflowInterface(dto.getWorkflowId());
        boolean isVideo = UserMediaTypeEnum.VIDEO.getDec().equals(media.getMediaType());
        TaskNodeContainer uploadNode = findFirstUploadNode(workflowInterface, !isVideo);
        if (uploadNode == null) {
            uploadNode = findFirstUploadNode(workflowInterface, isVideo);
        }
        if (uploadNode == null) {
            throw new MediaException("该工作流没有匹配的上传字段");
        }
        uploadNode.setNodeValue(inputUrl);

        List<TaskNodeContainer> nodes = new ArrayList<>();
        nodes.add(uploadNode);
        if (dto.getExtraNodes() != null) {
            nodes.addAll(dto.getExtraNodes());
        }

        SubmitTaskDto submitDto = new SubmitTaskDto()
                .setWorkflowId(dto.getWorkflowId())
                .setNodeContainer(nodes);
        return workflowService.submitTask(submitDto);
    }

    private UserMedia requireOwnedMedia(Long mediaId) {
        Long userId = UserUtils.getCurrentLoginId();
        UserMedia media = userMediaMapper.selectOne(new LambdaQueryWrapper<UserMedia>()
                .eq(UserMedia::getId, mediaId)
                .eq(UserMedia::getUserId, userId)
                .eq(UserMedia::getStatus, 1));
        if (media == null) {
            throw new MediaException("素材不存在或无访问权限");
        }
        return media;
    }

    private List<UserMediaVariant> loadVariants(Long mediaId, Long userId) {
        return variantMapper.selectList(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getMediaId, mediaId)
                .eq(UserMediaVariant::getUserId, userId)
                .orderByAsc(UserMediaVariant::getId));
    }

    private UserMediaVariant findVariant(Long mediaId, String variantType, String processor) {
        return variantMapper.selectOne(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getMediaId, mediaId)
                .eq(UserMediaVariant::getVariantType, variantType)
                .eq(UserMediaVariant::getProcessor, processor));
    }

    private Long resolveComfyuiWorkflowId(String variantType) {
        MediaStandardizationConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<MediaStandardizationConfig>()
                        .eq(MediaStandardizationConfig::getVariantType, variantType)
                        .eq(MediaStandardizationConfig::getEnabled, true));
        return config != null ? config.getWorkflowId() : null;
    }

    private String resolveInputUrlForComfyui(UserMedia media, Long preferredVariantId) {
        if (preferredVariantId != null) {
            UserMediaVariant variant = variantMapper.selectById(preferredVariantId);
            if (variant != null && MediaVariantStatusEnum.SUCCEEDED.getDec().equals(variant.getStatus())) {
                return uploadUtil.toSignedUrl(variant.getObjectKey());
            }
        }
        UserMediaVariant builtin = findVariant(media.getId(), MediaVariantTypeEnum.HEADSHOT_BUILTIN.getDec(),
                MediaVariantProcessorEnum.BUILTIN.getDec());
        if (builtin != null && MediaVariantStatusEnum.SUCCEEDED.getDec().equals(builtin.getStatus())) {
            return uploadUtil.toSignedUrl(builtin.getObjectKey());
        }
        return uploadUtil.toSignedUrl(media.getObjectKey());
    }

    private TaskNodeContainer findFirstUploadNode(WorkflowInterfaceVo workflowInterface, boolean imagePreferred) {
        if (workflowInterface.getFormContainer() == null) {
            return null;
        }
        String targetType = imagePreferred ? ComfyuiFormTypeEnum.IMAGE_UPLOAD.getDec()
                : ComfyuiFormTypeEnum.VIDEO_UPLOAD.getDec();
        String altType = imagePreferred ? ComfyuiFormTypeEnum.IMAGE_SCRIBBLE.getDec() : null;

        for (WorkflowInterfaceVo.WorkflowsFormContainer form : workflowInterface.getFormContainer()) {
            if (targetType.equals(form.getType()) || (altType != null && altType.equals(form.getType()))) {
                return new TaskNodeContainer()
                        .setNodeKey(form.getNodeKey())
                        .setInputs(form.getInputs())
                        .setIsUpload(true);
            }
        }
        return null;
    }

    private UserMediaVo toMediaVo(UserMedia media, List<UserMediaVariant> variants) {
        return new UserMediaVo()
                .setMediaId(media.getId())
                .setName(media.getName())
                .setMediaType(media.getMediaType())
                .setUrl(uploadUtil.toSignedUrl(media.getObjectKey()))
                .setMimeType(media.getMimeType())
                .setFileSize(media.getFileSize())
                .setWidth(media.getWidth())
                .setHeight(media.getHeight())
                .setDurationMs(media.getDurationMs())
                .setSource(media.getSource())
                .setTags(media.getTags())
                .setVariants(variants.stream().map(this::toVariantVo).toList())
                .setCreateTime(media.getCreateTime());
    }

    private MediaVariantVo toVariantVo(UserMediaVariant variant) {
        return new MediaVariantVo()
                .setVariantId(variant.getId())
                .setMediaId(variant.getMediaId())
                .setVariantType(variant.getVariantType())
                .setUrl(StringUtils.hasText(variant.getObjectKey()) ? uploadUtil.toSignedUrl(variant.getObjectKey()) : null)
                .setStatus(variant.getStatus())
                .setProcessor(variant.getProcessor())
                .setWorkflowId(variant.getWorkflowId())
                .setTaskId(variant.getTaskId())
                .setMeta(variant.getMeta())
                .setCreateTime(variant.getCreateTime());
    }

    private String variantLabel(String variantType) {
        if (MediaVariantTypeEnum.HEADSHOT_BUILTIN.getDec().equals(variantType)) {
            return "大头照";
        }
        if (MediaVariantTypeEnum.HEADSHOT_COMFYUI.getDec().equals(variantType)) {
            return "精修大头照";
        }
        if (MediaVariantTypeEnum.BG_REMOVED.getDec().equals(variantType)) {
            return "抠图";
        }
        return variantType;
    }

    private void validateUploadFile(MultipartFile file) {
        String contentType = file.getContentType();
        long size = file.getSize();
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        AliConfiguration.Oss ossConfig = aliConfiguration.getOss();
        boolean isValidType = false;
        Long maxSizeInBytes = null;
        for (AliConfiguration.SupportedFileType supportedFileType : ossConfig.getSupportedFileTypes()) {
            if (supportedFileType.getExtension().equalsIgnoreCase(extension)
                    && supportedFileType.getMimeType().equalsIgnoreCase(contentType)) {
                isValidType = true;
                maxSizeInBytes = supportedFileType.getMaxSizeInBytes();
                break;
            }
        }
        if (!isValidType) {
            throw new UploadException("不支持的文件类型: " + contentType + " (扩展名: " + extension + ")");
        }
        if (maxSizeInBytes != null && size > maxSizeInBytes) {
            throw new UploadException("文件大小超过限制. 最大允许: " + maxSizeInBytes / (1024 * 1024) + "MB");
        }
    }

    private String resolveMediaType(String mimeType, String extension) {
        if (mimeType != null && mimeType.startsWith("video/")) {
            return UserMediaTypeEnum.VIDEO.getDec();
        }
        if (mimeType != null && mimeType.startsWith("audio/")) {
            return UserMediaTypeEnum.AUDIO.getDec();
        }
        return UserMediaTypeEnum.IMAGE.getDec();
    }

    private void fillImageDimensions(MultipartFile file, UserMedia media) {
        if (!UserMediaTypeEnum.IMAGE.getDec().equals(media.getMediaType())) {
            return;
        }
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image != null) {
                media.setWidth(image.getWidth());
                media.setHeight(image.getHeight());
            }
        } catch (Exception e) {
            log.debug("读取图片尺寸失败", e);
        }
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "bin";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "bin";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "未命名";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    /** insert 前占位，满足 object_key NOT NULL；insert 后立刻 update 为真实路径 */
    private String pendingObjectKey(Long userId) {
        return MediaObjectKeyUtil.originalKey(userId, 0L, "pending");
    }
}
