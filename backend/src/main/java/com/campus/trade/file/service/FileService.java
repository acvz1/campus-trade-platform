package com.campus.trade.file.service;

import com.campus.trade.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadResponse storeImage(MultipartFile file, Long userId, String type);
}
