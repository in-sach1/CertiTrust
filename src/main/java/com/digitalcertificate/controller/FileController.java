package com.digitalcertificate.controller;

import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.service.InstitutionService;
import com.digitalcertificate.service.StorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Serves uploaded institution logos (the file name is looked up in the database, never taken from the URL). */
@Controller
public class FileController {
    private final InstitutionService institutions;
    private final StorageService storage;

    public FileController(InstitutionService institutions, StorageService storage) {
        this.institutions = institutions;
        this.storage = storage;
    }

    @GetMapping("/logo/{id}")
    public ResponseEntity<byte[]> logo(@PathVariable Long id) throws IOException {
        Institution i = institutions.get(id);
        if (i.getLogoPath() == null) {
            throw new ResourceNotFoundException("No logo");
        }
        Path file = storage.upload(i.getLogoPath());
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("No logo");
        }
        MediaType type = i.getLogoPath().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
                .body(Files.readAllBytes(file));
    }
}
