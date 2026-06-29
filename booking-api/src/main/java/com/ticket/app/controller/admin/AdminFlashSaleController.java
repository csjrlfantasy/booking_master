package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminFlashSaleRequest;
import com.ticket.app.controller.admin.dto.response.AdminFlashSaleResponse;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketFlashSale;
import com.ticket.app.repository.ticket.TicketFlashSaleRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "管理后台 - 秒杀特价票", description = "配置抢票播报站秒杀活动")
@RestController
@RequestMapping("/admin/flash-sales")
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final TicketRepository ticketRepository;
    private final TicketFlashSaleRepository flashSaleRepository;
    private final JwtUtils jwtUtils;

    @GetMapping
    @Operation(summary = "秒杀活动列表")
    public Result<List<AdminFlashSaleResponse>> list(HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/flash-sales - adminId={}", adminId);
        return Result.success(flashSaleRepository.selectAdminFlashSales());
    }

    @PostMapping
    @Transactional
    @Operation(summary = "创建秒杀活动")
    public Result<Long> create(@RequestBody AdminFlashSaleRequest request, HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("POST /api/admin/flash-sales - adminId={}, ticketId={}", adminId, request != null ? request.getTicketId() : null);

        TicketFlashSale flashSale = buildFlashSale(request);
        flashSaleRepository.insert(flashSale);
        return Result.success(flashSale.getId());
    }

    @PutMapping("/{id}/status")
    @Transactional
    @Operation(summary = "启停秒杀活动")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody AdminFlashSaleRequest request, HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        Integer status = request != null ? request.getStatus() : null;
        log.info("PUT /api/admin/flash-sales/{}/status - adminId={}, status={}", id, adminId, status);

        if (!TicketFlashSale.STATUS_ENABLED.equals(status) && !TicketFlashSale.STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀状态不正确");
        }

        TicketFlashSale existing = flashSaleRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀活动不存在");
        }

        TicketFlashSale updated = new TicketFlashSale();
        updated.setId(id);
        updated.setStatus(status);
        updated.setUpdatedAt(LocalDateTime.now());
        flashSaleRepository.updateById(updated);
        return Result.success("秒杀状态已更新", null);
    }

    private TicketFlashSale buildFlashSale(AdminFlashSaleRequest request) {
        if (request == null || request.getTicketId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请选择票务");
        }
        if (request.getFlashPrice() == null || request.getFlashPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀价必须大于0");
        }
        if (request.getFlashStock() == null || request.getFlashStock() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀库存必须大于0");
        }
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀时间范围不正确");
        }

        Ticket ticket = ticketRepository.selectById(request.getTicketId());
        if (ticket == null || ticket.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "票务不存在");
        }

        TicketFlashSale flashSale = new TicketFlashSale();
        flashSale.setTicketId(request.getTicketId());
        flashSale.setFlashPrice(request.getFlashPrice());
        flashSale.setFlashStock(request.getFlashStock());
        flashSale.setSoldCount(0);
        flashSale.setStartTime(request.getStartTime());
        flashSale.setEndTime(request.getEndTime());
        flashSale.setStatus(request.getStatus() != null ? request.getStatus() : TicketFlashSale.STATUS_ENABLED);
        if (!TicketFlashSale.STATUS_ENABLED.equals(flashSale.getStatus()) && !TicketFlashSale.STATUS_DISABLED.equals(flashSale.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "秒杀状态不正确");
        }
        return flashSale;
    }

    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "Token解析失败");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
