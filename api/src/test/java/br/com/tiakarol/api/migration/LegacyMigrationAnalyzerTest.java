package br.com.tiakarol.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyMigrationAnalyzerTest {
    @Test
    void approvesConsistentSnapshotWithoutExposingPatientData() throws Exception {
        LegacySnapshot snapshot = snapshot(
                List.of(patient(1, "52998224725", "1990-01-31")),
                List.of(vaccine(10, 4, 3)),
                List.of(application(100, 1, 10, "Aplicado")));

        LegacyMigrationReport report = new LegacyMigrationAnalyzer().analyze(snapshot);

        assertThat(report.readyForImport()).isTrue();
        assertThat(report.errorCount()).isZero();
        assertThat(report.sourceCounts()).containsEntry("patients", 1L).containsEntry("applications", 1L);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(report);
        assertThat(json).doesNotContain("52998224725", "Paciente Teste");
    }

    @Test
    void reportsDuplicatesOrphansInvalidDatesAndStock() {
        LegacySnapshot snapshot = snapshot(
                List.of(patient(1, "111.111.111-11", "31/02/2020"),
                        patient(2, "11111111111", "2020-01-01")),
                List.of(vaccine(10, 1, -1)),
                List.of(application(100, 999, 888, "Status inventado")));

        LegacyMigrationReport report = new LegacyMigrationAnalyzer().analyze(snapshot);

        assertThat(report.readyForImport()).isFalse();
        assertThat(report.errorCount()).isGreaterThanOrEqualTo(5);
        assertThat(report.issues()).extracting(LegacyMigrationIssue::code).contains(
                "PATIENT_DOCUMENT_DUPLICATED", "PATIENT_BIRTH_DATE_INVALID", "VACCINE_STOCK_NEGATIVE",
                "PATIENT_CPF_INVALID", "APPLICATION_PATIENT_ORPHAN", "APPLICATION_VACCINE_ORPHAN",
                "APPLICATION_STATUS_UNKNOWN");
    }

    @Test
    void acceptsKnownLegacyDateFormats() {
        assertThat(LegacyValueNormalizer.date("2026-08-26")).contains(java.time.LocalDate.of(2026, 8, 26));
        assertThat(LegacyValueNormalizer.date("26/08/2026")).contains(java.time.LocalDate.of(2026, 8, 26));
        assertThat(LegacyValueNormalizer.date("data inválida")).isEmpty();
        assertThat(LegacyValueNormalizer.validCpf("529.982.247-25")).isTrue();
        assertThat(LegacyValueNormalizer.validCpf("111.111.111-11")).isFalse();
    }

    private LegacySnapshot snapshot(List<LegacySnapshot.PatientRow> patients,
                                    List<LegacySnapshot.VaccineRow> vaccines,
                                    List<LegacySnapshot.ApplicationRow> applications) {
        return new LegacySnapshot(patients, vaccines, applications, List.of(), List.of(), List.of(), 1);
    }

    private LegacySnapshot.PatientRow patient(long id, String document, String birthDate) {
        return new LegacySnapshot.PatientRow(id, "Paciente Teste", document, birthDate, "11999999999",
                "Nenhuma", null, null, null, null, 0);
    }

    private LegacySnapshot.VaccineRow vaccine(long id, int total, int available) {
        return new LegacySnapshot.VaccineRow(id, "Vacina Teste", "Tipo", "LOTE-1", "2027-01-01",
                "Laboratório", "Fornecedor", "NF-1", total, available, BigDecimal.TEN, BigDecimal.valueOf(100));
    }

    private LegacySnapshot.ApplicationRow application(long id, long patientId, long vaccineId, String status) {
        return new LegacySnapshot.ApplicationRow(id, patientId, vaccineId, LocalDateTime.now(), status,
                "Dinheiro", BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO);
    }
}
