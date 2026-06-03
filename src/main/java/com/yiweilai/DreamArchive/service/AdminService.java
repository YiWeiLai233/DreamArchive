package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AdminDreamDetailRequest;
import com.yiweilai.DreamArchive.DTO.AdminOverview;
import com.yiweilai.DreamArchive.DTO.AdminOverviewRequest;
import com.yiweilai.DreamArchive.DTO.AdminUserActionRequest;
import com.yiweilai.DreamArchive.DTO.AdminUserDeleteRequest;
import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.AdminMapper;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private static final int DEFAULT_USER_PAGE_SIZE = 10;
    private static final int DEFAULT_DREAM_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private DreamContentMapper dreamContentMapper;

    @Autowired
    private SensitiveDataEncryptor sensitiveDataEncryptor;

    public Result<AdminOverview> getOverview(String authorizationHeader, AdminOverviewRequest request) {
        Result<User> authResult = requireAdmin(authorizationHeader);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getCode(), authResult.getMessage());
        }

        return Result.success(buildOverview(request));
    }

    @Transactional
    public Result<AdminOverview> handleUserAction(String authorizationHeader, AdminUserActionRequest request) {
        Result<User> authResult = requireAdmin(authorizationHeader);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getCode(), authResult.getMessage());
        }
        if (request == null || !hasText(request.getAction())) {
            return Result.error(400, "缺少管理员操作类型");
        }

        String action = request.getAction().trim().toUpperCase();
        User currentAdmin = authResult.getData();
        Result<Void> actionResult = switch (action) {
            case "CREATE" -> createUser(request);
            case "UPDATE" -> updateUser(currentAdmin, request);
            case "BAN" -> updateStatus(currentAdmin, request, "BANNED");
            case "UNBAN" -> updateStatus(currentAdmin, request, "ACTIVE");
            default -> Result.error(400, "不支持的管理员操作");
        };

        if (actionResult.getCode() != 200) {
            return Result.error(actionResult.getCode(), actionResult.getMessage());
        }
        return Result.success("操作成功", buildOverview(null));
    }

    @Transactional
    public Result<AdminOverview> deleteUser(String authorizationHeader, AdminUserDeleteRequest request) {
        Result<User> authResult = requireAdmin(authorizationHeader);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getCode(), authResult.getMessage());
        }

        Result<Void> deleteResult = softDeleteUser(authResult.getData(), request == null ? null : request.getId());
        if (deleteResult.getCode() != 200) {
            return Result.error(deleteResult.getCode(), deleteResult.getMessage());
        }
        return Result.success("账号已删除", buildOverview(null));
    }

    public Result<DreamContent> getDreamDetail(String authorizationHeader, AdminDreamDetailRequest request) {
        Result<User> authResult = requireAdmin(authorizationHeader);
        if (authResult.getCode() != 200) {
            return Result.error(authResult.getCode(), authResult.getMessage());
        }
        if (request == null || !hasText(request.getId())) {
            return Result.error(400, "缺少梦境ID");
        }

        DreamContent dream = dreamContentMapper.selectById(request.getId().trim());
        if (dream == null) {
            return Result.error(404, "梦境不存在");
        }
        return Result.success(dream);
    }

    private Result<User> requireAdmin(String authorizationHeader) {
        User user = currentPrincipal();
        if (user == null) {
            TokenService.AuthenticatedUser currentUser;
            try {
                currentUser = tokenService.parseBearerToken(authorizationHeader);
            } catch (IllegalArgumentException e) {
                return Result.error(401, e.getMessage());
            }
            user = loginMapper.selectById(currentUser.getUserId());
        }
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            return Result.error(401, "登录用户不存在");
        }
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            return Result.error(403, "账号已被封禁");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return Result.error(403, "需要管理员权限");
        }
        return Result.success(user);
    }

    private User currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private AdminOverview buildOverview(AdminOverviewRequest request) {
        int userPageSize = normalizePageSize(request == null ? null : request.getUserPageSize(), DEFAULT_USER_PAGE_SIZE);
        int dreamPageSize = normalizePageSize(request == null ? null : request.getDreamPageSize(), DEFAULT_DREAM_PAGE_SIZE);
        String userKeyword = trim(request == null ? null : request.getUserKeyword());
        String dreamKeyword = trim(request == null ? null : request.getDreamKeyword());

        int userResultTotal = adminMapper.countUserSummaries(userKeyword);
        int dreamResultTotal = adminMapper.countRecentDreams(dreamKeyword);
        int userTotalPages = totalPages(userResultTotal, userPageSize);
        int dreamTotalPages = totalPages(dreamResultTotal, dreamPageSize);
        int userPage = normalizePage(request == null ? null : request.getUserPage(), userTotalPages);
        int dreamPage = normalizePage(request == null ? null : request.getDreamPage(), dreamTotalPages);

        AdminOverview overview = new AdminOverview();
        overview.setTotalUsers(adminMapper.countUsers());
        overview.setAdminUsers(adminMapper.countAdminUsers());
        overview.setTotalDreams(adminMapper.countDreams());
        overview.setTodayDreams(adminMapper.countTodayDreams());
        overview.setUsers(adminMapper.selectUserSummaries(userKeyword, offset(userPage, userPageSize), userPageSize));
        overview.setRecentDreams(adminMapper.selectRecentDreams(dreamKeyword, offset(dreamPage, dreamPageSize), dreamPageSize));
        overview.setUserPage(userPage);
        overview.setUserPageSize(userPageSize);
        overview.setUserResultTotal(userResultTotal);
        overview.setUserTotalPages(userTotalPages);
        overview.setDreamPage(dreamPage);
        overview.setDreamPageSize(dreamPageSize);
        overview.setDreamResultTotal(dreamResultTotal);
        overview.setDreamTotalPages(dreamTotalPages);
        return overview;
    }

    private int normalizePageSize(Integer pageSize, int defaultPageSize) {
        if (pageSize == null || pageSize <= 0) {
            return defaultPageSize;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int totalPages(int total, int pageSize) {
        return Math.max(1, (int) Math.ceil(total / (double) pageSize));
    }

    private int normalizePage(Integer page, int totalPages) {
        if (page == null || page <= 0) {
            return 1;
        }
        return Math.min(page, totalPages);
    }

    private int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }

    private Result<Void> createUser(AdminUserActionRequest request) {
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String password = request.getPassword();

        Result<Void> validation = validateBaseUserFields(username, email);
        if (validation.getCode() != 200) {
            return validation;
        }
        if (!hasText(password) || password.length() < 6) {
            return Result.error(400, "密码长度至少 6 位");
        }
        if (adminMapper.countByUsernameExceptId(username, null) > 0) {
            return Result.error(400, "该用户名已被使用");
        }
        if (adminMapper.countByEmailExceptId(email, null) > 0) {
            return Result.error(400, "该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(sensitiveDataEncryptor.encrypt(password));
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setDeleted(false);

        int inserted = adminMapper.insertUser(user);
        return inserted > 0 ? Result.success(null) : Result.error("新增账号失败");
    }

    private Result<Void> updateUser(User currentAdmin, AdminUserActionRequest request) {
        User target = findExistingTarget(request.getId());
        if (target == null) {
            return Result.error(404, "账号不存在或已被删除");
        }
        if (isSuperAdmin(target)) {
            return Result.error(403, "超级管理员不可编辑");
        }

        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        Result<Void> validation = validateBaseUserFields(username, email);
        if (validation.getCode() != 200) {
            return validation;
        }
        if (hasText(request.getPassword()) && request.getPassword().length() < 6) {
            return Result.error(400, "密码长度至少 6 位");
        }
        if (adminMapper.countByUsernameExceptId(username, target.getId()) > 0) {
            return Result.error(400, "该用户名已被使用");
        }
        if (adminMapper.countByEmailExceptId(email, target.getId()) > 0) {
            return Result.error(400, "该邮箱已被注册");
        }

        String role = normalizeRole(hasText(request.getRole()) ? request.getRole() : target.getRole());
        String status = normalizeStatus(hasText(request.getStatus()) ? request.getStatus() : target.getStatus());
        if (target.getId() == currentAdmin.getId() && !"ADMIN".equalsIgnoreCase(role)) {
            return Result.error(400, "不能取消自己的管理员权限");
        }
        if (target.getId() == currentAdmin.getId() && "BANNED".equalsIgnoreCase(status)) {
            return Result.error(400, "不能封禁自己的账号");
        }

        target.setUsername(username);
        target.setEmail(email);
        target.setRole(role);
        target.setStatus(status);
        target.setDeleted(false);
        target.setPassword(hasText(request.getPassword()) ? sensitiveDataEncryptor.encrypt(request.getPassword()) : null);

        int updated = adminMapper.updateUser(target);
        return updated > 0 ? Result.success(null) : Result.error("修改账号失败");
    }

    private Result<Void> softDeleteUser(User currentAdmin, Integer id) {
        User target = findExistingTarget(id);
        if (target == null) {
            return Result.error(404, "账号不存在或已被删除");
        }
        if (isSuperAdmin(target)) {
            return Result.error(403, "超级管理员不可删除");
        }
        if (target.getId() == currentAdmin.getId()) {
            return Result.error(400, "不能删除自己的账号");
        }

        int updated = adminMapper.softDeleteUser(target.getId());
        return updated > 0 ? Result.success(null) : Result.error("删除账号失败");
    }

    private Result<Void> updateStatus(User currentAdmin, AdminUserActionRequest request, String status) {
        User target = findExistingTarget(request.getId());
        if (target == null) {
            return Result.error(404, "账号不存在或已被删除");
        }
        if (isSuperAdmin(target)) {
            return Result.error(403, "超级管理员不可封禁");
        }
        if (target.getId() == currentAdmin.getId() && "BANNED".equalsIgnoreCase(status)) {
            return Result.error(400, "不能封禁自己的账号");
        }

        int updated = adminMapper.updateUserStatus(target.getId(), status);
        return updated > 0 ? Result.success(null) : Result.error("更新账号状态失败");
    }

    private User findExistingTarget(Integer id) {
        if (id == null) {
            return null;
        }
        User target = adminMapper.selectUserById(id);
        if (target == null || Boolean.TRUE.equals(target.getDeleted())) {
            return null;
        }
        return target;
    }

    private Result<Void> validateBaseUserFields(String username, String email) {
        if (!hasText(username)) {
            return Result.error(400, "用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            return Result.error(400, "用户名长度必须在3-20个字符之间");
        }
        if (!hasText(email)) {
            return Result.error(400, "邮箱不能为空");
        }
        if (!email.contains("@")) {
            return Result.error(400, "邮箱格式不正确");
        }
        return Result.success(null);
    }

    private String normalizeRole(String role) {
        if ("SUPER_ADMIN".equalsIgnoreCase(trim(role))) return "SUPER_ADMIN";
        return "ADMIN".equalsIgnoreCase(trim(role)) ? "ADMIN" : "USER";
    }

    private String normalizeStatus(String status) {
        return "BANNED".equalsIgnoreCase(trim(status)) ? "BANNED" : "ACTIVE";
    }

    private boolean isSuperAdmin(User user) {
        return user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
