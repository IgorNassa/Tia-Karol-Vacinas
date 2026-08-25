package br.com.tiakarol.api.finance;

import br.com.tiakarol.api.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpensePaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull @PastOrPresent LocalDate occurredOn,
        PaymentMethod paymentMethod,
        @Size(max = 2000) String notes) { }
