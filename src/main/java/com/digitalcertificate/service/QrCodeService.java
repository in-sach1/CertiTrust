package com.digitalcertificate.service;

import com.digitalcertificate.config.AppProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Creates the QR code that points to /verify?certificateId=... */
@Service
public class QrCodeService {
    private final StorageService storage;
    private final AppProperties props;

    public QrCodeService(StorageService storage, AppProperties props) {
        this.storage = storage;
        this.props = props;
    }

    public String verificationUrl(String certificateId) {
        return props.baseUrl().replaceAll("/+$", "") + "/verify?certificateId=" + certificateId;
    }

    /** Writes the PNG and returns its file name. */
    public String generate(String certificateId) {
        String fileName = certificateId + ".png";
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(verificationUrl(certificateId), BarcodeFormat.QR_CODE, 360, 360, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", storage.qr(fileName), new MatrixToImageConfig(0xFF0B1E3F, 0xFFFFFFFF));
            return fileName;
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not generate the QR code", e);
        }
    }

    /** Returns the PNG bytes, re-creating the file if it was deleted from disk. */
    public byte[] readOrCreate(String certificateId) {
        Path file = storage.qr(certificateId + ".png");
        try {
            if (!Files.exists(file)) {
                generate(certificateId);
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the QR code", e);
        }
    }
}
