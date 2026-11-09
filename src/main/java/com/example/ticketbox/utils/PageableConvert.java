package com.example.ticketbox.utils;

import com.example.ticketbox.user.dto.request.Pagination;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

public final class PageableConvert {

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_SORT_FIELD = "id";

    private PageableConvert() {
    }

    public static Pageable toPageable(Pagination pagination) {

        if (pagination == null) {
            return Pageable.unpaged();
        }

        int limit = Optional.ofNullable(pagination.getLimit())
                .filter(l -> l > 0)
                .orElse(DEFAULT_LIMIT);

        int offset = Optional.ofNullable(pagination.getOffset())
                .filter(o -> o >= 0)
                .orElse(DEFAULT_OFFSET);

        Sort sort = resolveSort(pagination);

        return PageRequest.of(offset, limit, sort);
    }

    private static Sort resolveSort(Pagination pagination) {
        if (pagination.getSortBy() == null || pagination.getDirection() == null) {
            return Sort.by(DEFAULT_SORT_FIELD).ascending();
        }
        return Sort.by(pagination.getDirection(), pagination.getSortBy());
    }
}
