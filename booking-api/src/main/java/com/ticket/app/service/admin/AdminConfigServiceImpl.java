package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.admin.dto.request.BatchUpdateConfigRequest;
import com.ticket.app.controller.admin.dto.request.NotificationConfigRequest;
import com.ticket.app.controller.admin.dto.request.UpdateConfigRequest;
import com.ticket.app.controller.admin.dto.response.SystemConfigResponse;
import com.ticket.app.entity.admin.SystemConfig;
import com.ticket.app.repository.admin.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public PageResult<SystemConfigResponse> getConfigList(String configGroup, String keyword) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getStatus, 1);
        queryWrapper.isNull(SystemConfig::getDeletedAt);

        if (StringUtils.hasText(configGroup)) {
            queryWrapper.eq(SystemConfig::getConfigGroup, configGroup);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(SystemConfig::getConfigName, keyword)
                    .or().like(SystemConfig::getConfigKey, keyword));
        }
        queryWrapper.orderByAsc(SystemConfig::getSortOrder);

        List<SystemConfig> configs = systemConfigRepository.selectList(queryWrapper);

        List<SystemConfigResponse> responses = configs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, (long) responses.size(), 1, responses.size());
    }

    @Override
    public SystemConfigResponse getConfig(Long configId) {
        SystemConfig config = systemConfigRepository.selectById(configId);
        if (config == null || config.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return convertToResponse(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long configId, UpdateConfigRequest request) {
        SystemConfig config = systemConfigRepository.selectById(configId);
        if (config == null || config.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }

        if (config.getIsSystem() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "系统配置不允许修改");
        }

        if (StringUtils.hasText(request.getConfigValue())) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getConfigDesc() != null) {
            config.setConfigDesc(request.getConfigDesc());
        }
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigRepository.updateById(config);

        log.info("更新系统配置: configId={}, configKey={}", configId, config.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateConfig(BatchUpdateConfigRequest request) {
        for (BatchUpdateConfigRequest.ConfigItem item : request.getConfigs()) {
            if (item.getId() != null && StringUtils.hasText(item.getConfigValue())) {
                SystemConfig config = systemConfigRepository.selectById(item.getId());
                if (config != null && config.getIsSystem() != 1 && config.getDeletedAt() == null) {
                    config.setConfigValue(item.getConfigValue());
                    config.setUpdatedAt(LocalDateTime.now());
                    systemConfigRepository.updateById(config);
                }
            }
        }
        log.info("批量更新系统配置: count={}", request.getConfigs().size());
    }

    @Override
    public NotificationConfigRequest getNotificationConfig() {
        NotificationConfigRequest request = new NotificationConfigRequest();

        // 查询邮件配置
        LambdaQueryWrapper<SystemConfig> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(SystemConfig::getConfigGroup, "notification");
        emailWrapper.eq(SystemConfig::getStatus, 1);
        List<SystemConfig> configs = systemConfigRepository.selectList(emailWrapper);

        for (SystemConfig config : configs) {
            String key = config.getConfigKey();
            String value = config.getConfigValue();
            switch (key != null ? key : "") {
                case "email_enabled" -> request.setEmailEnabled("true".equalsIgnoreCase(value));
                case "email_host" -> request.setEmailHost(value);
                case "email_port" -> {
                    if (value != null) request.setEmailPort(Integer.parseInt(value));
                }
                case "email_username" -> request.setEmailUsername(value);
                case "sms_enabled" -> request.setSmsEnabled("true".equalsIgnoreCase(value));
                case "sms_provider" -> request.setSmsProvider(value);
            }
        }

        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotificationConfig(NotificationConfigRequest request) {
        // 更新邮件配置
        if (request.getEmailEnabled() != null) {
            updateOrCreateConfig("email_enabled", request.getEmailEnabled() ? "true" : "false", "notification");
        }
        if (request.getEmailHost() != null) {
            updateOrCreateConfig("email_host", request.getEmailHost(), "notification");
        }
        if (request.getEmailPort() != null) {
            updateOrCreateConfig("email_port", String.valueOf(request.getEmailPort()), "notification");
        }
        if (request.getEmailUsername() != null) {
            updateOrCreateConfig("email_username", request.getEmailUsername(), "notification");
        }

        // 更新短信配置
        if (request.getSmsEnabled() != null) {
            updateOrCreateConfig("sms_enabled", request.getSmsEnabled() ? "true" : "false", "notification");
        }
        if (request.getSmsProvider() != null) {
            updateOrCreateConfig("sms_provider", request.getSmsProvider(), "notification");
        }

        log.info("更新通知配置成功");
    }

    /**
     * 更新或创建配置项
     */
    private void updateOrCreateConfig(String key, String value, String group) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey, key);
        queryWrapper.isNull(SystemConfig::getDeletedAt);

        SystemConfig config = systemConfigRepository.selectOne(queryWrapper);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigGroup(group);
            config.setStatus(1);
            config.setCreatedAt(LocalDateTime.now());
            systemConfigRepository.insert(config);
        } else {
            config.setConfigValue(value);
            config.setUpdatedAt(LocalDateTime.now());
            systemConfigRepository.updateById(config);
        }
    }

    /**
     * 转换为响应对象
     */
    private SystemConfigResponse convertToResponse(SystemConfig config) {
        SystemConfigResponse response = new SystemConfigResponse();
        response.setId(config.getId());
        response.setConfigKey(config.getConfigKey());
        response.setConfigValue(config.getConfigValue());
        response.setConfigType(config.getConfigType());
        response.setConfigName(config.getConfigName());
        response.setConfigDesc(config.getConfigDesc());
        response.setConfigGroup(config.getConfigGroup());
        response.setIsSystem(config.getIsSystem());
        return response;
    }
}