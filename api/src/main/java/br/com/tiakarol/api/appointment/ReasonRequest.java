package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.NotBlank;

record ReasonRequest(@NotBlank String reason) {
}
