package br.com.tiakarol.api.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

record RescheduleAppointmentRequest(@NotNull @Future OffsetDateTime scheduledAt,
                                    UUID vaccineLotId,
                                    @NotBlank String reason) { }
