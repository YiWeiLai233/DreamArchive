package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderPoolTest {

    @Test
    void providerPoolDoesNotBindProviderListFromApplicationProperties() {
        assertThat(AiProviderPool.class.getAnnotation(ConfigurationProperties.class)).isNull();
    }

    @Test
    void providerPoolSupportsFullProviderUpdatesForAdminChanges() throws Exception {
        Method method = AiProviderPool.class.getMethod("updateProvider", String.class, AiProvider.class);
        AiProviderPool pool = new AiProviderPool();
        pool.setProviders(List.of(new AiProvider(
                "mimo",
                "https://old.example.com/v1",
                "old-key",
                "old-model",
                10,
                true
        )));
        pool.init();

        AiProvider replacement = new AiProvider(
                "ignored-name",
                "https://new.example.com/v1",
                "new-key",
                "new-model",
                30,
                false
        );

        AiProvider updated = (AiProvider) method.invoke(pool, "mimo", replacement);

        assertThat(updated.getName()).isEqualTo("mimo");
        assertThat(updated.getUrl()).isEqualTo("https://new.example.com/v1");
        assertThat(updated.getApiKey()).isEqualTo("new-key");
        assertThat(updated.getModel()).isEqualTo("new-model");
        assertThat(updated.getWeight()).isEqualTo(30);
        assertThat(updated.isEnabled()).isFalse();
    }
}
