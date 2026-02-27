package com.umg.dto;

import lombok.*;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> the type of elements in the page
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    /** The content items for the current page. */
    private List<T> content;

    /** The current page number (zero-based). */
    private int page;

    /** The number of items per page. */
    private int size;

    /** The total number of matching elements across all pages. */
    private long totalElements;

    /** The total number of pages. */
    private int totalPages;

    /** Whether this is the first page. */
    private boolean first;

    /** Whether this is the last page. */
    private boolean last;

    /**
     * Creates a {@link PageResponse} from a Spring Data {@link org.springframework.data.domain.Page}.
     *
     * @param page    the Spring Data page
     * @param content the mapped content
     * @param <T>     the content element type
     * @return a new PageResponse
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
