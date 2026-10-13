package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.admin.dto.request.AdminCouponCreateRequest;
import com.ticket.app.controller.admin.dto.response.AdminCouponListItemResponse;
import com.ticket.app.entity.Coupon;
import com.ticket.app.entity.UserCoupon;
import com.ticket.app.repository.coupon.CouponRepository;
import com.ticket.app.repository.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    public AdminCouponCreateRequest detail(Long id) {
        Coupon coupon = couponRepository.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "coupon not found");
        }
        return toDetail(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminCouponCreateRequest request) {
        validateCreateRequest(request);
        Coupon coupon = buildCoupon(request);
        couponRepository.insert(coupon);
        return coupon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminCouponCreateRequest request) {
        validateCreateRequest(request);
        Coupon existing = couponRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "coupon not found");
        }

        Coupon coupon = buildCoupon(request);
        coupon.setId(id);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponRepository.updateById(coupon);
    }

    @Override
    public PageResult<AdminCouponListItemResponse> list(String keyword, Integer status, Integer page, Integer pageSize) {
        int current = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 10;
        Page<Coupon> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Coupon::getName, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreatedAt).orderByDesc(Coupon::getId);

        IPage<Coupon> result = couponRepository.selectPage(pageParam, wrapper);
        List<AdminCouponListItemResponse> list = result.getRecords().stream().map(this::toListItem).toList();
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        Coupon coupon = couponRepository.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "coupon not found");
        }
        coupon.setStatus(status);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponRepository.updateById(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long couponId, List<Long> userIds, String issueMode) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "userIds is required");
        }

        Coupon coupon = couponRepository.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "coupon not found");
        }
        if (!Coupon.STATUS_ACTIVE.equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "coupon is not active");
        }
        if (coupon.getRemainCount() == null || coupon.getRemainCount() <= 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "coupon stock is empty");
        }

        int issuedCount = 0;
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            long currentCount = userCouponRepository.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, couponId)
                    .ne(UserCoupon::getStatus, UserCoupon.STATUS_DISABLED));
            if (coupon.getPerLimit() != null && currentCount >= coupon.getPerLimit()) {
                continue;
            }
            if (coupon.getRemainCount() - issuedCount <= 0) {
                break;
            }
            userCouponRepository.insert(buildIssuedUserCoupon(userId, coupon, issueMode));
            issuedCount++;
        }

        if (issuedCount <= 0) {
            return;
        }

        coupon.setRemainCount(coupon.getRemainCount() - issuedCount);
        coupon.setUpdatedAt(LocalDateTime.now());
        couponRepository.updateById(coupon);
        log.info("issued coupon: couponId={}, issuedCount={}, issueMode={}", couponId, issuedCount, issueMode);
    }

    private Coupon buildCoupon(AdminCouponCreateRequest request) {
        Coupon coupon = new Coupon();
        coupon.setName(request.getName().trim());
        coupon.setCouponType(request.getCouponType());
        coupon.setCouponValue(request.getCouponValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setTotalCount(request.getTotalCount());
        coupon.setRemainCount(request.getRemainCount() != null ? request.getRemainCount() : request.getTotalCount());
        coupon.setPerLimit(request.getPerLimit());
        coupon.setReceiveType(request.getReceiveType());
        coupon.setValidityType(request.getValidityType());
        coupon.setValidStartDate(request.getValidStartDate());
        coupon.setValidEndDate(request.getValidEndDate());
        coupon.setValidDays(request.getValidDays());
        coupon.setApplicableScope(request.getApplicableScope());
        coupon.setApplicableTickets(request.getApplicableTickets());
        coupon.setApplicableCategories(request.getApplicableCategories());
        coupon.setDescription(request.getDescription());
        coupon.setNotice(request.getNotice());
        coupon.setReceiveStartTime(request.getReceiveStartTime());
        coupon.setReceiveEndTime(request.getReceiveEndTime());
        coupon.setStatus(request.getStatus() != null ? request.getStatus() : Coupon.STATUS_ACTIVE);
        return coupon;
    }

    private UserCoupon buildIssuedUserCoupon(Long userId, Coupon coupon, String issueMode) {
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setCouponName(coupon.getName());
        userCoupon.setCouponType(coupon.getCouponType());
        userCoupon.setCouponValue(coupon.getCouponValue());
        userCoupon.setMinOrderAmount(coupon.getMinOrderAmount());
        userCoupon.setReceiveTime(now);
        if (Coupon.VALID_TYPE_FIXED.equals(coupon.getValidityType())) {
            userCoupon.setValidStartTime(coupon.getValidStartDate() != null ? coupon.getValidStartDate().atStartOfDay() : now);
            userCoupon.setValidEndTime(coupon.getValidEndDate() != null ? coupon.getValidEndDate().atTime(23, 59, 59) : now);
        } else {
            userCoupon.setValidStartTime(now);
            userCoupon.setValidEndTime(now.plusDays(coupon.getValidDays() != null ? coupon.getValidDays() : 0));
        }
        userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
        userCoupon.setSource(issueMode == null || issueMode.trim().isEmpty() ? UserCoupon.SOURCE_ADMIN : issueMode.trim());
        return userCoupon;
    }

    private AdminCouponListItemResponse toListItem(Coupon coupon) {
        AdminCouponListItemResponse item = new AdminCouponListItemResponse();
        item.setId(coupon.getId());
        item.setName(coupon.getName());
        item.setCouponType(coupon.getCouponType());
        item.setCouponTypeText(couponTypeText(coupon.getCouponType()));
        item.setCouponValue(coupon.getCouponValue());
        item.setMinOrderAmount(coupon.getMinOrderAmount());
        item.setTotalCount(coupon.getTotalCount());
        item.setRemainCount(coupon.getRemainCount());
        item.setPerLimit(coupon.getPerLimit());
        item.setReceiveType(coupon.getReceiveType());
        item.setValidityType(coupon.getValidityType());
        item.setApplicableScope(coupon.getApplicableScope());
        item.setStatus(coupon.getStatus());
        item.setStatusText(statusText(coupon.getStatus()));
        item.setReceiveStartTime(coupon.getReceiveStartTime());
        item.setReceiveEndTime(coupon.getReceiveEndTime());
        item.setCreatedAt(coupon.getCreatedAt());
        return item;
    }

    private AdminCouponCreateRequest toDetail(Coupon coupon) {
        AdminCouponCreateRequest detail = new AdminCouponCreateRequest();
        detail.setName(coupon.getName());
        detail.setCouponType(coupon.getCouponType());
        detail.setCouponValue(coupon.getCouponValue());
        detail.setMinOrderAmount(coupon.getMinOrderAmount());
        detail.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        detail.setTotalCount(coupon.getTotalCount());
        detail.setRemainCount(coupon.getRemainCount());
        detail.setPerLimit(coupon.getPerLimit());
        detail.setReceiveType(coupon.getReceiveType());
        detail.setValidityType(coupon.getValidityType());
        detail.setValidStartDate(coupon.getValidStartDate());
        detail.setValidEndDate(coupon.getValidEndDate());
        detail.setValidDays(coupon.getValidDays());
        detail.setApplicableScope(coupon.getApplicableScope());
        detail.setApplicableTickets(coupon.getApplicableTickets());
        detail.setApplicableCategories(coupon.getApplicableCategories());
        detail.setDescription(coupon.getDescription());
        detail.setNotice(coupon.getNotice());
        detail.setReceiveStartTime(coupon.getReceiveStartTime());
        detail.setReceiveEndTime(coupon.getReceiveEndTime());
        detail.setStatus(coupon.getStatus());
        return detail;
    }

    private void validateCreateRequest(AdminCouponCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "coupon request is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "coupon name is required");
        }
        if (request.getCouponType() == null || request.getCouponValue() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "coupon type and value are required");
        }
        if (request.getTotalCount() == null || request.getTotalCount() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "totalCount must be greater than 0");
        }
        if (request.getPerLimit() == null || request.getPerLimit() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "perLimit must be greater than 0");
        }
        if (request.getValidityType() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "validityType is required");
        }
        if (Coupon.VALID_TYPE_FIXED.equals(request.getValidityType())
                && (request.getValidStartDate() == null || request.getValidEndDate() == null)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "validStartDate and validEndDate are required");
        }
        if (Coupon.VALID_TYPE_DYNAMIC.equals(request.getValidityType())
                && (request.getValidDays() == null || request.getValidDays() <= 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "validDays must be greater than 0");
        }
        validateStatus(request.getStatus() != null ? request.getStatus() : Coupon.STATUS_ACTIVE);
    }

    private void validateStatus(Integer status) {
        List<Integer> allowed = new ArrayList<>(List.of(Coupon.STATUS_INACTIVE, Coupon.STATUS_ACTIVE, Coupon.STATUS_ENDED));
        if (!allowed.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "invalid coupon status");
        }
    }

    private String couponTypeText(Integer couponType) {
        if (Coupon.TYPE_FULL_REDUCE.equals(couponType)) {
            return "满减券";
        }
        if (Coupon.TYPE_DISCOUNT.equals(couponType)) {
            return "折扣券";
        }
        if (Coupon.TYPE_CASH.equals(couponType)) {
            return "立减券";
        }
        return "未知";
    }

    private String statusText(Integer status) {
        if (Coupon.STATUS_INACTIVE.equals(status)) {
            return "未激活";
        }
        if (Coupon.STATUS_ACTIVE.equals(status)) {
            return "进行中";
        }
        if (Coupon.STATUS_ENDED.equals(status)) {
            return "已结束";
        }
        return "未知";
    }
}
