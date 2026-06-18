package com.closetnangam.be.domain.legal.service;

import com.closetnangam.be.domain.legal.dto.response.LegalDocumentResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LegalDocumentService {

    private static final Map<String, LegalDocumentFile> DOCUMENTS = Map.of(
            "terms", new LegalDocumentFile("terms.md", "terms"),
            "privacy-policy", new LegalDocumentFile("privacy-policy.md", "privacy_policy"),
            "marketing-consent", new LegalDocumentFile("marketing-consent.md", "marketing_consent")
    );

    public LegalDocumentResponse getDocument(String documentType) {
        LegalDocumentFile documentFile = DOCUMENTS.get(documentType);
        if (documentFile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 약관 문서입니다.");
        }

        ParsedDocument parsedDocument = parseFrontmatter(readDocument(documentFile.fileName()));
        Map<String, String> metadata = parsedDocument.metadata();

        return new LegalDocumentResponse(
                metadata.getOrDefault("policy_type", documentFile.defaultPolicyType()),
                metadata.getOrDefault("version", ""),
                metadata.getOrDefault("effective_date", ""),
                metadata.getOrDefault("last_updated", ""),
                parsedDocument.content()
        );
    }

    private String readDocument(String fileName) {
        Path localPath = Path.of("docs", "legal", fileName);
        if (Files.exists(localPath)) {
            try {
                return Files.readString(localPath, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "약관 문서를 불러오지 못했습니다.",
                        exception
                );
            }
        }

        ClassPathResource resource = new ClassPathResource("legal/" + fileName);
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "약관 문서를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private ParsedDocument parseFrontmatter(String markdown) {
        String normalized = markdown.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return new ParsedDocument(Map.of(), normalized.trim());
        }

        int endIndex = normalized.indexOf("\n---", 4);
        if (endIndex < 0) {
            return new ParsedDocument(Map.of(), normalized.trim());
        }

        String rawMetadata = normalized.substring(4, endIndex).trim();
        String content = normalized.substring(endIndex + 4).trim();
        Map<String, String> metadata = new HashMap<>();

        for (String line : rawMetadata.split("\n")) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex < 0) {
                continue;
            }

            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            if (!key.isBlank()) {
                metadata.put(key, value);
            }
        }

        return new ParsedDocument(metadata, content);
    }

    private record LegalDocumentFile(String fileName, String defaultPolicyType) {
    }

    private record ParsedDocument(Map<String, String> metadata, String content) {
    }
}
