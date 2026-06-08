package com.yiweilai.DreamArchive.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MinioServiceTest {

    @Test
    void presignedUrlUsesPublicEndpointWhenConfigured() {
        new ApplicationContextRunner()
                .withBean(MinioService.class)
                .withPropertyValues(
                        "minio.endpoint=http://127.0.0.1:1",
                        "minio.public-endpoint=http://storage.example.com:9000",
                        "minio.access-key=test-access-key",
                        "minio.secret-key=test-secret-key",
                        "minio.bucket=dream-archive"
                )
                .run(context -> {
                    MinioService service = context.getBean(MinioService.class);

                    String url = service.getPresignedUrl("dreams/avatar.jpg");

                    assertThat(url).startsWith("http://storage.example.com:9000/dream-archive/dreams/avatar.jpg?");
                    assertThat(url).doesNotContain("127.0.0.1");
                    assertThat(url).doesNotContain("minio:9000");
                });
    }

    @Test
    void extractObjectNameReadsBucketPathFromAnyPresignedHost() {
        MinioService service = new MinioService();
        ReflectionTestUtils.setField(service, "endpoint", "http://minio:9000");
        ReflectionTestUtils.setField(service, "bucket", "dream-archive");

        String objectName = service.extractObjectName(
                "http://storage.example.com:9000/dream-archive/dreams/avatar.jpg?X-Amz-Expires=604800"
        );

        assertThat(objectName).isEqualTo("dreams/avatar.jpg");
    }

    @Test
    void extractObjectNameKeepsRawObjectName() {
        MinioService service = new MinioService();

        assertThat(service.extractObjectName("dreams/avatar.jpg")).isEqualTo("dreams/avatar.jpg");
    }
}
