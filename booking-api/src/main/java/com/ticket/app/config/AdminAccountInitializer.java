package com.ticket.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.util.PasswordUtils;
import com.ticket.app.entity.User;
import com.ticket.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, DEFAULT_ADMIN_USERNAME);
        User existingAdmin = userRepository.selectOne(wrapper);
        if (existingAdmin != null) {
            log.info("默认管理员账号已存在: username={}", DEFAULT_ADMIN_USERNAME);
            return;
        }

        User admin = new User();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(PasswordUtils.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setPhone("13800000000");
        admin.setEmail("admin@ticketmore.local");
        admin.setNickname("系统管理员");
        admin.setUserType(User.USER_TYPE_ENTERPRISE);
        admin.setUserLevel(10);
        admin.setStatus(User.STATUS_NORMAL);
        admin.setSource("admin");
        admin.setLoginFailCount(0);

        userRepository.insert(admin);
        log.info("已创建默认管理员账号: username={}", DEFAULT_ADMIN_USERNAME);
    }
}
