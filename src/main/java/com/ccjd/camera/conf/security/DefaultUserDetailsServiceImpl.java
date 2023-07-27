package com.ccjd.camera.conf.security;

import com.ccjd.camera.conf.security.dto.LoginUser;
import com.ccjd.camera.service.IUserService;
import com.ccjd.camera.storager.dao.dto.User;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户登录认证逻辑
 */
@Component
public class DefaultUserDetailsServiceImpl implements UserDetailsService {

    private final static Logger logger = LoggerFactory.getLogger(DefaultUserDetailsServiceImpl.class);

    @Autowired
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StrUtil.isBlank(username)) {
            logger.info("登录用户：{} 不存在", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        }

        // 查出密码
        User user = userService.getUserByUsername(username);
        if (user == null) {
            logger.info("登录用户：{} 不存在", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        }
        String password = SecurityUtils.encryptPassword(user.getPassword());
        user.setPassword(password);
        return new LoginUser(user, LocalDateTime.now());
    }


}
