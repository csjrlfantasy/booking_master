package com.ticket.app.controller.banner;

import com.ticket.app.common.Result;
import com.ticket.app.controller.banner.dto.BannerResponse;
import com.ticket.app.entity.Banner;
import com.ticket.app.repository.banner.BannerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerControllerTest {

    @Mock
    private BannerRepository bannerRepository;

    @Test
    void listShouldReturnVisibleBannersForRequestedPlatform() {
        Banner banner = new Banner();
        banner.setId(7L);
        banner.setTitle("票多星首页推荐");
        banner.setImageUrl("/api/uploads/banner/home.jpg");
        banner.setLinkType("ticket");
        banner.setLinkTarget("12");
        banner.setPlatform("web");
        banner.setSortOrder(3);
        banner.setStatus(Banner.STATUS_ONLINE);

        when(bannerRepository.selectVisibleByPlatform(any(String.class), any(LocalDateTime.class)))
                .thenReturn(List.of(banner));

        BannerController controller = new BannerController(bannerRepository);
        Result<List<BannerResponse>> result = controller.list("web");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(7L, result.getData().get(0).getId());
        assertEquals("票多星首页推荐", result.getData().get(0).getTitle());
        assertEquals("/api/uploads/banner/home.jpg", result.getData().get(0).getImageUrl());
        assertEquals("ticket", result.getData().get(0).getLinkType());
        assertEquals("12", result.getData().get(0).getLinkTarget());
        verify(bannerRepository).selectVisibleByPlatform(any(String.class), any(LocalDateTime.class));
    }

    @Test
    void listShouldDefaultToWebPlatform() {
        BannerController controller = new BannerController(bannerRepository);

        controller.list(null);

        verify(bannerRepository).selectVisibleByPlatform(org.mockito.ArgumentMatchers.eq("web"), any(LocalDateTime.class));
    }
}
