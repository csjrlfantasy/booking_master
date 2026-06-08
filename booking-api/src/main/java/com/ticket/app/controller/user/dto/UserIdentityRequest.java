package com.ticket.app.controller.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserIdentityRequest {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    private String idCardNo;

    private String phone;

    private Boolean self = false;

    private Boolean defaultIdentity = false;
}
