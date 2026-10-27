package com.digitalcertificate.service;

import com.digitalcertificate.config.AppProperties;
import com.digitalcertificate.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

/** All file access goes through here: fixed folders, generated file names, no path traversal. */
@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final long MAX_LOGO_BYTES = 1_000_000;
    private static final int MAX_LOGO_DIMENSION = 4000;

    private final Path uploadDir;
    private final Path certificateDir;
    private final Path qrDir;

    public StorageService(AppProperties props) throws IOException {
        this.uploadDir = prepare(props.storage().uploadDir());
        this.certificateDir = prepare(props.storage().certificateDir());
        this.qrDir = prepare(props.storage().qrDir());
    }

    private static Path prepare(String dir) throws IOException {
        Path p = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(p);
        return p;
    }

    public Path upload(String fileName) { return resolve(uploadDir, fileName); }
    public Path certificate(String fileName) { return resolve(certificateDir, fileName); }
    public Path qr(String fileName) { return resolve(qrDir, fileName); }

    private static Path resolve(Path dir, String fileName) {
        Path p = dir.resolve(fileName).normalize();
        if (!p.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return p;
    }

    /**
     * Validates and stores an institution logo (PNG or JPEG, max 1 MB).
     * The type is checked from the file's real content, not from its name; the stored name is generated.
     * Returns the new file name, or oldName when nothing was uploaded.
     */
    public String saveLogo(Long institutionId, MultipartFile file, String oldName) {
        if (file == null || file.isEmpty()) {
            return oldName;
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("The logo must be smaller than 1 MB.");
        }
        try {
            byte[] bytes = file.getBytes();
            String ext = detectExtension(bytes);
            if (ext == null) {
                throw new BusinessException("The logo must be a PNG or JPEG image.");
            }
            if (!isValidImage(bytes, ext)) {
                throw new BusinessException("The logo is not a valid image (or is larger than 4000 px).");
            }
            String name = "logo-" + institutionId + "-" + UUID.randomUUID() + "." + ext;
            Files.write(upload(name), bytes);
            if (oldName != null) {
                Files.deleteIfExists(upload(oldName));
            }
            return name;
        } catch (IOException e) {
            log.error("Failed to process logo for institution {}", institutionId, e);
            throw new BusinessException("The logo could not be processed.");
        }
    }

    private static boolean isValidImage(byte[] bytes, String expectedExtension) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (("png".equals(expectedExtension) && !"png".equalsIgnoreCase(format))
                        || ("jpg".equals(expectedExtension) && !"jpeg".equalsIgnoreCase(format))) {
                    return false;
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_LOGO_DIMENSION || height > MAX_LOGO_DIMENSION) {
                    return false;
                }
                BufferedImage image = reader.read(0);
                return image != null;
            } finally {
                reader.dispose();
            }
        }
    }

    private static String detectExtension(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        return null;
    }
}
