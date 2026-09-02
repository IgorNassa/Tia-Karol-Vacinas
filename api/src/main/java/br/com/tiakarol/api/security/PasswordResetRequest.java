package br.com.tiakarol.api.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(@NotBlank @Size(min = 12, max = 100) String password) { }
