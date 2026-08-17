package com.bor.eboard.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** AES-CBC OTP response decryption reused from the PondProject reference. */
@Service
public class CryptoService {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 16;
    private final String otpKey;

    public CryptoService(@Value("${government-sms.otp-encryption-key}") String otpKey) {
        this.otpKey = otpKey;
    }

    public String decryptOtp(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = generateKeySpec(otpKey);
            IvParameterSpec iv = new IvParameterSpec(secretKey.getEncoded());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decoded = Base64.getDecoder().decode(encryptedText.replaceAll("\\s", "+"));
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Government SMS gateway returned an unreadable OTP response", ex);
        }
    }

    public String encryptOtp(String otp) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = generateKeySpec(otpKey);
            IvParameterSpec iv = new IvParameterSpec(secretKey.getEncoded());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create OTP verification payload", ex);
        }
    }

    private SecretKeySpec generateKeySpec(String key) {
        byte[] keyBytes = new byte[KEY_SIZE];
        byte[] source = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, keyBytes, 0, Math.min(source.length, KEY_SIZE));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
