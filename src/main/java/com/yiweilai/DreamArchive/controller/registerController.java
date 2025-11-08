package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class registerController {
    @PostMapping("/register")
    public String setUser(@RequestBody User user){

    return "200";
    }
}
