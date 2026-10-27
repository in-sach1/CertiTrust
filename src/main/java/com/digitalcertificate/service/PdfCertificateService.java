package com.digitalcertificate.service;

import com.digitalcertificate.entity.Certificate;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Draws the downloadable A4 landscape PDF certificate. */
@Service
public class PdfCertificateService {
    private static final Color NAVY = new Color(11, 30, 63);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color CYAN = new Color(6, 182, 212);
    private static final Color GRAY = new Color(90, 104, 130);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private final StorageService storage;
    private final QrCodeService qr;

    public PdfCertificateService(StorageService storage, QrCodeService qr) {
        this.storage = storage;
        this.qr = qr;
    }

    /** Writes the PDF and returns its file name. */
    public String generate(Certificate c) {
        String fileName = c.getCertificateId() + ".pdf";
        Path file = storage.certificate(fileName);
        byte[] qrBytes = qr.readOrCreate(c.getCertificateId());
        byte[] logo = readLogo(c);

        Document doc = new Document(PageSize.A4.rotate(), 50, 50, 124, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle("Certificate " + c.getCertificateId());
            doc.addAuthor(c.getInstitutionName());
            doc.addCreator("CertiTrust");
            doc.open();

            Rectangle page = doc.getPageSize();
            float w = page.getWidth();
            float h = page.getHeight();
            drawFrame(writer.getDirectContentUnder(), w, h);
            drawLogo(doc, writer.getDirectContent(), logo, c.getInstitutionName(), w, h);

            doc.add(centered(c.getInstitutionName().toUpperCase(Locale.ROOT), FontFactory.TIMES_BOLD, 13, BLUE, 4));
            doc.add(centered("CERTIFICATE OF " + c.getCertificateType().getLabel().toUpperCase(Locale.ROOT),
                    FontFactory.TIMES_BOLD, 26, NAVY, 10));
            doc.add(centered("This certificate is proudly presented to", FontFactory.TIMES_ITALIC, 14, GRAY, 6));

            Chunk name = new Chunk(c.getRecipientName(), FontFactory.getFont(FontFactory.TIMES_BOLD, 34, Font.BOLD, NAVY));
            name.setUnderline(CYAN, 1.2f, 0f, -6f, 0f, PdfContentByte.LINE_CAP_BUTT);
            Paragraph recipient = new Paragraph(name);
            recipient.setAlignment(Element.ALIGN_CENTER);
            recipient.setSpacingAfter(12);
            doc.add(recipient);

            doc.add(centered(lead(c), FontFactory.TIMES_ITALIC, 14, GRAY, 4));
            doc.add(centered(c.getCourse(), FontFactory.TIMES_BOLD, 21, BLUE, 8));

            StringBuilder meta = new StringBuilder("Issued: " + c.getIssueDate().format(DATE));
            if (c.getExpiryDate() != null) {
                meta.append("   |   Valid until: ").append(c.getExpiryDate().format(DATE));
            }
            if (c.getGrade() != null) {
                meta.append("   |   Grade / Score: ").append(c.getGrade());
            }
            doc.add(centered(meta.toString(), FontFactory.HELVETICA, 10.5f, GRAY, 4));
            if (c.getAdditionalInfo() != null) {
                String info = c.getAdditionalInfo().replaceAll("\\s+", " ");
                doc.add(centered(info.length() > 200 ? info.substring(0, 197) + "..." : info,
                        FontFactory.TIMES_ITALIC, 10, GRAY, 4));
            }

            doc.add(bottomRow(c, qrBytes));
            doc.add(centered("Digitally secured by CertiTrust  -  Verify Every Credential. Trust Every Certificate.",
                    FontFactory.HELVETICA, 7.5f, GRAY, 0));
            doc.close();
            return fileName;
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Could not generate the PDF certificate", e);
        }
    }

