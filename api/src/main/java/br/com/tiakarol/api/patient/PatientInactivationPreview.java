package br.com.tiakarol.api.patient;

import br.com.tiakarol.api.appointment.PatientHistoryEvidence;
import java.util.UUID;

record PatientInactivationPreview(UUID patientId, String patientName, boolean requiresDoubleConfirmation,
                                  PatientHistoryEvidence evidence) { }
