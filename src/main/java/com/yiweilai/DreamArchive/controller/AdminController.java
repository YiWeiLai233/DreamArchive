package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.AdminDreamDetailRequest;
import com.yiweilai.DreamArchive.DTO.AdminOverview;
import com.yiweilai.DreamArchive.DTO.AdminOverviewRequest;
import com.yiweilai.DreamArchive.DTO.AdminUserActionRequest;
import com.yiweilai.DreamArchive.DTO.AdminUserDeleteRequest;
import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.service.AdminService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/overview")
    public Result<AdminOverview> getOverview(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) AdminOverviewRequest request) {
        return adminService.getOverview(authorizationHeader, request);
    }

    @PostMapping("/user-action")
    public Result<AdminOverview> handleUserAction(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminUserActionRequest request) {
        return adminService.handleUserAction(authorizationHeader, request);
    }

    @PostMapping("/delete-user")
    public Result<AdminOverview> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminUserDeleteRequest request) {
        return adminService.deleteUser(authorizationHeader, request);
    }

    @PostMapping("/dream-detail")
    public Result<DreamContent> getDreamDetail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody AdminDreamDetailRequest request) {
        return adminService.getDreamDetail(authorizationHeader, request);
    }
}
