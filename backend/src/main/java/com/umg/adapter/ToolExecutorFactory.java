package com.umg.adapter;

import com.umg.domain.enums.ToolType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 도구 실행기 팩토리.
 *
 * <p>{@link ToolType}에 따라 적절한 {@link ToolExecutor} 구현체를 반환합니다.
 * Spring이 주입하는 모든 ToolExecutor 빈 목록에서 요청된 도구 타입을
 * 지원하는 실행기를 동적으로 검색합니다.</p>
 */
@Component
public class ToolExecutorFactory {

    private final List<ToolExecutor> executors;

    /**
     * 모든 ToolExecutor 구현체를 주입받아 팩토리를 초기화합니다.
     *
     * @param executors Spring 컨텍스트에 등록된 모든 ToolExecutor 빈 목록
     */
    public ToolExecutorFactory(List<ToolExecutor> executors) {
        this.executors = executors;
    }

    /**
     * 지정된 도구 타입에 적합한 실행기를 반환합니다.
     *
     * @param toolType 도구 타입 (N8N, CUBE_JS, AWS_REMOTE)
     * @return 해당 타입을 지원하는 ToolExecutor 인스턴스
     * @throws IllegalArgumentException 지원하지 않는 도구 타입인 경우
     */
    public ToolExecutor getExecutor(ToolType toolType) {
        return executors.stream()
                .filter(executor -> executor.supports(toolType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 도구 타입입니다: " + toolType));
    }
}
