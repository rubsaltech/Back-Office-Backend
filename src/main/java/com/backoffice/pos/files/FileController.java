package com.backoffice.pos.files;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Generic image upload used by product/category/logo/avatar pickers. */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    public record FileUploadResponse(String url, String filename) {
    }

    @PostMapping
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file) {
        String name = storage.store(file);
        return new FileUploadResponse("/uploads/" + name, name);
    }
}
