package com.ticket.app.controller.admin.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 管理员用户列表请求参数
 */
@Data
public class AdminUserListRequest {

    @Parameter(description = "页码", example = "1")
    private Integer page = 1;

    @Parameter(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Parameter(description = "搜索关键词(用户名/手机号/昵称)")
    private String keyword;

    @Parameter(description = "用户状态: 0-禁用, 1-正常, 2-待激活")
    private Integer status;

    @Parameter(description = "用户类型: 1-普通, 2-VIP, 3-企业")
    private Integer userType;

    @Parameter(description = "注册开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Parameter(description = "注册结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}