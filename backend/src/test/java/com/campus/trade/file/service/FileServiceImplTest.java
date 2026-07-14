package com.campus.trade.file.service;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.file.dto.FileUploadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceImplTest {
    @Test
    void storesValidatedPngUnderUserMonthDirectory() {
        Path tempDir = testDirectory();
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", png);
        FileServiceImpl service = new FileServiceImpl(tempDir.toString());

        FileUploadResponse response = service.storeImage(file, 7L, "product");

        assertThat(response.url()).matches("/uploads/\\d{6}/7/[a-f0-9]{32}\\.png");
        Path storedFile = tempDir.resolve(response.url().substring("/uploads/".length()));
        assertThat(Files.exists(storedFile)).isTrue();
    }

    @Test
    void rejectsSpoofedImageExtension() {
        Path tempDir = testDirectory();
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.png", "image/png", "not-an-image".getBytes());
        FileServiceImpl service = new FileServiceImpl(tempDir.toString());

        assertThatThrownBy(() -> service.storeImage(file, 7L, "product"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }

    private Path testDirectory() {
        return Path.of("target", "test-uploads", UUID.randomUUID().toString()).toAbsolutePath();
    }
}
