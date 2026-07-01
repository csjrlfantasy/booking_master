package com.ticket.app.service.coupon;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.coupon.dto.CouponCenterResponse;
import com.ticket.app.controller.coupon.dto.UserCouponResponse;
import com.ticket.app.controller.coupon.dto.ValidateCouponRequest;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.entity.Coupon;
import com.ticket.app.entity.UserCoupon;
import com.ticket.app.repository.coupon.CouponRepository;
import com.ticket.app.repository.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "优惠券不存在");
        }
        if (!Coupon.STATUS_ACTIVE.equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不可领取");
        }
        if (coupon.getRemainCount() <= 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已领完");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getReceiveStartTime() != null && now.isBefore(coupon.getReceiveStartTime())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券领取尚未开始");
        }
        if (coupon.getReceiveEndTime() != null && now.isAfter(coupon.getReceiveEndTime())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券领取已结束");
        }

        LambdaQueryWrapper<UserCoupon> userCouponWrapper = new LambdaQueryWrapper<>();
        userCouponWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId)
                .ne(UserCoupon::getStatus, UserCoupon.STATUS_DISABLED);
        Long receivedCount = userCouponRepository.selectCount(userCouponWrapper);
        if (receivedCount >= coupon.getPerLimit()) {
            throw new BusinessException(ResultCode.COUPON_REACH_LIMIT, "已达到领取上限");
        }

        LocalDateTime validStartTime;
        LocalDateTime validEndTime;
        if (Coupon.VALID_TYPE_FIXED.equals(coupon.getValidityType())) {
            validStartTime = coupon.getValidStartDate().atStartOfDay();
            validEndTime = coupon.getValidEndDate().atTime(23, 59, 59);
        } else {
            validStartTime = now;
            validEndTime = now.plusDays(coupon.getValidDays());
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponName(coupon.getName());
        userCoupon.setCouponType(coupon.getCouponType());
        userCoupon.setCouponValue(coupon.getCouponValue());
        userCoupon.setMinOrderAmount(coupon.getMinOrderAmount());
        userCoupon.setReceiveTime(now);
        userCoupon.setValidStartTime(validStartTime);
        userCoupon.setValidEndTime(validEndTime);
        userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
        userCoupon.setSource(UserCoupon.SOURCE_MANUAL);
        userCouponRepository.insert(userCoupon);

        coupon.setRemainCount(coupon.getRemainCount() - 1);
        couponRepository.updateById(coupon);

        log.info("领取优惠券成功: userId={}, couponId={}, userCouponId={}", userId, couponId, userCoupon.getId());
        return userCoupon.getId();
    }

    @Override
    public PageResult<CouponCenterResponse> getCouponCenter(Long userId, Integer page, Integer pageSize) {
        Page<Coupon> pageParam = new Page<>(page, pageSize);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, Coupon.STATUS_ACTIVE)
                .and(q -> q.isNull(Coupon::getReceiveStartTime).or().le(Coupon::getReceiveStartTime, now))
                .and(q -> q.isNull(Coupon::getReceiveEndTime).or().ge(Coupon::getReceiveEndTime, now))
                .orderByDesc(Coupon::getCreatedAt);

        IPage<Coupon> pageResult = couponRepository.selectPage(pageParam, wrapper);
        List<CouponCenterResponse> list = pageResult.getRecords().stream()
                .map(coupon -> buildCouponCenterResponse(userId, coupon))
                .collect(Collectors.toList());

        return PageResult.of(list, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public PageResult<UserCouponResponse> getUserCouponList(Long userId, Integer status, Integer page, Integer pageSize) {
        Page<UserCoupon> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreatedAt);

        IPage<UserCoupon> pageResult = userCouponRepository.selectPage(pageParam, wrapper);
        List<UserCouponResponse> list = pageResult.getRecords().stream()
                .map(this::buildUserCouponResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public List<UserCouponResponse> getAvailableCoupons(Long userId, BigDecimal orderAmount, Long ticketId, Integer categoryId) {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                .isNull(UserCoupon::getOrderId)
                .le(UserCoupon::getValidStartTime, now)
                .ge(UserCoupon::getValidEndTime, now);

        List<UserCoupon> coupons = userCouponRepository.selectList(wrapper);
        List<UserCouponResponse> availableCoupons = new ArrayList<>();
        for (UserCoupon userCoupon : coupons) {
            if (userCoupon.getMinOrderAmount() != null && orderAmount.compareTo(userCoupon.getMinOrderAmount()) < 0) {
                continue;
            }

            Coupon coupon = couponRepository.selectById(userCoupon.getCouponId());
            if (coupon == null) {
                continue;
            }
            if (Coupon.SCOPE_ALL.equals(coupon.getApplicableScope())) {
                // no extra filter
            } else if (Coupon.SCOPE_TICKET.equals(coupon.getApplicableScope())) {
                if (ticketId == null) {
                    continue;
                }
                String applicableTickets = coupon.getApplicableTickets();
                if (applicableTickets != null) {
                    JSONArray ticketIds = JSON.parseArray(applicableTickets);
                    if (!ticketIds.contains(ticketId)) {
                        continue;
                    }
                }
            } else if (Coupon.SCOPE_CATEGORY.equals(coupon.getApplicableScope())) {
                if (categoryId == null) {
                    continue;
                }
                String applicableCategories = coupon.getApplicableCategories();
                if (applicableCategories != null) {
                    JSONArray categoryIds = JSON.parseArray(applicableCategories);
                    if (!categoryIds.contains(categoryId)) {
                        continue;
                    }
                }
            }

            availableCoupons.add(buildUserCouponResponse(userCoupon));
        }

        return availableCoupons;
    }

    @Override
    public ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long ticketId, Integer categoryId) {
        UserCoupon userCoupon = requireOwnedCoupon(userId, userCouponId);
        ensureCouponCanBeApplied(userCoupon, orderAmount);

        Coupon coupon = couponRepository.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "优惠券模板不存在");
        }
        validateCouponScope(coupon, ticketId, categoryId);

        ValidateCouponResponse response = new ValidateCouponResponse();
        response.setUsable(true);
        response.setUserCouponId(userCoupon.getId());
        response.setCouponId(coupon.getId());
        response.setCouponName(coupon.getName());
        response.setCouponType(coupon.getCouponType());
        response.setCouponValue(coupon.getCouponValue());
        response.setDiscountAmount(calculateDiscountAmount(userCoupon, orderAmount));
        response.setMinOrderAmount(coupon.getMinOrderAmount());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCouponToOrder(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = requireOwnedCoupon(userId, userCouponId);
        ensureCouponCanBeApplied(userCoupon, userCoupon.getMinOrderAmount() == null ? BigDecimal.ZERO : userCoupon.getMinOrderAmount());
        if (userCoupon.getOrderId() != null && !Objects.equals(orderId, userCoupon.getOrderId())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已绑定其他订单");
        }

        userCoupon.setOrderId(orderId);
        userCouponRepository.updateById(userCoupon);
        log.info("绑定优惠券到待支付订单: userId={}, userCouponId={}, orderId={}", userId, userCouponId, orderId);
    }

    @Override
    public ValidateCouponResponse validateCoupon(Long userId, ValidateCouponRequest request) {
        if (request.getUserCouponId() != null) {
            return validateUserCoupon(
                    userId,
                    request.getUserCouponId(),
                    request.getOrderAmount(),
                    request.getTicketId(),
                    request.getCategoryId()
            );
        }

        ValidateCouponResponse response = new ValidateCouponResponse();
        if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
            response.setUsable(false);
            response.setReason("请提供 userCouponId");
            return response;
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                .isNull(UserCoupon::getOrderId);

        try {
            Long couponId = Long.parseLong(request.getCouponCode());
            wrapper.eq(UserCoupon::getCouponId, couponId);
        } catch (NumberFormatException exception) {
            LambdaQueryWrapper<Coupon> couponWrapper = new LambdaQueryWrapper<>();
            couponWrapper.eq(Coupon::getName, request.getCouponCode());
            Coupon coupon = couponRepository.selectOne(couponWrapper);
            if (coupon != null) {
                wrapper.eq(UserCoupon::getCouponId, coupon.getId());
            }
        }

        UserCoupon userCoupon = userCouponRepository.selectOne(wrapper);
        if (userCoupon == null) {
            response.setUsable(false);
            response.setReason("优惠券不存在或不可用");
            return response;
        }

        try {
            return validateUserCoupon(
                    userId,
                    userCoupon.getId(),
                    request.getOrderAmount(),
                    request.getTicketId(),
                    request.getCategoryId()
            );
        } catch (BusinessException exception) {
            response.setUsable(false);
            response.setReason(exception.getMessage());
            return response;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = requireOwnedCoupon(userId, userCouponId);
        if (!UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_ALREADY_USED, "优惠券已使用");
        }
        if (userCoupon.getValidEndTime() != null && LocalDateTime.now().isAfter(userCoupon.getValidEndTime())) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已过期");
        }
        if (userCoupon.getOrderId() != null && !Objects.equals(orderId, userCoupon.getOrderId())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已绑定其他订单");
        }

        userCoupon.setStatus(UserCoupon.STATUS_USED);
        userCoupon.setUseTime(LocalDateTime.now());
        userCoupon.setOrderId(orderId);
        userCouponRepository.updateById(userCoupon);

        log.info("使用优惠券: userId={}, userCouponId={}, orderId={}", userId, userCouponId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCouponUsed(Long userId, Long userCouponId, Long orderId) {
        useCoupon(userId, userCouponId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnCoupon(Long userId, Long orderId) {
        returnCoupon(userId, orderId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnCoupon(Long userId, Long orderId, boolean forceExpireIfOutOfWindow) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getOrderId, orderId);

        UserCoupon userCoupon = userCouponRepository.selectOne(wrapper);
        if (userCoupon == null) {
            log.warn("退还优惠券失败，未找到对应优惠券: userId={}, orderId={}", userId, orderId);
            return;
        }

        boolean releasable = UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())
                || UserCoupon.STATUS_USED.equals(userCoupon.getStatus());
        if (!releasable) {
            log.warn("退还优惠券失败，状态不支持释放: userId={}, orderId={}, status={}", userId, orderId, userCoupon.getStatus());
            return;
        }

        boolean shouldExpire = forceExpireIfOutOfWindow
                && userCoupon.getValidEndTime() != null
                && LocalDateTime.now().isAfter(userCoupon.getValidEndTime());

        userCoupon.setStatus(shouldExpire ? UserCoupon.STATUS_EXPIRED : UserCoupon.STATUS_UNUSED);
        userCoupon.setUseTime(null);
        userCoupon.setOrderId(null);
        userCouponRepository.updateById(userCoupon);

        log.info("退还优惠券成功: userId={}, userCouponId={}, orderId={}, expired={}",
                userId, userCoupon.getId(), orderId, shouldExpire);
    }

    @Override
    public UserCouponResponse getUserCouponDetail(Long userId, Long userCouponId) {
        UserCoupon userCoupon = requireOwnedCoupon(userId, userCouponId);
        return buildUserCouponResponse(userCoupon);
    }

    private UserCoupon requireOwnedCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.selectById(userCouponId);
        if (userCoupon == null || !Objects.equals(userId, userCoupon.getUserId())) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "用户优惠券不存在");
        }
        return userCoupon;
    }

    private void ensureCouponCanBeApplied(UserCoupon userCoupon, BigDecimal orderAmount) {
        if (!UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_ALREADY_USED, "优惠券已使用");
        }
        if (userCoupon.getOrderId() != null) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已绑定其他订单");
        }

        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getValidStartTime() != null && now.isBefore(userCoupon.getValidStartTime())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券尚未生效");
        }
        if (userCoupon.getValidEndTime() != null && now.isAfter(userCoupon.getValidEndTime())) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已过期");
        }
        if (userCoupon.getMinOrderAmount() != null && orderAmount.compareTo(userCoupon.getMinOrderAmount()) < 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "订单金额不满足最低消费金额");
        }
    }

    private UserCouponResponse buildUserCouponResponse(UserCoupon userCoupon) {
        UserCouponResponse response = new UserCouponResponse();
        response.setId(userCoupon.getId());
        response.setUserId(userCoupon.getUserId());
        response.setCouponId(userCoupon.getCouponId());
        response.setCouponName(userCoupon.getCouponName());
        response.setCouponType(userCoupon.getCouponType());
        response.setCouponTypeText(getCouponTypeText(userCoupon.getCouponType()));
        response.setCouponValue(userCoupon.getCouponValue());
        response.setMinOrderAmount(userCoupon.getMinOrderAmount());
        response.setValidStartTime(userCoupon.getValidStartTime());
        response.setValidEndTime(userCoupon.getValidEndTime());
        response.setReceiveTime(userCoupon.getReceiveTime());
        response.setUseTime(userCoupon.getUseTime());
        response.setStatus(userCoupon.getStatus());
        response.setStatusText(getStatusText(userCoupon.getStatus()));
        response.setSource(userCoupon.getSource());

        if (userCoupon.getValidEndTime() != null) {
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), userCoupon.getValidEndTime());
            response.setRemainingDays((int) Math.max(0, days));
        }

        return response;
    }

    private CouponCenterResponse buildCouponCenterResponse(Long userId, Coupon coupon) {
        CouponCenterResponse response = new CouponCenterResponse();
        response.setCouponId(coupon.getId());
        response.setCouponName(coupon.getName());
        response.setCouponType(coupon.getCouponType());
        response.setCouponValue(coupon.getCouponValue());
        response.setMinOrderAmount(coupon.getMinOrderAmount());
        response.setRemainCount(coupon.getRemainCount());
        response.setPerLimit(coupon.getPerLimit());
        response.setReceiveStartTime(coupon.getReceiveStartTime());
        response.setReceiveEndTime(coupon.getReceiveEndTime());

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .ne(UserCoupon::getStatus, UserCoupon.STATUS_DISABLED);
        response.setReceived(userCouponRepository.selectCount(wrapper) > 0);
        return response;
    }

    private void validateCouponScope(Coupon coupon, Long ticketId, Integer categoryId) {
        if (Coupon.SCOPE_ALL.equals(coupon.getApplicableScope())) {
            return;
        }
        if (Coupon.SCOPE_TICKET.equals(coupon.getApplicableScope())) {
            if (ticketId == null) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不适用于当前票务");
            }
            String applicableTickets = coupon.getApplicableTickets();
            if (applicableTickets != null) {
                JSONArray ticketIds = JSON.parseArray(applicableTickets);
                if (!ticketIds.contains(ticketId)) {
                    throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不适用于当前票务");
                }
            }
            return;
        }
        if (Coupon.SCOPE_CATEGORY.equals(coupon.getApplicableScope())) {
            if (categoryId == null) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不适用于当前分类");
            }
            String applicableCategories = coupon.getApplicableCategories();
            if (applicableCategories != null) {
                JSONArray categoryIds = JSON.parseArray(applicableCategories);
                if (!categoryIds.contains(categoryId)) {
                    throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不适用于当前分类");
                }
            }
        }
    }

    private BigDecimal calculateDiscountAmount(UserCoupon userCoupon, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (Coupon.TYPE_FULL_REDUCE.equals(coupon.getCouponType())) {
            discount = coupon.getCouponValue();
        } else if (Coupon.TYPE_DISCOUNT.equals(coupon.getCouponType())) {
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                    coupon.getCouponValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else if (Coupon.TYPE_CASH.equals(coupon.getCouponType())) {
            discount = coupon.getCouponValue();
        }

        return discount;
    }

    private String getCouponTypeText(Integer type) {
        if (type == null) {
            return "未知";
        }
        if (Coupon.TYPE_FULL_REDUCE.equals(type)) {
            return "满减券";
        }
        if (Coupon.TYPE_DISCOUNT.equals(type)) {
            return "折扣券";
        }
        if (Coupon.TYPE_CASH.equals(type)) {
            return "立减券";
        }
        return "未知";
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (UserCoupon.STATUS_UNUSED.equals(status)) {
            return "未使用";
        }
        if (UserCoupon.STATUS_USED.equals(status)) {
            return "已使用";
        }
        if (UserCoupon.STATUS_EXPIRED.equals(status)) {
            return "已过期";
        }
        if (UserCoupon.STATUS_DISABLED.equals(status)) {
            return "已失效";
        }
        return "未知";
    }
}
