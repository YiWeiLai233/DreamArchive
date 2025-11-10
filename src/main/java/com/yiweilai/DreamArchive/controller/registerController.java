package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.registerService;
import com.yiweilai.DreamArchive.util.passwordEncrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class registerController {
    @Autowired
    registerService registerservice;
  @Autowired
    passwordEncrypt pe;
    @PostMapping("/register")

    public String setUser(@RequestBody User user){
        //密码加密存储
        String Username= user.getUsername();
        String Password= user.getPassword();
        String Email = user.getEmail();
    return registerservice.newUser(Username,pe.encrypt(Password),Email);
    }
}
