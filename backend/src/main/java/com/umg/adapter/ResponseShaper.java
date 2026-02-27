package com.umg.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies JSONPath transformation rules to filter and shape raw response
 * data from tool backends.
 *
 * <p>When a tool has a {@code responseMappingRule} configured, this component
 * evaluates the JSONPath expression against the raw response and returns the
 * matching subset. If evaluation fails, the original response is returned
 * unchanged.</p>
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
     * Applies a JSONPath expression to the raw response data.
     *
     * @param rawResponse the raw response from the tool backend
     * @param jsonPathExpression the JSONPath expression to evaluate
     * @return the shaped response, or the original if shaping fails
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
                log.warn("JSONPath '{}' returned null; returning original response", jsonPathExpression);
                return rawResponse;
            }

            // Convert the result back to a JSON string for consistent return type
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.warn("Failed to apply JSONPath '{}': {}. Returning original response.",
                    jsonPathExpression, e.getMessage());
            return rawResponse;
        }
    }
}
