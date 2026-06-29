package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.controller.ticket.dto.TicketFlashSaleResponse;
import com.ticket.app.repository.ticket.TicketFlashSaleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tickets/flash-sale")
@Tag(name = "秒杀特价票", description = "客户侧秒杀票展示接口")
@RequiredArgsConstructor
public class TicketFlashSaleController {

    private final TicketFlashSaleRepository flashSaleRepository;

    @GetMapping
    @Operation(summary = "获取秒杀特价票", description = "抢票播报站使用此接口，不再混用热门票")
    public Result<List<TicketFlashSaleResponse>> list(
            @Parameter(description = "返回数量") @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("GET /api/tickets/flash-sale - limit={}", limit);
        return Result.success(flashSaleRepository.selectActiveFlashSaleTickets(limit));
    }
}
