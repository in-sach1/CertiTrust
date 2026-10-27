package com.digitalcertificate.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageDto<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
