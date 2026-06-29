package com.ticket.app.controller.ticket;

import com.ticket.app.common.PageResult;
import com.ticket.app.common.Result;
import com.ticket.app.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 票务控制器
 */
@Slf4j
@RestController
@RequestMapping("/tickets")
@Tag(name = "票务管理", description = "票务列表、详情、搜索、热门推荐相关接口")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 获取票务列表
     * GET /api/tickets
     */
    @GetMapping
    @Operation(summary = "获取票务列表", description = "分页获取票务列表，按日期排序，支持分类筛选")
    public Result<PageResult<?>> list(
            @Parameter(description = "分类ID") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "最低价格") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "排序方式") @RequestParam(required = false, defaultValue = "date") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        log.info("GET /api/tickets - 获取票务列表: categoryId={}, keyword={}, startDate={}, endDate={}, minPrice={}, maxPrice={}, sortBy={}, sortOrder={}, pageNum={}, pageSize={}",
                categoryId, keyword, startDate, endDate, minPrice, maxPrice, sortBy, sortOrder, pageNum, pageSize);

        PageResult<?> result = ticketService.getTicketList(
                categoryId,
                keyword,
                startDate,
                endDate,
                minPrice,
                maxPrice,
                sortBy,
                sortOrder,
                pageNum,
                pageSize
        );

        return Result.success(result);
    }

    /**
     * 获取票务详情
     * GET /api/tickets/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取票务详情", description = "获取票务详细信息，包含座位信息")
    public Result<Object> detail(
            @Parameter(description = "票务ID", required = true) @PathVariable Long id
    ) {
        log.info("GET /api/tickets/{id} - 获取票务详情: id={}", id);

        Object result = ticketService.getTicketDetail(id);
        if (result == null) {
            return Result.fail(404, "票务不存在");
        }

        return Result.success(result);
    }

    /**
     * 票务搜索
     * GET /api/tickets/search
     */
    @GetMapping("/search")
    @Operation(summary = "票务搜索", description = "支持关键词搜索、价格区间筛选、日期筛选")
    public Result<PageResult<?>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "最低价格") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "排序方式") @RequestParam(required = false, defaultValue = "date") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        log.info("GET /api/tickets/search - 票务搜索: keyword={}, minPrice={}, maxPrice={}, startDate={}, endDate={}, sortBy={}, sortOrder={}, pageNum={}, pageSize={}",
                keyword, minPrice, maxPrice, startDate, endDate, sortBy, sortOrder, pageNum, pageSize);

        PageResult<?> result = ticketService.getTicketList(
                null,
                keyword,
                startDate,
                endDate,
                minPrice,
                maxPrice,
                sortBy,
                sortOrder,
                pageNum,
                pageSize
        );

        return Result.success(result);
    }

    /**
     * 获取热门票务
     * GET /api/tickets/hot
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门票务", description = "返回热门推荐票务，按销量/浏览量排序")
    public Result<?> hot(
            @Parameter(description = "返回数量") @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        log.info("GET /api/tickets/hot - 获取热门票务: limit={}", limit);

        var result = ticketService.getHotTickets(limit);

        return Result.success(result);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "鑾峰彇鍗虫将寮€鍞エ鍔?", description = "杩斿洖寰呭紑鍞垨鏈埌寮€鍞椂闂寸殑绁ㄥ姟")
    public Result<?> upcoming(
            @Parameter(description = "杩斿洖鏁伴噺") @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        log.info("GET /api/tickets/upcoming - 鑾峰彇鍗虫将寮€鍞エ鍔? limit={}", limit);

        var result = ticketService.getUpcomingTickets(limit);

        return Result.success(result);
    }
}
