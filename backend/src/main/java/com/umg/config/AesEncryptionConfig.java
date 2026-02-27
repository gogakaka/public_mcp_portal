package com.umg.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Configuration for AES-256-GCM encryption used to protect sensitive
 * data at rest (e.g. tool connection configurations).
 *
 * <p>Uses Bouncy Castle as the JCE provider and AES/GCM/NoPadding for
 * authenticated encryption. A random 12-byte IV is prepended to each
 * ciphertext so that the same plaintext produces different ciphertext
 * on every encryption.</p>
 */
@Configuration
public class AesEncryptionConfig {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${app.security.aes-key}")
    private String aesKeyString;

    /**
     * Provides the AES {@link SecretKey} bean derived from the configured key string.
     *
     * @return a 256-bit AES secret key
     */
    @Bean
    public SecretKey aesSecretKey() {
        byte[] keyBytes = aesKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        } else if (keyBytes.length > 32) {
            byte[] trimmed = new byte[32];
            System.arraycopy(keyBytes, 0, trimmed, 0, 32);
            keyBytes = trimmed;
        }
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * Provides the AES encryptor bean.
     *
     * @param secretKey the AES secret key
     * @return an AesEncryptor instance
     */
    @Bean
    public AesEncryptor aesEncryptor(SecretKey secretKey) {
        return new AesEncryptor(secretKey);
    }

    /**
     * Stateless AES-256-GCM encryptor / decryptor.
     */
    public static class AesEncryptor {

        private final SecretKey secretKey;
        private final SecureRandom secureRandom = new SecureRandom();

        public AesEncryptor(SecretKey secretKey) {
            this.secretKey = secretKey;
        }

        /**
         * Encrypts a plaintext string using AES-256-GCM.
         *
         * @param plaintext the string to encrypt
         * @return Base64-encoded ciphertext (IV prepended)
         */
        public String encrypt(String plaintext) {
            if (plaintext == null) {
                return null;
            }
            try {
                byte[] iv = new byte[GCM_IV_LENGTH];
                secureRandom.nextBytes(iv);

                Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

                byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

                ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
                byteBuffer.put(iv);
                byteBuffer.put(ciphertext);

                return Base64.getEncoder().encodeToString(byteBuffer.array());
            } catch (Exception e) {
                throw new RuntimeException("Failed to encrypt data", e);
            }
        }

        /**
         * Decrypts a Base64-encoded ciphertext produced by {@link #encrypt(String)}.
         *
         * @param encryptedText the Base64-encoded ciphertext
         * @return the original plaintext
         */
        public String decrypt(String encryptedText) {
            if (encryptedText == null) {
                return null;
            }
            try {
                byte[] decoded = Base64.getDecoder().decode(encryptedText);
                ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

                byte[] iv = new byte[GCM_IV_LENGTH];
                byteBuffer.get(iv);

                byte[] ciphertext = new byte[byteBuffer.remaining()];
                byteBuffer.get(ciphertext);

                Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

                byte[] plaintext = cipher.doFinal(ciphertext);
                return new String(plaintext, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt data", e);
            }
        }
    }
}
