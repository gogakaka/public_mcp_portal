package com.umg.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utility class for evaluating JSONPath expressions against JSON data.
 *
 * <p>Provides a safe evaluation method that returns {@link Optional#empty()}
 * when the path does not match, rather than throwing an exception.</p>
 */
public final class JsonPathUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonPathUtil.class);

    private static final Configuration CONFIG = Configuration.builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    private JsonPathUtil() {
    }

    /**
     * Evaluates a JSONPath expression against a JSON string.
     *
     * @param json           the JSON document as a string
     * @param jsonPathExpression the JSONPath expression to evaluate
     * @param <T>            the expected result type
     * @return an Optional containing the result, or empty if not found
     */
    public static <T> Optional<T> evaluate(String json, String jsonPathExpression) {
        try {
            T result = JsonPath.using(CONFIG).parse(json).read(jsonPathExpression);
            return Optional.ofNullable(result);
        } catch (PathNotFoundException e) {
            log.debug("JSONPath '{}' not found in document", jsonPathExpression);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to evaluate JSONPath '{}': {}", jsonPathExpression, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Evaluates a JSONPath expression and returns the result as a string.
     *
     * @param json           the JSON document
     * @param jsonPathExpression the JSONPath expression
     * @return an Optional containing the string result, or empty if not found
     */
    public static Optional<String> evaluateAsString(String json, String jsonPathExpression) {
        return evaluate(json, jsonPathExpression).map(Object::toString);
    }
}
