package com.bor.eboard.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

/** Reusable wrapper around the Government SMS API used by PondProject. */
@Slf4j
@Service
public class SmsService {
    private final RestTemplate restTemplate;
    private final String url;
    private final String loginText;
    private final String passwordResetText;

    public SmsService(RestTemplateBuilder builder,
                      @Value("${government-sms.url}") String url,
                      @Value("${government-sms.connect-timeout-seconds:5}") long connectTimeout,
                      @Value("${government-sms.read-timeout-seconds:10}") long readTimeout,
                      @Value("${government-sms.text.login:e-Board Login OTP}") String loginText,
                      @Value("${government-sms.text.password-reset:e-Board Password Reset OTP}") String passwordResetText) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(connectTimeout))
                .setReadTimeout(Duration.ofSeconds(readTimeout)).build();
        this.url = url;
        this.loginText = loginText;
        this.passwordResetText = passwordResetText;
    }

    public String requestOtp(String mobile, String purpose) {
        if (mobile == null || !mobile.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("A valid registered mobile number is required");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        String text = "PASSWORD_RESET".equals(purpose) ? passwordResetText : loginText;
        String body = "{\"mobno\":\"" + mobile + "\",\"text\":\"" + escape(text) + "\"}";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalStateException("Government SMS gateway returned an invalid response");
            }
            return unquote(response.getBody().trim());
        } catch (RestClientException ex) {
            log.error("Government SMS gateway request failed for mobile ending {}",
                    mobile.substring(mobile.length() - 4));
            throw new IllegalStateException("OTP could not be sent. Please try again shortly.", ex);
        }
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String unquote(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }
}
