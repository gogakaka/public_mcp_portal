package com.umg.adapter;

import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 도구 실행 인터페이스.
 *
 * <p>각 도구 타입(N8N, Cube.js, AWS Remote)에 대한 실행 로직을 추상화합니다.
 * 구현체는 제네릭 도구 호출 요청을 대상 백엔드가 요구하는
 * 고유 프로토콜 및 형식으로 변환하는 책임을 갖습니다.</p>
 */
public interface ToolExecutor {

    /**
     * 지정된 파라미터로 도구를 실행합니다.
     *
     * @param tool        연결 설정 정보를 포함한 도구 엔티티
     * @param params      실행에 필요한 입력 파라미터
     * @param userContext 사용자 컨텍스트 정보 (부서 등, RLS용)
     * @return 실행 결과를 담은 CompletableFuture
     */
    CompletableFuture<Object> execute(Tool tool, Map<String, Object> params, String userContext);

    /**
     * 이 어댑터가 지정된 도구 타입을 지원하는지 확인합니다.
     *
     * @param type 도구 타입
     * @return 지원 여부
     */
    boolean supports(ToolType type);
}
