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
}
