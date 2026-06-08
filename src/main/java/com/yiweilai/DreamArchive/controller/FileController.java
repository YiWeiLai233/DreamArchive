package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.service.MinioService;
import com.yiweilai.DreamArchive.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    @Autowired
    private MinioService minioService;

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }
        // 1. contentType 白名单校验
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return Result.error("仅支持 JPG、PNG、WebP 格式");
        }
        // 2. 文件头（magic bytes）校验，防止伪造 contentType
        String detectedExt;
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            detectedExt = detectImageExtension(header, read);
            if (detectedExt == null) {
                return Result.error("文件内容不是有效的图片");
            }
        } catch (Exception e) {
            return Result.error("文件读取失败");
        }
        try {
            String objectName = minioService.uploadImage(file, detectedExt);
            String url = minioService.getPresignedUrl(objectName);
            return Result.success(Map.of(
                    "objectName", objectName,
                    "url", url != null ? url : ""
            ));
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败，请稍后重试");
        }
    }

    /**
     * 通过文件头魔数识别图片格式，返回扩展名（.jpg/.png/.webp），不匹配返回 null。
     * JPEG: FF D8 FF
     * PNG:  89 50 4E 47 (.PNG)
     * WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
     */
    private String detectImageExtension(byte[] header, int len) {
        if (len >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (len >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return ".png";
        }
        if (len >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return ".webp";
        }
        return null;
    }
}
