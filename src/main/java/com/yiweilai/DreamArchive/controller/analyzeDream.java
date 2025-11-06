package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.Dream;
import com.yiweilai.DreamArchive.DTO.DreamRequest;
import com.yiweilai.DreamArchive.service.DreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class analyzeDream {
    @Autowired
    DreamService dreamService;
    @PostMapping("/analysisDream")
    @CrossOrigin(origins = "*")
    public Dream analysisDream(@RequestBody Dream dream){
        return dreamService.setDream(
                dream.getTime(),
                dream.getPlace(),
                dream.getEmotion(),
                dream.getContent(),
                dream.getInterpretation()
        );
    }
    @CrossOrigin(origins = "*")
    @GetMapping("/dream/{id}")
    public Dream dream(@PathVariable String id){
        return dreamService.getDreamById(id);
    }
}
