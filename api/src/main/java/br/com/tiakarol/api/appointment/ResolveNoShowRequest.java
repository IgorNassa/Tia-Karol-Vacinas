package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record ResolveNoShowRequest(
        @NotNull NoShowResolution resolution,
        @NotBlank String reason) {
}
