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

/**
 * 优惠券服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receiveCoupon(Long userId, Long couponId) {
        // 查询优惠券
        Coupon coupon = couponRepository.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "优惠券不存在");
        }
        
        // 检查优惠券状态
        if (!Coupon.STATUS_ACTIVE.equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券不可领取");
        }
        
        // 检查库存
        if (coupon.getRemainCount() <= 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已领完");
        }
        
        // 检查领取时间
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getReceiveStartTime() != null && now.isBefore(coupon.getReceiveStartTime())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券领取尚未开始");
        }
        if (coupon.getReceiveEndTime() != null && now.isAfter(coupon.getReceiveEndTime())) {
            throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券领取已结束");
        }
        
        // 检查用户已领取数量
        LambdaQueryWrapper<UserCoupon> userCouponWrapper = new LambdaQueryWrapper<>();
        userCouponWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId)
                .ne(UserCoupon::getStatus, UserCoupon.STATUS_DISABLED);
        Long receivedCount = userCouponRepository.selectCount(userCouponWrapper);
        
        if (receivedCount >= coupon.getPerLimit()) {
            throw new BusinessException(ResultCode.COUPON_REACH_LIMIT, "已达到领取上限");
        }
        
        // 计算有效期
        LocalDateTime validStartTime;
        LocalDateTime validEndTime;
        
        if (Coupon.VALID_TYPE_FIXED.equals(coupon.getValidityType())) {
            validStartTime = coupon.getValidStartDate().atStartOfDay();
            validEndTime = coupon.getValidEndDate().atTime(23, 59, 59);
        } else {
            validStartTime = now;
            validEndTime = now.plusDays(coupon.getValidDays());
        }
        
        // 创建用户优惠券
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
        
        // 扣减库存
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
                .le(UserCoupon::getValidStartTime, now)
                .ge(UserCoupon::getValidEndTime, now);
        
        List<UserCoupon> coupons = userCouponRepository.selectList(wrapper);
        
        // 过滤符合条件的优惠券
        List<UserCouponResponse> availableCoupons = new ArrayList<>();
        for (UserCoupon uc : coupons) {
            // 检查订单金额是否满足最低消费
            if (uc.getMinOrderAmount() != null && orderAmount.compareTo(uc.getMinOrderAmount()) < 0) {
                continue;
            }
            
            // 检查适用范围
            Coupon coupon = couponRepository.selectById(uc.getCouponId());
            if (coupon == null) continue;
            
            if (Coupon.SCOPE_ALL.equals(coupon.getApplicableScope())) {
                // 全场通用
            } else if (Coupon.SCOPE_TICKET.equals(coupon.getApplicableScope())) {
                // 指定票务
                if (ticketId == null) continue;
                String applicableTickets = coupon.getApplicableTickets();
                if (applicableTickets != null) {
                    JSONArray ticketIds = JSON.parseArray(applicableTickets);
                    if (!ticketIds.contains(ticketId)) continue;
                }
            } else if (Coupon.SCOPE_CATEGORY.equals(coupon.getApplicableScope())) {
                // 指定分类
                if (categoryId == null) continue;
                String applicableCategories = coupon.getApplicableCategories();
                if (applicableCategories != null) {
                    JSONArray categoryIds = JSON.parseArray(applicableCategories);
                    if (!categoryIds.contains(categoryId)) continue;
                }
            }
            
            availableCoupons.add(buildUserCouponResponse(uc));
        }
        
        return availableCoupons;
    }

    @Override
    public ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long ticketId, Integer categoryId) {
        UserCoupon userCoupon = userCouponRepository.selectById(userCouponId);
        if (userCoupon == null || !Objects.equals(userId, userCoupon.getUserId())) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "用户优惠券不存在");
        }

        if (!UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_ALREADY_USED, "优惠券已使用");
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
    public ValidateCouponResponse validateCoupon(Long userId, ValidateCouponRequest request) {
        ValidateCouponResponse response = new ValidateCouponResponse();
        
        // 尝试通过优惠券码查找用户优惠券
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED);
        
        // 如果传入的是数字ID
        try {
            Long couponId = Long.parseLong(request.getCouponCode());
            wrapper.eq(UserCoupon::getCouponId, couponId);
        } catch (NumberFormatException e) {
            // 如果传入的是优惠券码，需要先查询优惠券
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
            response.setReason("优惠券不存在或已使用");
            return response;
        }
        
        // 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(userCoupon.getValidStartTime())) {
            response.setUsable(false);
            response.setReason("优惠券尚未生效");
            return response;
        }
        if (now.isAfter(userCoupon.getValidEndTime())) {
            response.setUsable(false);
            response.setReason("优惠券已过期");
            return response;
        }
        
        // 检查订单金额
        if (userCoupon.getMinOrderAmount() != null && 
            request.getOrderAmount().compareTo(userCoupon.getMinOrderAmount()) < 0) {
            response.setUsable(false);
            response.setReason("订单金额不满足最低消费" + userCoupon.getMinOrderAmount() + "元");
            return response;
        }
        
        // 计算优惠金额
        BigDecimal discountAmount = calculateDiscountAmount(userCoupon, request.getOrderAmount());
        
        response.setUsable(true);
        response.setUserCouponId(userCoupon.getId());
        response.setCouponId(userCoupon.getCouponId());
        response.setCouponName(userCoupon.getCouponName());
        response.setCouponType(userCoupon.getCouponType());
        response.setCouponValue(userCoupon.getCouponValue());
        response.setDiscountAmount(discountAmount);
        response.setMinOrderAmount(userCoupon.getMinOrderAmount());
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponRepository.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "用户优惠券不存在");
        }
        
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权使用此优惠券");
        }
        
        if (!UserCoupon.STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_ALREADY_USED, "优惠券已使用");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(userCoupon.getValidEndTime())) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已过期");
        }
        
        userCoupon.setStatus(UserCoupon.STATUS_USED);
        userCoupon.setUseTime(now);
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
                .eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, UserCoupon.STATUS_USED);
        
        UserCoupon userCoupon = userCouponRepository.selectOne(wrapper);
        if (userCoupon == null) {
            log.warn("退还优惠券失败，未找到对应优惠券: userId={}, orderId={}", userId, orderId);
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        boolean outOfWindow = userCoupon.getValidEndTime() != null && now.isAfter(userCoupon.getValidEndTime());
        if (forceExpireIfOutOfWindow && outOfWindow) {
            userCoupon.setStatus(UserCoupon.STATUS_EXPIRED);
        } else {
            userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
        }
        userCoupon.setUseTime(null);
        userCoupon.setOrderId(null);
        userCouponRepository.updateById(userCoupon);
        
        log.info("退还优惠券成功: userId={}, userCouponId={}, orderId={}", userId, userCoupon.getId(), orderId);
    }

    @Override
    public UserCouponResponse getUserCouponDetail(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "用户优惠券不存在");
        }
        
        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权查看此优惠券");
        }
        
        return buildUserCouponResponse(userCoupon);
    }

    /**
     * 构建用户优惠券响应
     */
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
        
        // 计算剩余有效期天数
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

    /**
     * 计算优惠金额
     */
    private BigDecimal calculateDiscountAmount(UserCoupon userCoupon, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.selectById(userCoupon.getCouponId());
        if (coupon == null) return BigDecimal.ZERO;
        
        BigDecimal discount = BigDecimal.ZERO;
        
        if (Coupon.TYPE_FULL_REDUCE.equals(coupon.getCouponType())) {
            // 满减券
            discount = coupon.getCouponValue();
        } else if (Coupon.TYPE_DISCOUNT.equals(coupon.getCouponType())) {
            // 折扣券
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                    coupon.getCouponValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            // 检查最高优惠金额
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else if (Coupon.TYPE_CASH.equals(coupon.getCouponType())) {
            // 立减券
            discount = coupon.getCouponValue();
        }
        
        return discount;
    }

    /**
     * 获取优惠券类型文本
     */
    private String getCouponTypeText(Integer type) {
        if (type == null) return "未知";
        if (Coupon.TYPE_FULL_REDUCE.equals(type)) return "满减券";
        if (Coupon.TYPE_DISCOUNT.equals(type)) return "折扣券";
        if (Coupon.TYPE_CASH.equals(type)) return "立减券";
        return "未知";
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        if (UserCoupon.STATUS_UNUSED.equals(status)) return "未使用";
        if (UserCoupon.STATUS_USED.equals(status)) return "已使用";
        if (UserCoupon.STATUS_EXPIRED.equals(status)) return "已过期";
        if (UserCoupon.STATUS_DISABLED.equals(status)) return "已失效";
        return "未知";
    }
}
