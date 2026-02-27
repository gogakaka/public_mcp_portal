package com.umg.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JsonPath 평가 유틸리티.
 *
 * <p>JSON 데이터에 대해 JSONPath 표현식을 안전하게 평가합니다.
 * 경로가 일치하지 않는 경우 예외를 발생시키는 대신 null을 반환하여
 * 호출 측에서 안전하게 처리할 수 있도록 합니다.</p>
 *
 * <p>{@link com.umg.adapter.ResponseShaper}에서 도구 실행 결과를
 * 가공할 때 주로 사용됩니다.</p>
 */
@Component
public class JsonPathUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonPathUtil.class);

    /** 경로가 없을 때 null을 반환하도록 설정된 JSONPath 설정 */
    private static final Configuration CONFIG = Configuration.builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    /**
     * JSON 문자열에 대해 JSONPath 표현식을 평가합니다.
     *
     * <p>경로가 존재하지 않으면 null을 반환하고,
     * 평가 중 오류가 발생하면 로그를 남기고 null을 반환합니다.</p>
     *
     * @param json               JSON 문서 문자열
     * @param jsonPathExpression JSONPath 표현식 (예: "$.data[*].name")
     * @return 평가 결과 객체, 또는 경로가 없거나 오류 시 null
     */
    public Object evaluate(String json, String jsonPathExpression) {
        try {
            return JsonPath.using(CONFIG).parse(json).read(jsonPathExpression);
        } catch (PathNotFoundException e) {
            log.debug("JSONPath '{}' 경로를 찾을 수 없습니다", jsonPathExpression);
            return null;
        } catch (Exception e) {
            log.warn("JSONPath '{}' 평가 실패: {}", jsonPathExpression, e.getMessage());
            return null;
        }
    }

    /**
     * JSON 문자열에 대해 JSONPath 표현식을 평가하고 결과를 문자열로 반환합니다.
     *
     * @param json               JSON 문서 문자열
     * @param jsonPathExpression JSONPath 표현식
     * @return 평가 결과의 문자열 표현, 또는 null
     */
    public String evaluateAsString(String json, String jsonPathExpression) {
        Object result = evaluate(json, jsonPathExpression);
        return result != null ? result.toString() : null;
    }
}
