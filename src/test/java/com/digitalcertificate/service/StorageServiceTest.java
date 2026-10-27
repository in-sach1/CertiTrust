package com.digitalcertificate.service;

import com.digitalcertificate.config.AppProperties;
import com.digitalcertificate.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAValidPngUsingGeneratedFileName() throws IOException {
        StorageService storage = storageService();
        MockMultipartFile upload = new MockMultipartFile(
                "logo", "logo.png", "image/png", png(20, 20));

        String storedName = storage.saveLogo(7L, upload, null);

        assertTrue(storedName.matches("logo-7-[0-9a-f-]+\\.png"));
        assertTrue(Files.isRegularFile(storage.upload(storedName)));
    }

    @Test
    void rejectsImagesThatExceedTheDimensionLimit() throws IOException {
        StorageService storage = storageService();
        MockMultipartFile upload = new MockMultipartFile(
                "logo", "large.png", "image/png", png(4001, 1));

        assertThrows(BusinessException.class, () -> storage.saveLogo(7L, upload, null));
    }

    @Test
    void rejectsNonImageContent() throws IOException {
        StorageService storage = storageService();
        MockMultipartFile upload = new MockMultipartFile(
                "logo", "logo.png", "image/png", "not an image".getBytes());

        assertThrows(BusinessException.class, () -> storage.saveLogo(7L, upload, null));
    }

    private StorageService storageService() throws IOException {
        AppProperties.Storage directories = new AppProperties.Storage(
                tempDir.resolve("uploads").toString(),
                tempDir.resolve("certificates").toString(),
                tempDir.resolve("qr").toString());
        return new StorageService(new AppProperties("http://localhost:8080", false, false, directories));
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
