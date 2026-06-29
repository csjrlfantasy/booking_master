package com.ticket.app.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AdminRouteMappingTest {

    @Test
    void adminControllersShouldNotRepeatServletContextPath() {
        assertRequestMapping(AdminStatsController.class, "/admin/stats");
        assertRequestMapping(AdminUserController.class, "/admin");
        assertRequestMapping(AdminOrderController.class, "/admin");
        assertRequestMapping(AdminTicketController.class, "/admin/tickets");
        assertRequestMapping(AdminConfigController.class, "/admin/config");
    }

    private void assertRequestMapping(Class<?> controllerClass, String expectedPath) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[] { expectedPath }, requestMapping.value(), controllerClass.getSimpleName());
    }
}
