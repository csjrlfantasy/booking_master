package com.ticket.app.controller.banner;

import com.ticket.app.common.Result;
import com.ticket.app.controller.banner.dto.BannerResponse;
import com.ticket.app.repository.banner.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public Result<List<BannerResponse>> list(@RequestParam(required = false) String platform) {
        String targetPlatform = normalizePlatform(platform);
        List<BannerResponse> banners = bannerRepository.selectVisibleByPlatform(targetPlatform, LocalDateTime.now())
                .stream()
                .map(BannerResponse::from)
                .toList();
        return Result.success(banners);
    }

    private String normalizePlatform(String platform) {
        if ("app".equalsIgnoreCase(platform)) {
            return "app";
        }
        return "web";
    }
}
