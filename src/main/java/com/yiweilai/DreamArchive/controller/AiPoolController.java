package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import com.yiweilai.DreamArchive.DTO.AiProviderUpdateRequest;
import com.yiweilai.DreamArchive.service.AiProviderPool;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-pool")
public class AiPoolController {

    @Autowired
    private AiProviderPool providerPool;

    @GetMapping("/providers")
    public Result<List<AiProvider>> listProviders() {
        return Result.success("获取成功", providerPool.getProviders());
    }

    @PostMapping("/providers")
    public Result<String> addProvider(@RequestBody AiProvider provider) {
        try {
            providerPool.addProvider(provider);
            return Result.success("添加成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/providers/{name}/update")
    public Result<AiProvider> updateProvider(
            @PathVariable String name,
            @RequestBody(required = false) AiProviderUpdateRequest request,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean visionEnabled) {
        try {
            AiProviderUpdateRequest effectiveRequest = request != null ? request : new AiProviderUpdateRequest();
            if (weight != null) {
                effectiveRequest.setWeight(weight);
            }
            if (enabled != null) {
                effectiveRequest.setEnabled(enabled);
            }
            if (visionEnabled != null) {
                effectiveRequest.setVisionEnabled(visionEnabled);
            }
            AiProvider updated = providerPool.updateProvider(name, effectiveRequest);
            if (updated == null) {
                return Result.error("provider [" + name + "] 不存在");
            }
            return Result.success("更新成功", updated);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/providers/{name}/delete")
    public Result<String> removeProvider(@PathVariable String name) {
        if (providerPool.removeProvider(name)) {
            return Result.success("删除成功");
        }
        return Result.error("provider [" + name + "] 不存在");
    }

    @PostMapping("/providers/{name}/reset")
    public Result<String> resetCircuit(@PathVariable String name) {
        if (providerPool.resetCircuit(name)) {
            return Result.success("熔断已重置");
        }
        return Result.error("provider [" + name + "] 不存在");
    }
}
