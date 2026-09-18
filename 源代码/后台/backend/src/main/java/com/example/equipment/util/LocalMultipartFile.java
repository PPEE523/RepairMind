package com.example.equipment.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 将本地 java.io.File 包装为 MultipartFile，便于复用基于 MultipartFile 的上传逻辑
 * （例如把本地存储的案例图片上传到 Dify 知识库）。
 */
public class LocalMultipartFile implements MultipartFile {

    private final File file;
    private final String contentType;

    public LocalMultipartFile(File file) {
        this.file = file;
        this.contentType = probeContentType(file);
    }

    private static String probeContentType(File file) {
        try {
            String ct = Files.probeContentType(file.toPath());
            return ct != null ? ct : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return file.getName();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return !file.exists() || file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(file.toPath());
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
