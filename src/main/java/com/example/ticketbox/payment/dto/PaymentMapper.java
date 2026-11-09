package com.example.ticketbox.payment.dto;

import com.example.ticketbox.entity.Payments;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;
import com.example.ticketbox.payment.dto.response.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    @Mapping(source = "bookingId", target = "booking.id")
    Payments toEntity(CreatePaymentRequest request);

    @Mapping(source = "booking.id", target = "bookingId")
    PaymentResponse toResponse(Payments payment);
}
