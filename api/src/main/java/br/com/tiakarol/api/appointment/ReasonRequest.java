package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.NotBlank;

public record ReasonRequest(@NotBlank String reason) {
}
