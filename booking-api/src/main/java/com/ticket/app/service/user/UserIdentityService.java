package com.ticket.app.service.user;

import com.ticket.app.controller.user.dto.UserIdentityRequest;
import com.ticket.app.controller.user.dto.UserIdentityResponse;
import com.ticket.app.entity.UserIdentity;

import java.util.List;

public interface UserIdentityService {
    List<UserIdentityResponse> list(Long userId);

    UserIdentityResponse create(Long userId, UserIdentityRequest request);

    UserIdentityResponse update(Long userId, Long identityId, UserIdentityRequest request);

    void delete(Long userId, Long identityId);

    UserIdentity requireOwnedIdentity(Long userId, Long identityId);
}
