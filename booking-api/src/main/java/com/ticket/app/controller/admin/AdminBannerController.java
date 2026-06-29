package com.ticket.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminBannerRequest;
import com.ticket.app.controller.banner.dto.BannerResponse;
import com.ticket.app.entity.Banner;
import com.ticket.app.repository.banner.BannerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerRepository bannerRepository;
    private final JwtUtils jwtUtils;

    @GetMapping
    public Result<List<BannerResponse>> list(@RequestParam(required = false) String platform, HttpServletRequest request) {
        getAdminIdFromRequest(request);
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<Banner>()
                .isNull(Banner::getDeletedAt)
                .orderByAsc(Banner::getSortOrder)
                .orderByDesc(Banner::getId);
        if (!isBlank(platform)) {
            wrapper.eq(Banner::getPlatform, normalizePlatform(platform));
        }
        List<BannerResponse> banners = bannerRepository.selectList(wrapper)
                .stream()
                .map(BannerResponse::from)
                .toList();
        return Result.success(banners);
    }

    @PostMapping
    @Transactional
    public Result<Long> create(@RequestBody AdminBannerRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        validate(request);
        Banner banner = buildBanner(request);
        bannerRepository.insert(banner);
        return Result.success(banner.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody AdminBannerRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        validate(request);
        Banner existing = bannerRepository.selectById(id);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Banner不存在");
        }
        Banner banner = buildBanner(request);
        banner.setId(id);
        banner.setUpdatedAt(LocalDateTime.now());
        bannerRepository.updateById(banner);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Transactional
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody AdminBannerRequest request, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        Integer status = request != null ? request.getStatus() : null;
        if (!Banner.STATUS_ONLINE.equals(status) && !Banner.STATUS_OFFLINE.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Banner状态不正确");
        }
        Banner banner = new Banner();
        banner.setId(id);
        banner.setStatus(status);
        banner.setUpdatedAt(LocalDateTime.now());
        bannerRepository.updateById(banner);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        getAdminIdFromRequest(httpRequest);
        Banner banner = new Banner();
        banner.setId(id);
        banner.setDeletedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        bannerRepository.updateById(banner);
        return Result.success();
    }

    private Banner buildBanner(AdminBannerRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.getTitle().trim());
        banner.setImageUrl(request.getImageUrl().trim());
        banner.setLinkType(normalizeLinkType(request.getLinkType()));
        banner.setLinkTarget(firstNotBlank(request.getLinkTarget(), ""));
        banner.setPlatform(normalizePlatform(request.getPlatform()));
        banner.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        banner.setStatus(request.getStatus() != null ? request.getStatus() : Banner.STATUS_ONLINE);
        banner.setStartTime(request.getStartTime());
        banner.setEndTime(request.getEndTime());
        return banner;
    }

    private void validate(AdminBannerRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请填写Banner信息");
        }
        if (isBlank(request.getTitle())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请填写Banner标题");
        }
        if (isBlank(request.getImageUrl())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请上传Banner图片");
        }
        Integer status = request.getStatus();
        if (status != null && !Banner.STATUS_ONLINE.equals(status) && !Banner.STATUS_OFFLINE.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Banner状态不正确");
        }
    }

    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (isBlank(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (isBlank(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
        return userId;
    }

    private String normalizePlatform(String platform) {
        if ("app".equalsIgnoreCase(platform)) {
            return "app";
        }
        return "web";
    }

    private String normalizeLinkType(String linkType) {
        if ("ticket".equalsIgnoreCase(linkType)
                || "category".equalsIgnoreCase(linkType)
                || "url".equalsIgnoreCase(linkType)) {
            return linkType.toLowerCase();
        }
        return "none";
    }

    private String firstNotBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
