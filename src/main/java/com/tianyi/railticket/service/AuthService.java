package com.tianyi.railticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianyi.railticket.common.exception.BizException;
import com.tianyi.railticket.common.exception.ErrorCode;
import com.tianyi.railticket.dto.LoginDTO;
import com.tianyi.railticket.entity.UserDO;
import com.tianyi.railticket.mapper.UserMapper;
import com.tianyi.railticket.vo.LoginVO;
import com.tianyi.railticket.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 登录：校验凭证 → 签发 token */
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 按手机号查用户（@TableLogic 已自动过滤注销用户）
        UserDO user = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getPhone, loginDTO.getPhone()));

        // 2. 查不到或密码不匹配 → 同一错误码，防账号枚举（|| 短路同时兜住 user 为 null）
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 签发 token：Sa-Token 生成随机串，把 loginId 写进 Redis
        StpUtil.login(user.getId());
        log.info("登录成功 | userId={}", user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        return vo;
    }

    /** 当前登录用户：前端刷新页面后确认登录态用 */
    public UserInfoVO currentUser() {
        UserDO user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            // token 还有效但用户已被注销 → 立刻踢掉会话，别让幽灵账号继续用
            StpUtil.logout();
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        return vo;
    }

}
