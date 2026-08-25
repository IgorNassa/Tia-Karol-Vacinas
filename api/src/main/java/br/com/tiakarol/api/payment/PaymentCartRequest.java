package br.com.tiakarol.api.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PaymentCartRequest(
        @NotEmpty @Size(max = 10) List<@Valid PaymentItemRequest> payments) {

    public record PaymentItemRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) { }
}
