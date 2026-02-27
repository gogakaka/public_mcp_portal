package com.umg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 해싱 유틸리티.
 *
 * <p>주로 API 키를 데이터베이스에 저장하기 전에 해싱하는 데 사용됩니다.
 * 원본 API 키 대신 SHA-256 해시만 저장하여, 데이터베이스가 노출되더라도
 * 원본 키를 복구할 수 없도록 합니다.</p>
 */
public final class HashUtil {

    private HashUtil() {
    }

    /**
     * 입력 문자열의 SHA-256 해시를 계산합니다.
     *
     * @param input 해시할 문자열
     * @return 소문자 16진수로 인코딩된 SHA-256 다이제스트 (64자)
     * @throws RuntimeException SHA-256 알고리즘을 사용할 수 없는 경우 (정상 환경에서는 발생하지 않음)
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
