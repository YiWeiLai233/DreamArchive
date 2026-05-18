package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.RegisterService;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.passwordEncrypt;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RegisterController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private passwordEncrypt passwordEncrypt;

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        String encryptedPassword = passwordEncrypt.encrypt(user.getPassword());
        return registerService.newUser(user.getUsername(), encryptedPassword, user.getEmail());
    }
}
