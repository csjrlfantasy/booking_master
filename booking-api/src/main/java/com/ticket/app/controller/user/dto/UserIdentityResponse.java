package com.ticket.app.controller.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserIdentityResponse {
    private Long id;
    private String realName;
    private String idCardNo;
    private String maskedIdCardNo;
    private String phone;
    private Boolean self;
    private Boolean defaultIdentity;
}
