package com.umg.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} that transparently encrypts and decrypts
 * string fields using AES-256-GCM.
 *
 * <p>Apply this converter to entity fields that contain sensitive data (such
 * as connection configurations with credentials) so that the data is encrypted
 * at rest in the database.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;Convert(converter = AesAttributeConverter.class)
 * &#64;Column(name = "connection_config", columnDefinition = "TEXT")
 * private String connectionConfig;
 * </pre>
 */
@Converter
@Component
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private final AesEncryptionConfig.AesEncryptor aesEncryptor;

    public AesAttributeConverter(AesEncryptionConfig.AesEncryptor aesEncryptor) {
        this.aesEncryptor = aesEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return aesEncryptor.decrypt(dbData);
    }
}
