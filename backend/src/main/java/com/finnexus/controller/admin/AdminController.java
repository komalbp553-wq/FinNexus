package com.finnexus.controller.admin;

import com.finnexus.domain.dto.AdminStatsResponse;
import com.finnexus.domain.dto.AdminUserResponse;
import com.finnexus.service.AdminService;
import com.finnexus.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> users() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long id) {
        userService.setUserEnabled(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable Long id) {
        userService.setUserEnabled(id, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminService.stats());
    }
}
