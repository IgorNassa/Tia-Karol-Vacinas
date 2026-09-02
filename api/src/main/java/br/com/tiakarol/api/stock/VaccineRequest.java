package br.com.tiakarol.api.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record VaccineRequest(@NotBlank @Size(max = 255) String name,
                      @Size(max = 100) String vaccineType,
                      @Size(max = 255) String manufacturer) { }
