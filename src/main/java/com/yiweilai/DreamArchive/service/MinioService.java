package com.yiweilai.DreamArchive.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file, String extension) {
        try {
            String objectName = "dreams/" + UUID.randomUUID() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload image to MinIO", e);
            throw new RuntimeException("图片上传失败");
        }
    }

    /**
     * 从完整 URL 或预签名 URL 中提取 objectName。
     * 如果输入已经是纯 objectName（不含 http），直接返回。
     */
    public String extractObjectName(String urlOrObjectName) {
        if (urlOrObjectName == null || urlOrObjectName.isEmpty()) {
            return urlOrObjectName;
        }
        if (!urlOrObjectName.startsWith("http")) {
            return urlOrObjectName;
        }
        // URL 格式: endpoint/bucket/objectName?params
        String prefix = endpoint + "/" + bucket + "/";
        int idx = urlOrObjectName.indexOf(prefix);
        if (idx >= 0) {
            String path = urlOrObjectName.substring(idx + prefix.length());
            int queryIdx = path.indexOf('?');
            return queryIdx >= 0 ? path.substring(0, queryIdx) : path;
        }
        return urlOrObjectName;
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 下载图片并返回 base64 data URL（供多模态 AI 使用）
     */
    public String getImageAsBase64(String objectName) {
        try (var stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build())) {
            byte[] bytes = stream.readAllBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            // 根据 objectName 扩展名推断 MIME 类型
            String mime = "image/jpeg";
            String lower = objectName.toLowerCase();
            if (lower.endsWith(".png")) mime = "image/png";
            else if (lower.endsWith(".gif")) mime = "image/gif";
            else if (lower.endsWith(".webp")) mime = "image/webp";
            return "data:" + mime + ";base64," + base64;
        } catch (Exception e) {
            log.error("Failed to download image for base64: {}", e.getMessage());
            return null;
        }
    }

    public void deleteObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete MinIO object: {}", e.getMessage());
        }
    }
}
