package com.ticket.app.service.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.controller.admin.dto.request.BatchUpdateConfigRequest;
import com.ticket.app.controller.admin.dto.request.NotificationConfigRequest;
import com.ticket.app.controller.admin.dto.request.UpdateConfigRequest;
import com.ticket.app.controller.admin.dto.response.SystemConfigResponse;

/**
 * 系统配置服务接口
 */
public interface AdminConfigService {

    /**
     * 获取配置列表
     *
     * @param configGroup 配置分组
     * @param keyword 搜索关键词
     * @return 分页配置列表
     */
    PageResult<SystemConfigResponse> getConfigList(String configGroup, String keyword);

    /**
     * 获取单个配置
     *
     * @param configId 配置ID
     * @return 配置详情
     */
    SystemConfigResponse getConfig(Long configId);

    /**
     * 更新配置
     *
     * @param configId 配置ID
     * @param request 更新请求
     */
    void updateConfig(Long configId, UpdateConfigRequest request);

    /**
     * 批量更新配置
     *
     * @param request 批量更新请求
     */
    void batchUpdateConfig(BatchUpdateConfigRequest request);

    /**
     * 获取通知配置
     *
     * @return 通知配置
     */
    NotificationConfigRequest getNotificationConfig();

    /**
     * 更新通知配置
     *
     * @param request 通知配置请求
     */
    void updateNotificationConfig(NotificationConfigRequest request);
}