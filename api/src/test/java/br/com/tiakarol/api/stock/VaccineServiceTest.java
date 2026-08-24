package br.com.tiakarol.api.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccineServiceTest {
    @Mock private VaccineRepository repository;
    @Mock private AuditService auditService;
    private VaccineService service;

    @BeforeEach void setUp() { service = new VaccineService(repository, auditService); }

    @Test
    void createsNormalizedCatalogItemAndAudits() {
        when(repository.save(any(Vaccine.class))).thenAnswer(call -> call.getArgument(0));
        VaccineResponse response = service.create(new VaccineRequest("  Influenza  ", " Dose ", " Fabricante "));
        assertThat(response.name()).isEqualTo("Influenza");
        assertThat(response.active()).isTrue();
        verify(auditService).log("VACCINE", response.id(), "VACCINE_CREATED", null, response, null);
    }

    @Test
    void rejectsDuplicateBusinessKey() {
        when(repository.existsByBusinessKey("Influenza", "Dose", "Fabricante")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new VaccineRequest("Influenza", "Dose", "Fabricante")))
                .isInstanceOf(StockDomainException.class).hasMessageContaining("Já existe vacina");
    }

    @Test
    void blocksNewLotForInactiveVaccine() {
        Vaccine vaccine = new Vaccine(new VaccineRequest("Influenza", null, null));
        vaccine.inactivate();
        when(repository.findById(vaccine.getId())).thenReturn(Optional.of(vaccine));
        assertThatThrownBy(() -> service.findActive(vaccine.getId()))
                .isInstanceOf(StockDomainException.class).hasMessageContaining("inativa");
    }
}
