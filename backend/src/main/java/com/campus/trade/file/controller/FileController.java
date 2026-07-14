package com.campus.trade.file.controller;

import com.campus.trade.common.result.Result;
import com.campus.trade.file.dto.FileUploadResponse;
import com.campus.trade.file.service.FileService;
import com.campus.trade.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传商品图或头像")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadResponse> upload(@RequestParam MultipartFile file,
                                             @RequestParam(defaultValue = "product") String type,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(fileService.storeImage(file, user.userId(), type));
    }
}
