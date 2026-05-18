package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.LoginService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        return loginService.login(username, password);
    }
}
