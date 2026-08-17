package com.bor.eboard.documentviewer.service;

import com.bor.eboard.common.exception.ForbiddenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentShareTokenService {

    private static final String SUBJECT = "SECURE_DOCUMENT_SHARE";
    private final SecretKey secretKey;

    public DocumentShareTokenService(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(ShareGrant grant) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("source", grant.source());
        claims.put("documentId", grant.documentId().toString());
        claims.put("fileName", grant.fileName());
        claims.put("mimeType", grant.mimeType());
        claims.put("fileSize", grant.fileSize());
        claims.put("issuedBy", grant.issuedBy());
        if (grant.version() != null) {
            claims.put("version", grant.version());
        }
        return Jwts.builder()
                .subject(SUBJECT)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(Date.from(grant.expiresAt()))
                .signWith(secretKey)
                .compact();
    }

    public ShareGrant parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!SUBJECT.equals(claims.getSubject())) {
                throw new ForbiddenException("Invalid secure document link");
            }
            Object rawVersion = claims.get("version");
            Object rawFileSize = claims.get("fileSize");
            Number version = rawVersion instanceof Number number ? number : null;
            Number fileSize = rawFileSize instanceof Number number ? number : null;
            return new ShareGrant(
                    claims.get("source", String.class),
                    UUID.fromString(claims.get("documentId", String.class)),
                    version == null ? null : version.intValue(),
                    claims.get("fileName", String.class),
                    claims.get("mimeType", String.class),
                    fileSize == null ? 0L : fileSize.longValue(),
                    claims.get("issuedBy", String.class),
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ForbiddenException("Secure document link is invalid or expired");
        }
    }

    public record ShareGrant(
            String source,
            UUID documentId,
            Integer version,
            String fileName,
            String mimeType,
            long fileSize,
            String issuedBy,
            Instant expiresAt) {
    }
}
