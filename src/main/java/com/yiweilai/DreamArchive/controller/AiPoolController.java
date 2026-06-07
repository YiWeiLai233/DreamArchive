package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import com.yiweilai.DreamArchive.DTO.AiProviderUpdateRequest;
import com.yiweilai.DreamArchive.mapper.AiProviderMapper;
import com.yiweilai.DreamArchive.service.AiProviderPool;
import com.yiweilai.DreamArchive.util.AesEncryptor;
import com.yiweilai.DreamArchive.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-pool")
public class AiPoolController {

    private static final Logger log = LoggerFactory.getLogger(AiPoolController.class);

    @Autowired
    private AiProviderPool providerPool;

    @Autowired(required = false)
    private AiProviderMapper aiProviderMapper;

    @Autowired(required = false)
    private AesEncryptor aesEncryptor;

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

    /**
     * 迁移接口：加密数据库中所有明文 apiKey（仅需调用一次）
     */
    @PostMapping("/providers/encrypt-api-keys")
    public Result<String> encryptAllApiKeys() {
        if (aiProviderMapper == null || aesEncryptor == null) {
            return Result.error("加密组件未就绪");
        }
        try {
            List<AiProvider> providers = aiProviderMapper.selectAll();
            int encrypted = 0;
            for (AiProvider p : providers) {
                if (p.getApiKey() != null && !p.getApiKey().isBlank() && !aesEncryptor.isEncrypted(p.getApiKey())) {
                    String encryptedKey = aesEncryptor.encrypt(p.getApiKey());
                    p.setApiKey(encryptedKey);
                    aiProviderMapper.update(p);
                    encrypted++;
                    log.info("Encrypted apiKey for provider: {}", p.getName());
                }
            }
            // 重新加载内存
            providerPool.refreshFromDatabase();
            return Result.success("加密完成，共处理 " + encrypted + " 个 provider");
        } catch (Exception e) {
            log.error("Failed to encrypt api keys", e);
            return Result.error("加密失败: " + e.getMessage());
        }
    }
}
