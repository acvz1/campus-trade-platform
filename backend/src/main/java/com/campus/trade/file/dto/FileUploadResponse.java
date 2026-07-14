package com.campus.trade.file.dto;

public record FileUploadResponse(String url, String type, String originalName, long size) {
}
