package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        /** Demo/testing hook: set true to force the simulated payment to fail. */
        Boolean forceFailure
) {}
