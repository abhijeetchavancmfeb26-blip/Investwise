package com.investwise.investment.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Stable JSON shape for a page of results, so clients never see Spring internals. */
public record PageResponse<T>(List<T> content, int pageNumber, int pageSize,
                              long totalElements, int totalPages, boolean last) {

    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