    private static String lead(Certificate c) {
        switch (c.getCertificateType()) {
            case COURSE_COMPLETION: return "for successfully completing";
            case DEGREE: return "who has been awarded the degree of";
            case DIPLOMA: return "who has been awarded the diploma in";
            case PARTICIPATION: return "for active participation in";
            default: return "in recognition of outstanding achievement in";
        }
    }

    private static Paragraph centered(String text, String fontName, float size, Color color, float after) {
        Paragraph p = new Paragraph(text, FontFactory.getFont(fontName, size, Font.NORMAL, color));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(after);
        return p;
    }

    private static void drawFrame(PdfContentByte under, float w, float h) {
        under.setColorFill(new Color(247, 250, 255));
        under.rectangle(0, 0, w, h);
        under.fill();
        under.setColorStroke(NAVY);
        under.setLineWidth(5);
        under.rectangle(18, 18, w - 36, h - 36);
        under.stroke();
        under.setColorStroke(CYAN);
        under.setLineWidth(1.2f);
        under.rectangle(28, 28, w - 56, h - 56);
        under.stroke();
    }

    private static void drawLogo(Document doc, PdfContentByte over, byte[] logo, String institution, float w, float h)
            throws DocumentException {
        if (logo != null) {
            try {
                Image img = Image.getInstance(logo);
                img.scaleToFit(70, 70);
                img.setAbsolutePosition(w / 2 - img.getScaledWidth() / 2, h - 108);
                doc.add(img);
                return;
            } catch (IOException | DocumentException ignored) {
                // fall back to the initials badge below
            }
        }
        over.setColorFill(NAVY);
        over.circle(w / 2, h - 76, 32);
        over.fill();
        over.setColorStroke(CYAN);
        over.setLineWidth(2);
        over.circle(w / 2, h - 76, 36);
        over.stroke();
        ColumnText.showTextAligned(over, Element.ALIGN_CENTER,
                new Phrase(initials(institution), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, Color.WHITE)),
                w / 2, h - 84, 0);
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0)) && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.length() == 0 ? "CT" : sb.toString();
    }

    private PdfPTable bottomRow(Certificate c, byte[] qrBytes) throws DocumentException, IOException {
        PdfPTable table = new PdfPTable(new float[] {3.2f, 4.6f, 2.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(16);
        table.setSpacingAfter(8);

        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.NORMAL, GRAY);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, NAVY);
        Font mono = FontFactory.getFont(FontFactory.COURIER, 7, Font.NORMAL, GRAY);

        PdfPCell sign = noBorder();
        sign.setVerticalAlignment(Element.ALIGN_BOTTOM);
        sign.addElement(new Paragraph("____________________________", small));
        sign.addElement(new Paragraph(c.getIssuer().getName(), bold));
        sign.addElement(new Paragraph("Authorized Signatory, " + c.getInstitutionName(), small));
        table.addCell(sign);

        PdfPCell info = noBorder();
        info.setVerticalAlignment(Element.ALIGN_BOTTOM);
        info.addElement(new Paragraph("Certificate ID: " + c.getCertificateId(), bold));
        info.addElement(new Paragraph("Verify at: " + qr.verificationUrl(c.getCertificateId()), small));
        info.addElement(new Paragraph("SHA-256: " + c.getCertificateHash().substring(0, 32), mono));
        info.addElement(new Paragraph("        " + c.getCertificateHash().substring(32), mono));
        table.addCell(info);

        PdfPCell code = noBorder();
        code.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image qrImage = Image.getInstance(qrBytes);
        qrImage.scaleToFit(92, 92);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        code.addElement(qrImage);
        Paragraph caption = new Paragraph("Scan to verify", small);
        caption.setAlignment(Element.ALIGN_CENTER);
        code.addElement(caption);
        table.addCell(code);
        return table;
    }

    private static PdfPCell noBorder() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private byte[] readLogo(Certificate c) {
        String logoPath = c.getInstitution() == null ? null : c.getInstitution().getLogoPath();
        if (logoPath == null) {
            return null;
        }
        try {
            Path p = storage.upload(logoPath);
            return Files.exists(p) ? Files.readAllBytes(p) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
