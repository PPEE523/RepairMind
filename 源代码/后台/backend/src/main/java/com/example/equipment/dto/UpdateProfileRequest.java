package com.example.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在2-50之间")
    private String username;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;
}
