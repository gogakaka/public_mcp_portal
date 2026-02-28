package com.umg.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 응답 데이터 가공기.
 *
 * <p>JSONPath 규칙을 적용하여 도구 실행 결과(원본 응답)를 필터링하고 가공합니다.
 * 도구에 {@code responseMappingRule}이 설정되어 있으면 해당 규칙을 적용하고,
 * 설정되어 있지 않으면 원본 응답을 그대로 반환합니다.</p>
 *
 * <p>규칙 적용 중 오류가 발생하면 원본 응답을 반환하고 경고 로그를 기록합니다.</p>
 */
@Component
public class ResponseShaper {

    private static final Logger log = LoggerFactory.getLogger(ResponseShaper.class);

    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;

    public ResponseShaper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jsonPathConfig = Configuration.builder()
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    /**
     * 원본 응답에 JSONPath 표현식을 적용하여 가공된 결과를 반환합니다.
     *
     * <p>responseMappingRule이 null이거나 비어있으면 원본 응답을 그대로 반환합니다.
     * JSONPath 평가 결과가 null이면 경고 로그와 함께 원본 응답을 반환합니다.</p>
     *
     * @param rawResponse        도구 실행의 원본 응답 데이터
     * @param jsonPathExpression JSONPath 표현식 (예: "$.data[*].name")
     * @return 가공된 응답 또는 원본 응답
     */
    public Object shape(Object rawResponse, String jsonPathExpression) {
        if (rawResponse == null || jsonPathExpression == null || jsonPathExpression.isBlank()) {
            return rawResponse;
        }

        try {
            String jsonString;
            if (rawResponse instanceof String) {
                jsonString = (String) rawResponse;
            } else {
                jsonString = objectMapper.writeValueAsString(rawResponse);
            }

            Object result = JsonPath.using(jsonPathConfig).parse(jsonString).read(jsonPathExpression);

            if (result == null) {
                log.warn("JSONPath '{}' 평가 결과가 null입니다. 원본 응답을 반환합니다.", jsonPathExpression);
                return rawResponse;
            }

            /* 결과를 JSON 문자열로 변환하여 일관된 반환 타입 보장 */
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.warn("JSONPath '{}' 적용 실패: {}. 원본 응답을 반환합니다.",
                    jsonPathExpression, e.getMessage());
            return rawResponse;
        }
    }
}
