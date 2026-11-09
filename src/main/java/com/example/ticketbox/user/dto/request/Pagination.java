package com.example.ticketbox.user.dto.request;

import lombok.*;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagination {
    @Builder.Default
    private Integer limit = 20;     // page size
    @Builder.Default
    private Integer offset = 0;     // row offset
    @Builder.Default
    private String sortBy = "id";
    @Builder.Default
    private Sort.Direction direction = Sort.Direction.ASC;
}