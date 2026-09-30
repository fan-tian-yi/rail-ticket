package com.tianyi.railticket.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String nickname;
}
