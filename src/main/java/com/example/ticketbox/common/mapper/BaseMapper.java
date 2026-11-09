package com.example.ticketbox.common.mapper;

import java.util.List;

public interface BaseMapper<E, R> {

    R toResponse(E entity);

    List<R> toResponses(List<E> entities);
}