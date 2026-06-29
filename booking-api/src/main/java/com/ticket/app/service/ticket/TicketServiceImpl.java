package com.ticket.app.service.ticket;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 票务服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketPriceLevelRepository ticketPriceLevelRepository;
    private final TicketSessionRepository ticketSessionRepository;

    @Override
    public PageResult<?> getTicketList(
            Integer categoryId,
            String keyword,
            String startDate,
            String endDate,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String sortOrder,
            Integer pageNum,
            Integer pageSize
    ) {
        log.info("获取票务列表: categoryId={}, keyword={}, startDate={}, endDate={}, minPrice={}, maxPrice={}, pageNum={}, pageSize={}",
                categoryId, keyword, startDate, endDate, minPrice, maxPrice, pageNum, pageSize);

        // 解析日期 - 支持多种日期格式
        LocalDate startLocalDate = parseDate(startDate);
        LocalDate endLocalDate = parseDate(endDate);

        // 构建分页参数
        Page<Ticket> page = new Page<>(pageNum, pageSize);

        // 查询
        IPage<Ticket> ticketPage = ticketRepository.selectTicketPage(
                page,
                categoryId,
                keyword,
                startLocalDate,
                endLocalDate,
                minPrice,
                maxPrice,
                sortBy != null ? sortBy : "showStartDate",
                sortOrder != null ? sortOrder : "asc"
        );

        // 转换为VO
        List<TicketListVO> voList = ticketPage.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, ticketPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public Object getTicketDetail(Long ticketId) {
        log.info("获取票务详情: ticketId={}", ticketId);

        // 查询票务详情
        Ticket ticket = ticketRepository.selectTicketDetailById(ticketId);
        if (ticket == null) {
            return null;
        }

        // 查询票档列表
        List<TicketPriceLevel> priceLevels = ticketPriceLevelRepository.selectByTicketId(ticketId);
        List<TicketSession> sessions = ticketSessionRepository.selectByTicketId(ticketId);

        // 组装详情VO
        return convertToDetailVO(ticket, priceLevels, sessions);
    }

    @Override
    public List<?> getHotTickets(Integer limit) {
        log.info("获取热门票务: limit={}", limit);

        List<Ticket> tickets = ticketRepository.selectHotTickets(limit);

        List<TicketListVO> voList = tickets.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 转换为列表VO
     */
    @Override
    public List<?> getUpcomingTickets(Integer limit) {
        log.info("鑾峰彇鍗虫将寮€鍞エ鍔? limit={}", limit);

        List<Ticket> tickets = ticketRepository.selectUpcomingTickets(limit);

        return tickets.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
    }

    private TicketListVO convertToListVO(Ticket ticket) {
        TicketListVO vo = new TicketListVO();
        vo.setId(ticket.getId());
        vo.setTitle(ticket.getName());
        vo.setPrice(ticket.getMinimumPrice() != null ? ticket.getMinimumPrice().doubleValue() : null);
        vo.setVenue(ticket.getVenueName());
        
        if (ticket.getShowStartDate() != null) {
            vo.setDate(ticket.getShowStartDate().toString());
        }
        
        vo.setCategory(ticket.getCategoryName());
        vo.setImageUrl(ticket.getPoster());
        vo.setStatus(ticket.getStatus());
        vo.setStatusName(getStatusName(ticket.getStatus()));
        
        return vo;
    }

    /**
     * 转换为详情VO
     */
    private TicketDetailVO convertToDetailVO(Ticket ticket, List<TicketPriceLevel> priceLevels, List<TicketSession> sessions) {
        TicketDetailVO vo = new TicketDetailVO();
        vo.setId(ticket.getId());
        vo.setName(ticket.getName());
        vo.setSubName(ticket.getSubName());
        vo.setPoster(ticket.getPoster());
        vo.setDescription(ticket.getDescription());
        vo.setVenueName(ticket.getVenueName());
        vo.setVenueId(ticket.getVenueId());
        
        if (ticket.getShowStartDate() != null) {
            vo.setShowDate(ticket.getShowStartDate().toString());
        }
        
        if (ticket.getShowStartTime() != null) {
            vo.setShowTime(ticket.getShowStartTime().toString());
        }
        
        vo.setDuration(ticket.getDuration());
        vo.setAgeLimit(ticket.getAgeLimit());
        vo.setMinimumPrice(ticket.getMinimumPrice() != null ? ticket.getMinimumPrice().doubleValue() : null);
        vo.setMaximumPrice(ticket.getMaximumPrice() != null ? ticket.getMaximumPrice().doubleValue() : null);
        vo.setTotalStock(ticket.getTotalStock());
        vo.setAvailableStock(ticket.getAvailableStock());
        vo.setPerMaxBuy(ticket.getPerMaxBuy());
        vo.setViewCount(ticket.getViewCount());
        vo.setSaleCount(ticket.getSaleCount());
        vo.setStatus(ticket.getStatus());

        // 转换票档
        if (priceLevels != null && !priceLevels.isEmpty()) {
            List<PriceLevelVO> levelVOList = priceLevels.stream()
                    .map(level -> {
                        PriceLevelVO levelVO = new PriceLevelVO();
                        levelVO.setId(level.getId());
                        levelVO.setName(level.getName());
                        levelVO.setPrice(level.getPrice() != null ? level.getPrice().doubleValue() : null);
                        levelVO.setAvailableStock(level.getAvailableStock());
                        levelVO.setSeatType(level.getSeatType());
                        levelVO.setDescription(level.getDescription());
                        return levelVO;
                    })
                    .collect(Collectors.toList());
            vo.setPriceLevels(levelVOList);
        }
        if (sessions != null && !sessions.isEmpty()) {
            List<SessionVO> sessionVOList = sessions.stream()
                    .filter(session -> TicketSession.STATUS_ON_SALE.equals(session.getStatus()))
                    .map(session -> {
                        SessionVO sessionVO = new SessionVO();
                        sessionVO.setId(session.getId());
                        sessionVO.setSessionName(session.getSessionName());
                        sessionVO.setShowDate(session.getShowDate() != null ? session.getShowDate().toString() : null);
                        sessionVO.setShowTime(session.getShowStartTime() != null ? session.getShowStartTime().toString() : null);
                        sessionVO.setEndTime(session.getShowEndTime() != null ? session.getShowEndTime().toString() : null);
                        sessionVO.setAvailableCapacity(session.getAvailableCapacity());
                        sessionVO.setStatus(session.getStatus());
                        return sessionVO;
                    })
                    .collect(Collectors.toList());
            vo.setSessions(sessionVOList);
        }

        return vo;
    }

    /**
     * 解析日期 - 支持多种格式
     * BUG-007修复：支持多种日期格式，避免400错误
     */
    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        // 支持的日期格式列表
        String[] patterns = {
            "yyyy-MM-dd",           // ISO格式
            "yyyy/MM/dd",           // 斜杠格式
            "yyyy.MM.dd",           // 点分隔格式
            "yyyyMMdd",             // 无分隔符格式
            "yyyy-MM",              // 年月格式
            "yyyy/MM",              // 年月斜杠格式
        };
        
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // 尝试下一个格式
            }
        }
        
        // 所有格式都解析失败，返回null（不抛异常）
        log.warn("无法解析日期字符串: {}, 将忽略此过滤条件", dateStr);
        return null;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "已下架";
            case 1: return "待开售";
            case 2: return "售票中";
            case 3: return "已售罄";
            case 4: return "演出结束";
            default: return "未知";
        }
    }

    /**
     * 列表VO
     */
    public static class TicketListVO {
        private Long id;
        private String title;
        private Double price;
        private String venue;
        private String date;
        private String category;
        private String imageUrl;
        private Integer status;
        private String statusName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
    }

    /**
     * 详情VO
     */
    public static class TicketDetailVO {
        private Long id;
        private Long venueId;
        private String name;
        private String subName;
        private String poster;
        private String description;
        private String venueName;
        private String showDate;
        private String showTime;
        private Integer duration;
        private String ageLimit;
        private Double minimumPrice;
        private Double maximumPrice;
        private Integer totalStock;
        private Integer availableStock;
        private Integer perMaxBuy;
        private Integer viewCount;
        private Integer saleCount;
        private Integer status;
        private List<PriceLevelVO> priceLevels;
        private List<SessionVO> sessions;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getVenueId() { return venueId; }
        public void setVenueId(Long venueId) { this.venueId = venueId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSubName() { return subName; }
        public void setSubName(String subName) { this.subName = subName; }
        public String getPoster() { return poster; }
        public void setPoster(String poster) { this.poster = poster; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVenueName() { return venueName; }
        public void setVenueName(String venueName) { this.venueName = venueName; }
        public String getShowDate() { return showDate; }
        public void setShowDate(String showDate) { this.showDate = showDate; }
        public String getShowTime() { return showTime; }
        public void setShowTime(String showTime) { this.showTime = showTime; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public String getAgeLimit() { return ageLimit; }
        public void setAgeLimit(String ageLimit) { this.ageLimit = ageLimit; }
        public Double getMinimumPrice() { return minimumPrice; }
        public void setMinimumPrice(Double minimumPrice) { this.minimumPrice = minimumPrice; }
        public Double getMaximumPrice() { return maximumPrice; }
        public void setMaximumPrice(Double maximumPrice) { this.maximumPrice = maximumPrice; }
        public Integer getTotalStock() { return totalStock; }
        public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
        public Integer getAvailableStock() { return availableStock; }
        public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
        public Integer getPerMaxBuy() { return perMaxBuy; }
        public void setPerMaxBuy(Integer perMaxBuy) { this.perMaxBuy = perMaxBuy; }
        public Integer getViewCount() { return viewCount; }
        public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
        public Integer getSaleCount() { return saleCount; }
        public void setSaleCount(Integer saleCount) { this.saleCount = saleCount; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public List<PriceLevelVO> getPriceLevels() { return priceLevels; }
        public void setPriceLevels(List<PriceLevelVO> priceLevels) { this.priceLevels = priceLevels; }
        public List<SessionVO> getSessions() { return sessions; }
        public void setSessions(List<SessionVO> sessions) { this.sessions = sessions; }
    }

    /**
     * 票档VO
     */
    public static class SessionVO {
        private Long id;
        private String sessionName;
        private String showDate;
        private String showTime;
        private String endTime;
        private Integer availableCapacity;
        private Integer status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSessionName() { return sessionName; }
        public void setSessionName(String sessionName) { this.sessionName = sessionName; }
        public String getShowDate() { return showDate; }
        public void setShowDate(String showDate) { this.showDate = showDate; }
        public String getShowTime() { return showTime; }
        public void setShowTime(String showTime) { this.showTime = showTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public Integer getAvailableCapacity() { return availableCapacity; }
        public void setAvailableCapacity(Integer availableCapacity) { this.availableCapacity = availableCapacity; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class PriceLevelVO {
        private Long id;
        private String name;
        private Double price;
        private Integer availableStock;
        private Integer seatType;
        private String description;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Integer getAvailableStock() { return availableStock; }
        public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
        public Integer getSeatType() { return seatType; }
        public void setSeatType(Integer seatType) { this.seatType = seatType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
