package com.campus.trade.file.service;

import com.campus.trade.common.constant.Constant;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.ResultCode;
import com.campus.trade.file.dto.FileUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private static final Set<String> ALLOWED_TYPES = Set.of("product", "avatar");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private final Path uploadRoot;

    public FileServiceImpl(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public FileUploadResponse storeImage(MultipartFile file, Long userId, String type) {
        validateRequest(file, userId, type);
        String extension = detectImageExtension(file);
        String month = LocalDate.now().format(MONTH_FORMATTER);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = uploadRoot.resolve(month).resolve(String.valueOf(userId)).normalize();
        Path destination = directory.resolve(filename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法文件路径");
        }

        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败");
        }

        String url = "/uploads/" + month + "/" + userId + "/" + filename;
        return new FileUploadResponse(url, type.toLowerCase(Locale.ROOT), file.getOriginalFilename(), file.getSize());
    }

    private void validateRequest(MultipartFile file, Long userId, String type) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > Constant.MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件用途仅支持 product 或 avatar");
        }
    }

    private String detectImageExtension(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = file.getInputStream()) {
            length = inputStream.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无法读取上传文件");
        }

        if (length >= 3 && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF) {
            return "jpg";
        }
        if (length >= 8 && unsigned(header[0]) == 0x89 && header[1] == 0x50 && header[2] == 0x4E
                && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A
                && header[6] == 0x1A && header[7] == 0x0A) {
            return "png";
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg、jpeg、png、webp 图片");
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
