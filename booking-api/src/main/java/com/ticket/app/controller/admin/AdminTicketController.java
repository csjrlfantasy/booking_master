package com.ticket.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminCreateTicketRequest;
import com.ticket.app.controller.admin.dto.request.AdminUpdateTicketStatusRequest;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员票务控制器
 */
@Slf4j
@Tag(name = "管理后台 - 票务管理", description = "管理员操作票务的接口")
@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final TicketPriceLevelRepository priceLevelRepository;
    private final TicketSessionRepository sessionRepository;
    private final JwtUtils jwtUtils;

    @PostMapping
    @Transactional
    @Operation(summary = "创建票务", description = "管理员创建票务，并同步生成默认票档和默认场次")
    public Result<Long> createTicket(@RequestBody AdminCreateTicketRequest request, HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("POST /api/admin/tickets - 创建票务: adminId={}, name={}", adminId, request != null ? request.getName() : null);

        if (request == null || isBlank(request.getName())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请输入票务名称");
        }

        Ticket ticket = buildTicket(request);
        ticketRepository.insert(ticket);

        for (TicketPriceLevel priceLevel : buildPriceLevels(ticket, request)) {
            priceLevelRepository.insert(priceLevel);
        }

        TicketSession session = buildDefaultSession(ticket);
        sessionRepository.insert(session);

        return Result.success(ticket.getId());
    }

    /**
     * 获取票务列表（管理员）
     * GET /api/admin/tickets
     */
    @GetMapping
    @Operation(summary = "获取票务列表", description = "管理员获取票务列表，支持分页、搜索、筛选")
    public Result<PageResult<?>> getTicketList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "票务状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "是否热门") @RequestParam(required = false) Integer isHot,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/tickets - 获取票务列表: adminId={}, page={}, pageSize={}", adminId, page, pageSize);

        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Ticket::getDeletedAt);
        if (categoryId != null) {
            queryWrapper.eq(Ticket::getCategoryId, categoryId.intValue());
        }
        if (!isBlank(keyword)) {
            String keywordValue = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(Ticket::getName, keywordValue)
                    .or()
                    .like(Ticket::getSubName, keywordValue)
                    .or()
                    .like(Ticket::getVenueName, keywordValue));
        }
        if (status != null) {
            queryWrapper.eq(Ticket::getStatus, status);
        }
        if (isHot != null) {
            queryWrapper.eq(Ticket::getIsHot, isHot);
        }
        queryWrapper.orderByDesc(Ticket::getUpdatedAt).orderByDesc(Ticket::getId);

        IPage<Ticket> pageResult = ticketRepository.selectPage(new Page<>(page, pageSize), queryWrapper);
        PageResult<Ticket> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                page,
                pageSize
        );
        return Result.success(result);
    }

    /**
     * 获取票务详情
     * GET /api/admin/tickets/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取票务详情", description = "根据票务ID获取票务详情")
    public Result<Ticket> getTicketDetail(
            @Parameter(description = "票务ID", required = true) @PathVariable("id") Long ticketId,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        log.info("GET /api/admin/tickets/{id} - 获取票务详情: adminId={}, ticketId={}", adminId, ticketId);

        Ticket ticket = ticketRepository.selectTicketDetailById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "票务不存在");
        }
        return Result.success(ticket);
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "更新票务", description = "管理员更新票务基础信息、海报、默认票档和默认场次")
    public Result<Void> updateTicket(
            @Parameter(description = "票务ID", required = true) @PathVariable("id") Long ticketId,
            @RequestBody AdminCreateTicketRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        log.info("PUT /api/admin/tickets/{id} - 更新票务: adminId={}, ticketId={}, name={}",
                adminId, ticketId, request != null ? request.getName() : null);

        if (request == null || isBlank(request.getName())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请输入票务名称");
        }

        Ticket existingTicket = ticketRepository.selectTicketDetailById(ticketId);
        if (existingTicket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "票务不存在");
        }

        Ticket updatedTicket = buildTicket(request);
        int soldCount = existingTicket.getSaleCount() != null ? existingTicket.getSaleCount() : 0;
        updatedTicket.setId(ticketId);
        Integer nextStatus = request.getStatus() != null
                ? request.getStatus()
                : (existingTicket.getStatus() != null ? existingTicket.getStatus() : Ticket.STATUS_ON_SALE);
        if (!isAllowedStatus(nextStatus)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "绁ㄥ姟鐘舵€佷笉姝ｇ‘");
        }
        updatedTicket.setStatus(nextStatus);
        updatedTicket.setBookStartTime(resolveBookStartTime(nextStatus, existingTicket.getBookStartTime()));
        updatedTicket.setSaleCount(soldCount);
        updatedTicket.setViewCount(existingTicket.getViewCount() != null ? existingTicket.getViewCount() : 0);
        updatedTicket.setAvailableStock(calculateAvailable(updatedTicket.getTotalStock(), soldCount));
        updatedTicket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.updateById(updatedTicket);

        syncPriceLevels(updatedTicket, request);
        syncDefaultSession(updatedTicket);

        return Result.success("票务更新成功", null);
    }

    /**
     * 从请求中获取管理员ID
     */
    @PutMapping("/{id}/status")
    @Transactional
    @Operation(summary = "鏇存柊绁ㄥ姟鐘舵€?", description = "绠＄悊鍛樹笂鏋躲€佷笅鏋躲€佽涓哄嵆灏嗗紑鍞垨缁撴潫绁ㄥ姟")
    public Result<Void> updateTicketStatus(
            @Parameter(description = "绁ㄥ姟ID", required = true) @PathVariable("id") Long ticketId,
            @RequestBody AdminUpdateTicketStatusRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = getAdminIdFromRequest(httpRequest);
        Integer status = request != null ? request.getStatus() : null;
        log.info("PUT /api/admin/tickets/{id}/status - 鏇存柊绁ㄥ姟鐘舵€? adminId={}, ticketId={}, status={}",
                adminId, ticketId, status);

        if (!isAllowedStatus(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "绁ㄥ姟鐘舵€佷笉姝ｇ‘");
        }

        Ticket ticket = ticketRepository.selectById(ticketId);
        if (ticket == null || ticket.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "绁ㄥ姟涓嶅瓨鍦?");
        }

        Ticket updatedTicket = new Ticket();
        updatedTicket.setId(ticketId);
        updatedTicket.setStatus(status);
        updatedTicket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.updateById(updatedTicket);

        return Result.success("绁ㄥ姟鐘舵€佹洿鏂版垚鍔?", null);
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
            log.warn("Token解析异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
    }

    private Ticket buildTicket(AdminCreateTicketRequest request) {
        LocalDate showDate = parseShowDate(request.getShowDate());
        LocalTime showTime = parseShowTime(request.getShowTime());
        BigDecimal minPrice = normalizePrice(request.getPriceMin(), new BigDecimal("99.00"));
        BigDecimal maxPrice = normalizePrice(request.getPriceMax(), minPrice);
        List<AdminCreateTicketRequest.PriceLevelRequest> requestedLevels = validPriceLevels(request);
        if (!requestedLevels.isEmpty()) {
            minPrice = requestedLevels.stream()
                    .map(AdminCreateTicketRequest.PriceLevelRequest::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(minPrice);
            maxPrice = requestedLevels.stream()
                    .map(AdminCreateTicketRequest.PriceLevelRequest::getPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(maxPrice);
        }
        if (maxPrice.compareTo(minPrice) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最高票价不能低于最低票价");
        }

        int stock = request.getTotalStock() != null && request.getTotalStock() > 0 ? request.getTotalStock() : 100;
        if (!requestedLevels.isEmpty()) {
            stock = requestedLevels.stream()
                    .map(AdminCreateTicketRequest.PriceLevelRequest::getTotalStock)
                    .reduce(0, Integer::sum);
        }
        Integer categoryId = request.getCategoryId() != null ? request.getCategoryId() : 1;

        Ticket ticket = new Ticket();
        ticket.setName(request.getName().trim());
        ticket.setSubName(firstNotBlank(request.getSubtitle(), request.getSubName()));
        ticket.setCategoryId(categoryId);
        ticket.setCategoryName(firstNotBlank(request.getCategoryName(), categoryNameOf(categoryId)));
        ticket.setVenueId(request.getVenueId() != null ? request.getVenueId() : 1L);
        ticket.setVenueName(firstNotBlank(request.getVenueName(), "待定场馆"));
        ticket.setPoster(firstNotBlank(request.getPoster(), "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1200&q=80"));
        ticket.setImages("[]");
        ticket.setDescription(firstNotBlank(request.getDescription(), "暂无详情"));
        ticket.setHighlights("[]");
        ticket.setAttentions(firstNotBlank(request.getNotice(), "请凭订单信息入场，禁止倒卖黄牛票。"));
        ticket.setShowTimeType(1);
        ticket.setShowStartDate(showDate);
        ticket.setShowEndDate(showDate);
        ticket.setShowStartTime(showTime);
        ticket.setShowEndTime(showTime.plusHours(2));
        ticket.setDuration(120);
        ticket.setAgeLimit("");
        ticket.setArtistInfo("");
        ticket.setMinimumPrice(minPrice);
        ticket.setMaximumPrice(maxPrice);
        ticket.setTotalStock(stock);
        ticket.setAvailableStock(stock);
        ticket.setPerMaxBuy(6);
        Integer status = request.getStatus() != null ? request.getStatus() : Ticket.STATUS_ON_SALE;
        if (!isAllowedStatus(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "绁ㄥ姟鐘舵€佷笉姝ｇ‘");
        }
        ticket.setBookStartTime(resolveBookStartTime(status, null));
        ticket.setBookEndTime(showDate.atTime(showTime).minusMinutes(30));
        ticket.setIsHot(0);
        ticket.setIsRecommend(1);
        ticket.setStatus(status);
        ticket.setViewCount(0);
        ticket.setSaleCount(0);
        ticket.setSortOrder(0);
        ticket.setTags("[]");
        ticket.setExtInfo("{}");
        return ticket;
    }

    private TicketPriceLevel buildDefaultPriceLevel(Ticket ticket) {
        TicketPriceLevel priceLevel = new TicketPriceLevel();
        priceLevel.setTicketId(ticket.getId());
        priceLevel.setName("标准票");
        priceLevel.setPrice(ticket.getMinimumPrice());
        priceLevel.setCostPrice(BigDecimal.ZERO);
        priceLevel.setTotalStock(ticket.getTotalStock());
        priceLevel.setAvailableStock(ticket.getAvailableStock());
        priceLevel.setSoldCount(0);
        priceLevel.setSeatType(TicketPriceLevel.SEAT_TYPE_FREE);
        priceLevel.setDescription("后台创建默认票档");
        priceLevel.setBenefits("[]");
        priceLevel.setSortOrder(1);
        priceLevel.setStatus(1);
        return priceLevel;
    }

    private List<TicketPriceLevel> buildPriceLevels(Ticket ticket, AdminCreateTicketRequest request) {
        List<AdminCreateTicketRequest.PriceLevelRequest> requestedLevels = validPriceLevels(request);
        if (requestedLevels.isEmpty()) {
            return List.of(buildDefaultPriceLevel(ticket));
        }

        List<TicketPriceLevel> priceLevels = new ArrayList<>();
        for (int i = 0; i < requestedLevels.size(); i++) {
            AdminCreateTicketRequest.PriceLevelRequest requestedLevel = requestedLevels.get(i);
            TicketPriceLevel priceLevel = new TicketPriceLevel();
            priceLevel.setTicketId(ticket.getId());
            priceLevel.setName(firstNotBlank(requestedLevel.getName(), "票档" + (i + 1)));
            priceLevel.setPrice(requestedLevel.getPrice());
            priceLevel.setCostPrice(BigDecimal.ZERO);
            priceLevel.setTotalStock(requestedLevel.getTotalStock());
            priceLevel.setAvailableStock(requestedLevel.getTotalStock());
            priceLevel.setSoldCount(0);
            priceLevel.setSeatType(TicketPriceLevel.SEAT_TYPE_FREE);
            priceLevel.setDescription(firstNotBlank(requestedLevel.getDescription(), "后台创建票档"));
            priceLevel.setBenefits("[]");
            priceLevel.setSortOrder(i + 1);
            priceLevel.setStatus(1);
            priceLevels.add(priceLevel);
        }
        return priceLevels;
    }

    private List<AdminCreateTicketRequest.PriceLevelRequest> validPriceLevels(AdminCreateTicketRequest request) {
        if (request.getPriceLevels() == null) {
            return List.of();
        }
        return request.getPriceLevels().stream()
                .filter(level -> level != null
                        && level.getPrice() != null
                        && level.getPrice().compareTo(BigDecimal.ZERO) > 0
                        && level.getTotalStock() != null
                        && level.getTotalStock() > 0)
                .toList();
    }

    private TicketSession buildDefaultSession(Ticket ticket) {
        TicketSession session = new TicketSession();
        session.setTicketId(ticket.getId());
        session.setSessionNo("ADMIN-" + ticket.getId());
        session.setSessionName("默认场次");
        session.setShowDate(ticket.getShowStartDate());
        session.setShowStartTime(ticket.getShowStartTime());
        session.setShowEndTime(ticket.getShowEndTime());
        session.setWeekDay(ticket.getShowStartDate().getDayOfWeek().toString());
        session.setTotalCapacity(ticket.getTotalStock());
        session.setAvailableCapacity(ticket.getAvailableStock());
        session.setStatus(TicketSession.STATUS_ON_SALE);
        return session;
    }

    private void syncDefaultPriceLevel(Ticket ticket) {
        List<TicketPriceLevel> priceLevels = priceLevelRepository.selectByTicketId(ticket.getId());
        if (priceLevels == null || priceLevels.isEmpty()) {
            priceLevelRepository.insert(buildDefaultPriceLevel(ticket));
            return;
        }

        TicketPriceLevel priceLevel = priceLevels.get(0);
        int soldCount = priceLevel.getSoldCount() != null ? priceLevel.getSoldCount() : 0;
        priceLevel.setName(firstNotBlank(priceLevel.getName(), "标准票"));
        priceLevel.setPrice(ticket.getMinimumPrice());
        priceLevel.setTotalStock(ticket.getTotalStock());
        priceLevel.setAvailableStock(calculateAvailable(ticket.getTotalStock(), soldCount));
        priceLevel.setSeatType(priceLevel.getSeatType() != null ? priceLevel.getSeatType() : TicketPriceLevel.SEAT_TYPE_FREE);
        priceLevel.setDescription(firstNotBlank(priceLevel.getDescription(), "后台默认票档"));
        priceLevel.setBenefits(firstNotBlank(priceLevel.getBenefits(), "[]"));
        priceLevel.setSortOrder(priceLevel.getSortOrder() != null ? priceLevel.getSortOrder() : 1);
        priceLevel.setStatus(1);
        priceLevelRepository.updateById(priceLevel);
    }

    private void syncPriceLevels(Ticket ticket, AdminCreateTicketRequest request) {
        List<AdminCreateTicketRequest.PriceLevelRequest> requestedLevels = validPriceLevels(request);
        if (requestedLevels.isEmpty()) {
            syncDefaultPriceLevel(ticket);
            return;
        }

        List<TicketPriceLevel> existingLevels = priceLevelRepository.selectByTicketId(ticket.getId());
        for (int i = 0; i < requestedLevels.size(); i++) {
            AdminCreateTicketRequest.PriceLevelRequest requestedLevel = requestedLevels.get(i);
            if (existingLevels != null && i < existingLevels.size()) {
                TicketPriceLevel priceLevel = existingLevels.get(i);
                int soldCount = priceLevel.getSoldCount() != null ? priceLevel.getSoldCount() : 0;
                priceLevel.setName(firstNotBlank(requestedLevel.getName(), "票档" + (i + 1)));
                priceLevel.setPrice(requestedLevel.getPrice());
                priceLevel.setTotalStock(requestedLevel.getTotalStock());
                priceLevel.setAvailableStock(calculateAvailable(requestedLevel.getTotalStock(), soldCount));
                priceLevel.setSeatType(priceLevel.getSeatType() != null ? priceLevel.getSeatType() : TicketPriceLevel.SEAT_TYPE_FREE);
                priceLevel.setDescription(firstNotBlank(requestedLevel.getDescription(), "后台票档"));
                priceLevel.setBenefits(firstNotBlank(priceLevel.getBenefits(), "[]"));
                priceLevel.setSortOrder(i + 1);
                priceLevel.setStatus(1);
                priceLevelRepository.updateById(priceLevel);
            } else {
                TicketPriceLevel priceLevel = buildPriceLevels(ticket, request).get(i);
                priceLevelRepository.insert(priceLevel);
            }
        }

        if (existingLevels != null && existingLevels.size() > requestedLevels.size()) {
            for (int i = requestedLevels.size(); i < existingLevels.size(); i++) {
                TicketPriceLevel disabledLevel = existingLevels.get(i);
                disabledLevel.setStatus(0);
                priceLevelRepository.updateById(disabledLevel);
            }
        }
    }

    private void syncDefaultSession(Ticket ticket) {
        List<TicketSession> sessions = sessionRepository.selectByTicketId(ticket.getId());
        if (sessions == null || sessions.isEmpty()) {
            sessionRepository.insert(buildDefaultSession(ticket));
            return;
        }

        TicketSession session = sessions.get(0);
        session.setSessionNo(firstNotBlank(session.getSessionNo(), "ADMIN-" + ticket.getId()));
        session.setSessionName(firstNotBlank(session.getSessionName(), "默认场次"));
        session.setShowDate(ticket.getShowStartDate());
        session.setShowStartTime(ticket.getShowStartTime());
        session.setShowEndTime(ticket.getShowEndTime());
        session.setWeekDay(ticket.getShowStartDate().getDayOfWeek().toString());
        session.setTotalCapacity(ticket.getTotalStock());
        int soldCount = ticket.getSaleCount() != null ? ticket.getSaleCount() : 0;
        session.setAvailableCapacity(calculateAvailable(ticket.getTotalStock(), soldCount));
        session.setStatus(TicketSession.STATUS_ON_SALE);
        sessionRepository.updateById(session);
    }

    private int calculateAvailable(Integer totalStock, Integer soldCount) {
        int total = totalStock != null ? totalStock : 0;
        int sold = soldCount != null ? soldCount : 0;
        return Math.max(0, total - sold);
    }

    private LocalDateTime resolveBookStartTime(Integer status, LocalDateTime currentBookStartTime) {
        if (Ticket.STATUS_PENDING.equals(status)) {
            if (currentBookStartTime != null && currentBookStartTime.isAfter(LocalDateTime.now())) {
                return currentBookStartTime;
            }
            return LocalDateTime.now().plusDays(3);
        }
        return LocalDateTime.now().minusMinutes(1);
    }

    private boolean isAllowedStatus(Integer status) {
        return Ticket.STATUS_OFFLINE.equals(status)
                || Ticket.STATUS_PENDING.equals(status)
                || Ticket.STATUS_ON_SALE.equals(status)
                || Ticket.STATUS_SOLD_OUT.equals(status)
                || Ticket.STATUS_ENDED.equals(status);
    }

    private LocalDate parseShowDate(String value) {
        if (isBlank(value)) {
            return LocalDate.now().plusDays(30);
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "演出日期格式不正确");
        }
    }

    private LocalTime parseShowTime(String value) {
        if (isBlank(value)) {
            return LocalTime.of(19, 30);
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "演出时间格式不正确");
        }
    }

    private BigDecimal normalizePrice(BigDecimal value, BigDecimal defaultValue) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return defaultValue;
        }
        return value;
    }

    private String categoryNameOf(Integer categoryId) {
        if (categoryId == null) {
            return "演唱会";
        }
        return switch (categoryId) {
            case 2 -> "音乐会";
            case 3 -> "戏剧/话剧";
            case 4 -> "舞蹈/芭蕾";
            case 5 -> "体育赛事";
            default -> "演唱会";
        };
    }

    private String firstNotBlank(String first, String second) {
        return !isBlank(first) ? first.trim() : second;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
