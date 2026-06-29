package com.cn.common.vo;

import lombok.Data;

@Data
public class ClientVersionVo {

    private String version;

    private String minSupportedVersion;

    private String downloadUrl;
}
