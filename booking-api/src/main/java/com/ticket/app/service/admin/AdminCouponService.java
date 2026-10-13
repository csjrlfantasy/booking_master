package com.ticket.app.service.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.controller.admin.dto.request.AdminCouponCreateRequest;
import com.ticket.app.controller.admin.dto.response.AdminCouponListItemResponse;

import java.util.List;

public interface AdminCouponService {

    AdminCouponCreateRequest detail(Long id);

    Long create(AdminCouponCreateRequest request);

    void update(Long id, AdminCouponCreateRequest request);

    PageResult<AdminCouponListItemResponse> list(String keyword, Integer status, Integer page, Integer pageSize);

    void updateStatus(Long id, Integer status);

    void issue(Long couponId, List<Long> userIds, String issueMode);
}
