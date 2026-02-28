package com.umg.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * MCP JSON-RPC 메시지 모델 클래스.
 *
 * <p>MCP 클라이언트와 서버 간에 교환되는 다양한 구조화된 타입을 정의합니다.
 * 각 내부 클래스는 MCP 프로토콜의 특정 메시지 유형에 대응합니다.</p>
 */
public final class McpMessage {

    private McpMessage() {
    }

    /**
     * 서버 기능 선언.
     * initialize 응답 시 반환됩니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServerCapabilities {

        /** 도구 관련 기능 선언 */
        @JsonProperty("tools")
        private ToolCapability tools;

        /**
         * 도구 기능 세부 선언.
         */
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ToolCapability {
            /** 도구 목록 변경 알림 지원 여부 */
            @JsonProperty("listChanged")
            private boolean listChanged;
        }
    }

    /**
     * 서버 정보.
     * initialize 응답 시 반환됩니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServerInfo {
        /** 서버 이름 */
        @JsonProperty("name")
        private String name;

        /** 서버 버전 */
        @JsonProperty("version")
        private String version;
    }

    /**
     * MCP 도구 정의.
     * tools/list 응답에 포함되는 개별 도구 설명입니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolDescription {
        /** 도구 이름 (고유 식별자) */
        @JsonProperty("name")
        private String name;

        /** 도구 설명 */
        @JsonProperty("description")
        private String description;

        /** 입력 파라미터 JSON Schema */
        @JsonProperty("inputSchema")
        private Map<String, Object> inputSchema;
    }

    /**
     * 콘텐츠 블록.
     * tools/call 응답에 포함되는 개별 콘텐츠 요소입니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentBlock {
        /** 콘텐츠 타입 (text, image 등) */
        @JsonProperty("type")
        private String type;

        /** 텍스트 콘텐츠 */
        @JsonProperty("text")
        private String text;

        /** MIME 타입 (바이너리 데이터인 경우) */
        @JsonProperty("mimeType")
        private String mimeType;

        /** Base64 인코딩된 바이너리 데이터 */
        @JsonProperty("data")
        private String data;
    }

    /**
     * 도구 실행 결과.
     * tools/call 응답의 result 필드에 포함됩니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallResult {
        /** 응답 콘텐츠 블록 목록 */
        @JsonProperty("content")
        private List<ContentBlock> content;

        /** 오류 발생 여부 */
        @JsonProperty("isError")
        @Builder.Default
        private boolean isError = false;
    }

    /**
     * 도구 목록 결과.
     * tools/list 응답의 result 필드에 포함됩니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResult {
        /** 사용 가능한 도구 설명 목록 */
        @JsonProperty("tools")
        private List<ToolDescription> tools;
    }
}
