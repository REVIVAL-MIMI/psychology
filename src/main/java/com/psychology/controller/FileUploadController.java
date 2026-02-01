package com.psychology.controller;

import com.psychology.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@AuthenticationPrincipal Object user,
                                        @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading file: {} by user {}", file.getOriginalFilename(), user);

            String fileUrl = fileStorageService.storeFile(file);

            return ResponseEntity.ok(new FileUploadResponse(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            ));

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to upload file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            byte[] fileContent = fileStorageService.loadFile(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @lombok.Data
    public static class FileUploadResponse {
        private String fileUrl;
        private String originalName;
        private String contentType;
        private long size;
        private LocalDateTime uploadedAt = LocalDateTime.now();

        public FileUploadResponse(String fileUrl, String originalName, String contentType, long size) {
            this.fileUrl = fileUrl;
            this.originalName = originalName;
            this.contentType = contentType;
            this.size = size;
        }
    }

    @lombok.Data
    public static class ApiResponse {
        private String message;
        private LocalDateTime timestamp;

        public ApiResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
}