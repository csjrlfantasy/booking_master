package com.ticket.app.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.IdCardUtils;
import com.ticket.app.controller.user.dto.UserIdentityRequest;
import com.ticket.app.controller.user.dto.UserIdentityResponse;
import com.ticket.app.entity.UserIdentity;
import com.ticket.app.repository.user.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserIdentityServiceImpl implements UserIdentityService {

    private final UserIdentityRepository repository;

    @Override
    public List<UserIdentityResponse> list(Long userId) {
        LambdaQueryWrapper<UserIdentity> wrapper = baseWrapper(userId)
                .orderByDesc(UserIdentity::getIsDefault)
                .orderByDesc(UserIdentity::getIsSelf)
                .orderByDesc(UserIdentity::getCreatedAt);
        return repository.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserIdentityResponse create(Long userId, UserIdentityRequest request) {
        validateRequest(request);
        if (Boolean.TRUE.equals(request.getDefaultIdentity())) {
            clearDefault(userId);
        }
        UserIdentity identity = new UserIdentity();
        identity.setUserId(userId);
        applyRequest(identity, request);
        repository.insert(identity);
        return toResponse(identity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserIdentityResponse update(Long userId, Long identityId, UserIdentityRequest request) {
        validateRequest(request);
        UserIdentity identity = requireOwnedIdentity(userId, identityId);
        if (Boolean.TRUE.equals(request.getDefaultIdentity())) {
            clearDefault(userId);
        }
        applyRequest(identity, request);
        repository.updateById(identity);
        return toResponse(identity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long identityId) {
        UserIdentity identity = requireOwnedIdentity(userId, identityId);
        identity.setDeletedAt(LocalDateTime.now());
        repository.updateById(identity);
    }

    @Override
    public UserIdentity requireOwnedIdentity(Long userId, Long identityId) {
        UserIdentity identity = repository.selectById(identityId);
        if (identity == null || identity.getDeletedAt() != null || !userId.equals(identity.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "观演人证件不存在或无权使用");
        }
        return identity;
    }

    private void validateRequest(UserIdentityRequest request) {
        if (!IdCardUtils.isValid(request.getIdCardNo())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份证号不合法");
        }
    }

    private void applyRequest(UserIdentity identity, UserIdentityRequest request) {
        identity.setRealName(request.getRealName().trim());
        identity.setIdCardNo(request.getIdCardNo().trim().toUpperCase());
        identity.setPhone(request.getPhone());
        identity.setIsSelf(Boolean.TRUE.equals(request.getSelf()) ? 1 : 0);
        identity.setIsDefault(Boolean.TRUE.equals(request.getDefaultIdentity()) ? 1 : 0);
    }

    private void clearDefault(Long userId) {
        repository.selectList(baseWrapper(userId)).forEach(identity -> {
            if (Integer.valueOf(1).equals(identity.getIsDefault())) {
                identity.setIsDefault(0);
                repository.updateById(identity);
            }
        });
    }

    private LambdaQueryWrapper<UserIdentity> baseWrapper(Long userId) {
        return new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getUserId, userId)
                .isNull(UserIdentity::getDeletedAt);
    }

    private UserIdentityResponse toResponse(UserIdentity identity) {
        return UserIdentityResponse.builder()
                .id(identity.getId())
                .realName(identity.getRealName())
                .idCardNo(identity.getIdCardNo())
                .maskedIdCardNo(IdCardUtils.mask(identity.getIdCardNo()))
                .phone(identity.getPhone())
                .self(Integer.valueOf(1).equals(identity.getIsSelf()))
                .defaultIdentity(Integer.valueOf(1).equals(identity.getIsDefault()))
                .build();
    }
}
