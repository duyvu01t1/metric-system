package com.tailorshop.metric.config;

import com.tailorshop.metric.entity.User;
import com.tailorshop.metric.entity.UserRole;
import com.tailorshop.metric.repository.UserRepository;
import com.tailorshop.metric.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Initialize database with default data on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeRoles();
        initializeAdminUser();
    }

    /**
     * Initialize default user roles
     */
    private void initializeRoles() {
        String[] roleNames = {"ADMIN", "USER", "MANAGER", "SALE", "STAFF"};

        for (String roleName : roleNames) {
            Optional<UserRole> existingRole = userRoleRepository.findByName(roleName);
            if (existingRole.isEmpty()) {
                UserRole role = new UserRole();
                role.setName(roleName);
                String desc = switch (roleName) {
                    case "ADMIN"   -> "Quản trị viên — toàn quyền hệ thống";
                    case "MANAGER" -> "Quản lý — duyệt phân khách, xem báo cáo";
                    case "SALE"    -> "Nhân viên kinh doanh — chăm sóc khách, xử lý lead";
                    case "STAFF"   -> "Thợ may — thực hiện sản xuất";
                    default        -> roleName + " role";
                };
                role.setDescription(desc);
                userRoleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    /**
     * Initialize default admin user
     */
    private void initializeAdminUser() {
        String adminUsername = "admin";
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

        if (existingAdmin.isEmpty()) {
            // Get ADMIN role
            UserRole adminRole = userRoleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            // Create admin user
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setEmail("admin@tailorsystem.local");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123")); // Default password
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setIsActive(true);
            adminUser.setIsLocked(false);

            Set<UserRole> roles = new HashSet<>();
            roles.add(adminRole);
            adminUser.setRoles(roles);

            userRepository.save(adminUser);
            log.info("Created default admin user with username: {} and password: admin123", adminUsername);
            log.warn("IMPORTANT: Change admin password in production!");
        }
    }

}
