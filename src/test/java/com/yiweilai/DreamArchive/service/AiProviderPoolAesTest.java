package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import com.yiweilai.DreamArchive.DTO.AiProviderUpdateRequest;
import com.yiweilai.DreamArchive.util.AesEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiProviderPool AES 加密集成测试
 * 验证 apiKey 在内存中为明文、存储时加密的流程
 */
class AiProviderPoolAesTest {

    private static final String TEST_AES_KEY = "TestAesKey12345678901234567890ab";
    private AiProviderPool pool;
    private AesEncryptor aesEncryptor;

    @BeforeEach
    void setUp() {
        aesEncryptor = new AesEncryptor(TEST_AES_KEY);
        pool = new AiProviderPool();
        ReflectionTestUtils.setField(pool, "aesEncryptor", aesEncryptor);
    }

    @Test
    void acquireReturnsProviderWithPlainApiKey() {
        // 模拟从数据库加载的加密数据
        String rawKey = "sk-test-api-key-12345";
        String encryptedKey = aesEncryptor.encrypt(rawKey);

        AiProvider provider = new AiProvider("test", "https://api.test.com/v1", encryptedKey, "gpt-4", 10, true);
        pool.setProviders(List.of(provider));

        // 模拟解密（实际由 loadProvidersFromDatabase 完成）
        ReflectionTestUtils.invokeMethod(pool, "decryptApiKey", provider);

        AiProvider acquired = pool.acquire();
        assertThat(acquired.getApiKey()).isEqualTo(rawKey);
    }

    @Test
    void addProviderEncryptsApiKeyInMemory() {
        String rawKey = "sk-new-provider-key";
        AiProvider provider = new AiProvider("new-provider", "https://api.new.com/v1", rawKey, "gpt-4", 10, true);

        // 不使用数据库，直接测试加密方法
        AiProvider toEncrypt = copyProvider(provider);
        ReflectionTestUtils.invokeMethod(pool, "encryptApiKey", toEncrypt);

        assertThat(toEncrypt.getApiKey()).isNotEqualTo(rawKey);
        assertThat(aesEncryptor.isEncrypted(toEncrypt.getApiKey())).isTrue();
        assertThat(aesEncryptor.decrypt(toEncrypt.getApiKey())).isEqualTo(rawKey);
    }

    @Test
    void updateProviderEncryptsNewApiKey() {
        String oldKey = "sk-old-key";
        String newKey = "sk-new-key";

        AiProvider existing = new AiProvider("provider1", "https://api.old.com/v1", oldKey, "gpt-4", 10, true);
        pool.setProviders(List.of(existing));

        AiProviderUpdateRequest request = new AiProviderUpdateRequest();
        request.setApiKey(newKey);

        // 测试合并逻辑
        AiProvider merged = ReflectionTestUtils.invokeMethod(pool, "mergeProvider", existing, request);
        assertThat(merged.getApiKey()).isEqualTo(newKey);

        // 测试加密
        AiProvider toEncrypt = copyProvider(merged);
        ReflectionTestUtils.invokeMethod(pool, "encryptApiKey", toEncrypt);
        assertThat(aesEncryptor.decrypt(toEncrypt.getApiKey())).isEqualTo(newKey);
    }

    @Test
    void decryptApiKeyHandlesPlainTextGracefully() {
        // 旧数据可能是明文，解密时应保持原样
        AiProvider provider = new AiProvider("old", "https://api.old.com/v1", "plain-text-key", "gpt-4", 10, true);

        ReflectionTestUtils.invokeMethod(pool, "decryptApiKey", provider);

        // 非加密格式保持原样
        assertThat(provider.getApiKey()).isEqualTo("plain-text-key");
    }

    @Test
    void decryptApiKeyHandlesNullGracefully() {
        AiProvider provider = new AiProvider("test", "https://api.test.com/v1", null, "gpt-4", 10, true);

        // 不应抛异常
        ReflectionTestUtils.invokeMethod(pool, "decryptApiKey", provider);
        assertThat(provider.getApiKey()).isNull();
    }

    @Test
    void decryptApiKeyHandlesBlankGracefully() {
        AiProvider provider = new AiProvider("test", "https://api.test.com/v1", "", "gpt-4", 10, true);

        ReflectionTestUtils.invokeMethod(pool, "decryptApiKey", provider);
        assertThat(provider.getApiKey()).isEmpty();
    }

    @Test
    void multipleProvidersEncryptDecryptCorrectly() {
        String key1 = "sk-provider-1-key";
        String key2 = "sk-provider-2-key";
        String key3 = "sk-provider-3-key";

        AiProvider p1 = new AiProvider("p1", "https://api1.com/v1", key1, "gpt-4", 10, true);
        AiProvider p2 = new AiProvider("p2", "https://api2.com/v1", key2, "gpt-3.5", 20, true);
        AiProvider p3 = new AiProvider("p3", "https://api3.com/v1", key3, "claude-3", 15, false);

        // 加密
        for (AiProvider p : List.of(p1, p2, p3)) {
            ReflectionTestUtils.invokeMethod(pool, "encryptApiKey", p);
        }

        // 验证都已加密
        assertThat(aesEncryptor.isEncrypted(p1.getApiKey())).isTrue();
        assertThat(aesEncryptor.isEncrypted(p2.getApiKey())).isTrue();
        assertThat(aesEncryptor.isEncrypted(p3.getApiKey())).isTrue();

        // 解密验证
        for (AiProvider p : List.of(p1, p2, p3)) {
            ReflectionTestUtils.invokeMethod(pool, "decryptApiKey", p);
        }
        assertThat(p1.getApiKey()).isEqualTo(key1);
        assertThat(p2.getApiKey()).isEqualTo(key2);
        assertThat(p3.getApiKey()).isEqualTo(key3);
    }

    @Test
    void encryptApiKeyWithNullAesEncryptorDoesNotThrow() {
        ReflectionTestUtils.setField(pool, "aesEncryptor", null);

        AiProvider provider = new AiProvider("test", "https://api.test.com/v1", "sk-test-key", "gpt-4", 10, true);

        // 不应抛异常，无加密器时保持原样
        ReflectionTestUtils.invokeMethod(pool, "encryptApiKey", provider);
        assertThat(provider.getApiKey()).isEqualTo("sk-test-key");
    }

    private AiProvider copyProvider(AiProvider source) {
        AiProvider copy = new AiProvider(
                source.getName(),
                source.getUrl(),
                source.getApiKey(),
                source.getModel(),
                source.getWeight(),
                source.isEnabled()
        );
        copy.setVisionEnabled(source.isVisionEnabled());
        return copy;
    }
}
