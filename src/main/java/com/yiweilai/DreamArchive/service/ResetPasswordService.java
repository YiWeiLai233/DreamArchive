package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.mapper.ResetPasswordMapper;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordService {

    private static final Logger log = LoggerFactory.getLogger(ResetPasswordService.class);

    @Autowired
    private SensitiveDataEncryptor sensitiveDataEncryptor;

    @Autowired
    private ResetPasswordMapper resetPasswordMapper;

    @Autowired
    private LoginMapper loginMapper;

    /**
     * 检查用户是否存在
     */
    public boolean checkUserExists(String identifier) {
        if (identifier.contains("@")) {
            return loginMapper.selectByEmail(identifier) != null;
        } else {
            return loginMapper.selectByUsername(identifier) != null;
        }
    }

    /**
     * 重置密码
     */
    public Result<String> resetPassword(String identifier, String newPassword) {
        try {
            // 检查用户是否存在
            if (!checkUserExists(identifier)) {
                return Result.error("用户不存在");
            }

            // 加密新密码
            String encryptedPassword = sensitiveDataEncryptor.encrypt(newPassword);

            // 执行密码重置
            int rows;
            if (identifier.contains("@")) {
                rows = resetPasswordMapper.resetByEmail(identifier, encryptedPassword);
            } else {
                rows = resetPasswordMapper.resetByUsername(identifier, encryptedPassword);
            }

            if (rows > 0) {
                return Result.success("密码重置成功");
            } else {
                return Result.error("密码重置失败");
            }
        } catch (Exception e) {
            log.error("密码重置失败", e);
            return Result.error("密码重置失败，请稍后重试");
        }
    }
}
