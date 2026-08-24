package br.com.tiakarol.api.patient;

record PatientInactivationRequest(boolean confirmationAccepted, boolean historyEvidenceAccepted) { }
