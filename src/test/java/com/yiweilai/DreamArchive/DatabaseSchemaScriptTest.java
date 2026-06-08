package com.yiweilai.DreamArchive;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseSchemaScriptTest {

    @Test
    void dreamContentCreateScriptContainsImageUrlColumnUsedByMapper() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/db/dream_content_table.sql"));

        assertThat(schema).contains("image_url");
    }

    @Test
    void aiProviderCreateScriptContainsPersistentProviderColumns() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/db/ai_provider_table.sql"));

        assertThat(schema)
                .contains("CREATE TABLE IF NOT EXISTS ai_provider")
                .contains("name")
                .contains("url")
                .contains("api_key")
                .contains("model")
                .contains("weight")
                .contains("enabled")
                .contains("vision_enabled");
    }

    @Test
    void dockerInitScriptContainsAiProviderTable() throws Exception {
        String schema = Files.readString(Path.of("sql/init.sql"));

        assertThat(schema)
                .contains("CREATE TABLE `ai_provider`")
                .contains("api_key")
                .contains("vision_enabled");
    }
}
