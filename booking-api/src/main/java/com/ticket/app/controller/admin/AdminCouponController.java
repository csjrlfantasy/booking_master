package com.ticket.app.controller.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCouponCreateRequest;
import com.ticket.app.controller.admin.dto.request.AdminCouponIssueRequest;
import com.ticket.app.controller.admin.dto.request.AdminCouponStatusRequest;
import com.ticket.app.controller.admin.dto.response.AdminCouponListItemResponse;
import com.ticket.app.service.admin.AdminCouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;
    private final JwtUtils jwtUtils;

    @GetMapping
    public Result<PageResult<AdminCouponListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        getAdminIdFromRequest(request);
        return Result.success(adminCouponService.list(keyword, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AdminCouponCreateRequest> detail(@PathVariable Long id, HttpServletRequest request) {
        getAdminIdFromRequest(request);
        return Result.success(adminCouponService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody AdminCouponCreateRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        return Result.success(adminCouponService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AdminCouponCreateRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        adminCouponService.update(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody AdminCouponStatusRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        adminCouponService.updateStatus(id, request != null ? request.getStatus() : null);
        return Result.success();
    }

    @PostMapping("/{id}/issue")
    public Result<Void> issue(@PathVariable Long id, @RequestBody AdminCouponIssueRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        adminCouponService.issue(id, request != null ? request.getUserIds() : null, request != null ? request.getIssueMode() : null);
        return Result.success();
    }

    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "login required");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "invalid token");
        }
        return userId;
    }
}
