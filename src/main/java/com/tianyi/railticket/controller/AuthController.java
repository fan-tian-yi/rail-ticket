package com.tianyi.railticket.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.dto.LoginDTO;
import com.tianyi.railticket.service.AuthService;
import com.tianyi.railticket.vo.LoginVO;
import com.tianyi.railticket.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /** 登录：手机号 + 密码换 token */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.ok(authService.login(loginDTO));
    }

    /** 登出：让当前 token 立即失效 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /** 当前登录用户 */
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.currentUser());
    }
}
