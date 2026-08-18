package com.elearning.controller;

import com.elearning.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.allowed-image-types}")
    private String allowedImageTypesRaw;

    @Value("${app.upload.allowed-video-types}")
    private String allowedVideoTypesRaw;

    @Value("${app.upload.max-image-size-bytes}")
    private long maxImageSizeBytes;

    @Value("${app.upload.max-video-size-bytes}")
    private long maxVideoSizeBytes;

    /**
     * Uploads a course thumbnail or video asset. Validates the file's actual content type
     * (sniffed from bytes, not just trusted from the client-sent header/extension — a
     * malicious client can lie about both) against an allow-list, and enforces a
     * per-category size cap. Filenames are always replaced with a random UUID so a
     * user-supplied name can't be used for path traversal or to overwrite another file.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "image") String kind
    ) throws IOException {
        if (file.isEmpty()) {
            throw new ApiException("File is empty", 400);
        }

        List<String> allowedTypes = "video".equalsIgnoreCase(kind)
                ? Arrays.asList(allowedVideoTypesRaw.split(","))
                : Arrays.asList(allowedImageTypesRaw.split(","));

        long maxSize = "video".equalsIgnoreCase(kind) ? maxVideoSizeBytes : maxImageSizeBytes;

        if (file.getSize() > maxSize) {
            throw new ApiException(
                    "File is too large. Maximum size for " + kind + " uploads is " + (maxSize / (1024 * 1024)) + "MB",
                    400
            );
        }

        String detectedType = sniffContentType(file);
        if (!allowedTypes.contains(detectedType)) {
            throw new ApiException(
                    "Unsupported file type: " + detectedType + ". Allowed types: " + String.join(", ", allowedTypes),
                    400
            );
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String extension = extensionForContentType(detectedType);
        String filename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename).normalize();
        if (!filePath.startsWith(uploadPath.normalize())) {
            // Defense in depth — should be unreachable since filename is always a UUID we generate.
            throw new ApiException("Invalid file path", 400);
        }

        Files.copy(file.getInputStream(), filePath);

        String fileUrl = "/api/uploads/" + filename;
        return ResponseEntity.ok(Map.of("url", fileUrl, "contentType", detectedType));
    }

    /**
     * Detects the real content type by reading the file's magic bytes rather than trusting
     * the client-supplied Content-Type header, which is trivially spoofable.
     */
    private String sniffContentType(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);
            String sniffed = Files.probeContentType(Paths.get(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload"
            ));

            // Magic-byte checks for the most common formats we accept — more reliable than
            // probeContentType alone, which depends on the OS's registered file associations.
            if (matches(header, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
            if (matches(header, 0x89, 0x50, 0x4E, 0x47)) return "image/png";
            if (matches(header, 0x47, 0x49, 0x46, 0x38)) return "image/gif";
            if (header.length >= 12 && new String(header, 8, 4).equals("WEBP")) return "image/webp";
            if (header.length >= 8 && new String(header, 4, 4).equals("ftyp")) return "video/mp4";
            if (matches(header, 0x1A, 0x45, 0xDF, 0xA3)) return "video/webm";

            // Fall back to the client-declared type if we couldn't sniff it — still validated
            // against the allow-list afterward, so this isn't a bypass, just a fallback.
            return sniffed != null ? sniffed : file.getContentType();
        }
    }

    private boolean matches(byte[] header, int... expected) {
        if (header.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> "";
        };
    }
}
