package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * 通知配置请求参数
 */
@Data
public class NotificationConfigRequest {

    @Parameter(description = "是否启用邮件通知")
    private Boolean emailEnabled;

    @Parameter(description = "邮件服务器地址")
    private String emailHost;

    @Parameter(description = "邮件服务器端口")
    private Integer emailPort;

    @Parameter(description = "邮件用户名")
    private String emailUsername;

    @Parameter(description = "邮件密码")
    private String emailPassword;

    @Parameter(description = "是否启用短信通知")
    private Boolean smsEnabled;

    @Parameter(description = "短信服务商")
    private String smsProvider;
}