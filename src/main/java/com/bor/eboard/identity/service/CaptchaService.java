package com.bor.eboard.identity.service;

import com.bor.eboard.identity.dto.CaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Adapted from the production PondProject CaptchaService. */
@Service
public class CaptchaService {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private final ConcurrentHashMap<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long expirySeconds;
    private final int length;

    public CaptchaService(@Value("${security.captcha.expiry-seconds:40}") long expirySeconds,
                          @Value("${security.captcha.length:5}") int length) {
        this.expirySeconds = expirySeconds;
        this.length = length;
    }

    public CaptchaResponse generateCaptcha() {
        removeExpired();
        String code = randomCode();
        String id = UUID.randomUUID().toString();
        captchaStore.put(id, new CaptchaEntry(code, Instant.now().plusSeconds(expirySeconds)));
        return new CaptchaResponse(id, render(code), expirySeconds);
    }

    /** Always consumes the identifier, whether validation succeeds or fails. */
    public boolean validateCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) return false;
        CaptchaEntry entry = captchaStore.remove(captchaId);
        return entry != null && !Instant.now().isAfter(entry.expiry())
                && entry.code().equalsIgnoreCase(userInput.trim());
    }

    private String randomCode() {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        return value.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(180, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 180, 50);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(randomColor());
                graphics.drawString(String.valueOf(code.charAt(i)), 30 * i + 15, 35);
            }
            for (int i = 0; i < 3; i++) {
                graphics.setColor(randomColor());
                graphics.drawLine(random.nextInt(180), random.nextInt(50), random.nextInt(180), random.nextInt(50));
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate CAPTCHA image", e);
        } finally {
            graphics.dispose();
        }
    }

    private Color randomColor() { return new Color(random.nextInt(150), random.nextInt(135), random.nextInt(135)); }
    private void removeExpired() { captchaStore.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().expiry())); }
    private record CaptchaEntry(String code, Instant expiry) { }
}
