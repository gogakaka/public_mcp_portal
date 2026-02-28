package com.umg.util;

import java.util.UUID;

/**
 * UUID 생성 유틸리티.
 *
 * <p>ID 생성 전략을 일관되게 관리하기 위한 중앙 집중식 유틸리티입니다.
 * 필요에 따라 UUIDv7(시간 순서 보장) 등 다른 전략으로 쉽게 변경할 수 있도록
 * 구조화되어 있습니다.</p>
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    /**
     * 새로운 랜덤 UUID(버전 4)를 생성합니다.
     *
     * @return 새로운 UUID 인스턴스
     */
    public static UUID newId() {
        return UUID.randomUUID();
    }

    /**
     * 하이픈이 없는 UUID 문자열을 생성합니다.
     *
     * @return 32자리 16진수 문자열
     */
    public static String newIdString() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
