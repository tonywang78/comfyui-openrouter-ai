package com.cn.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.cn.common.enums.RoleEnum;
import com.cn.common.msg.Result;
import com.cn.common.service.ApiKeyAuthService;
import com.cn.common.utils.ApiKeyGenerator;
import com.cn.common.utils.CredentialUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("all")
public class RoutePathFilter {

    private final ApiKeyAuthService apiKeyAuthService;
    private final CredentialUtils credentialUtils;

    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**").addExclude("/favicon.ico")
                .setAuth(obj -> {
                    if (!StpUtil.isLogin()) {
                        String rawKey = credentialUtils.extractApiKeyFromRequest();
                        if (rawKey != null) {
                            boolean authenticated = apiKeyAuthService.authenticate(rawKey)
                                    .map(user -> {
                                        apiKeyAuthService.loginAsUser(user);
                                        return true;
                                    })
                                    .orElse(false);
                            if (!authenticated && ApiKeyGenerator.isApiKey(rawKey)) {
                                throw NotLoginException.newInstance(
                                        StpUtil.getLoginType(),
                                        NotLoginException.INVALID_TOKEN,
                                        "API Key 无效或已过期",
                                        null);
                            }
                        }
                    }

                    SaRouter.match("/system/**", r -> StpUtil.checkRole(RoleEnum.ADMIN.getDesc()));

                    SaRouter.match("/**")
                            .notMatch(
                                    "/auth/**",
                                    "/client/**",
                                    "/ws/**",
                                    "/llm/chat/stream",
                                    "/llm/generation/stream",
                                    "/llm/get/available-model/page",
                                    "/llm/get/available-model/list",
                                    "/llm/get/default-model",
                                    "/notice/get"
                            )
                            .check(r -> StpUtil.checkLogin());
                })
                .setError(e -> {
                    String rawKey = credentialUtils.extractApiKeyFromRequest();
                    if (rawKey != null && ApiKeyGenerator.isApiKey(rawKey)) {
                        return Result.error("API Key 无效或已过期", 401);
                    }
                    return Result.error("登录身份信息已过期,请重新登录", 401);
                })
                .setBeforeAuth(r -> {
                    SaHolder.getResponse()
                            .setHeader("Access-Control-Allow-Origin", "*")
                            .setHeader("Access-Control-Allow-Methods", "*")
                            .setHeader("Access-Control-Max-Age", "3600")
                            .setHeader("Access-Control-Allow-Headers", "*")
                            .setServer("Zeus");
                    if (SaHolder.getRequest().getMethod().equals(HttpMethod.OPTIONS.toString())) {
                        SaRouter.back();
                    }
                });
    }
}
