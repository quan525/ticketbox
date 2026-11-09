package com.example.ticketbox.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingReq {

    @JsonProperty("userId")
    @NotNull
    private Integer userId;

    @JsonProperty("showId")
    @NotNull
    private Integer showId;

    @JsonProperty("showSeatIds")
    @NotEmpty
    private List<@Positive Integer> showSeatIds;

    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;
}
