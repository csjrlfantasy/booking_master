package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.controller.admin.dto.request.AdminBannerRequest;
import com.ticket.app.entity.Banner;
import com.ticket.app.repository.banner.BannerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBannerControllerTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private AdminBannerController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminBannerController(bannerRepository, jwtUtils);
    }

    @Test
    void createShouldPersistBannerFields() {
        AdminBannerRequest request = new AdminBannerRequest();
        request.setTitle("移动端首页大图");
        request.setImageUrl("/api/uploads/banner/app.jpg");
        request.setPlatform("app");
        request.setLinkType("ticket");
        request.setLinkTarget("9");
        request.setSortOrder(20);
        request.setStatus(Banner.STATUS_ONLINE);

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);

        Result<Long> result = controller.create(request, httpRequest);

        assertTrue(result.isSuccess());
        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).insert(captor.capture());
        Banner saved = captor.getValue();
        assertEquals("移动端首页大图", saved.getTitle());
        assertEquals("/api/uploads/banner/app.jpg", saved.getImageUrl());
        assertEquals("app", saved.getPlatform());
        assertEquals("ticket", saved.getLinkType());
        assertEquals("9", saved.getLinkTarget());
        assertEquals(20, saved.getSortOrder());
        assertEquals(Banner.STATUS_ONLINE, saved.getStatus());
    }

    @Test
    void createShouldRejectMissingImage() {
        AdminBannerRequest request = new AdminBannerRequest();
        request.setTitle("无图 Banner");
        request.setPlatform("web");

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(jwtUtils.getUserIdFromToken("admin-token")).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.create(request, httpRequest));
    }
}
