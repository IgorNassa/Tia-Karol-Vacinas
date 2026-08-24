package br.com.tiakarol.api.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotNull @Valid UserRequest user,
                                @NotBlank @Size(min = 12, max = 100) String password) { }
