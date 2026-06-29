package com.ticket.app.controller.ticket;

import com.ticket.app.common.Result;
import com.ticket.app.service.ticket.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/categories")
@Tag(name = "分类管理", description = "获取票务分类接口")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类
     * GET /api/categories
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "返回所有票务分类，包含id、name、icon、count")
    public Result<List<?>> list() {
        log.info("GET /api/categories - 获取所有分类");

        List<?> result = categoryService.getCategoriesWithCount();

        return Result.success(result);
    }
}