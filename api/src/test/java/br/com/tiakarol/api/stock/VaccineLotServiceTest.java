package br.com.tiakarol.api.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccineLotServiceTest {
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private VaccineLotRepository lotRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditService auditService;

    private VaccineLotService service;

    @BeforeEach
    void setUp() {
        service = new VaccineLotService(lotRepository, movementRepository, currentUser, auditService);
    }

    @Test
    void createsLotWithInitialStockAndAudit() {
        VaccineLotRequest request = request(10);
        when(lotRepository.findByVaccineNameIgnoreCaseAndLotCodeIgnoreCase("Tríplice Viral", "L-001"))
                .thenReturn(Optional.empty());
        when(lotRepository.save(any(VaccineLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUser.id()).thenReturn(USER_ID);

        VaccineLotResponse response = service.createOrIncrease(request);

        assertThat(response.physicalQuantity()).isEqualTo(10);
        assertThat(response.reservedQuantity()).isZero();
        assertThat(response.availableQuantity()).isEqualTo(10);
        verify(movementRepository).save(any(StockMovement.class));
        verify(auditService).log("VACCINE_LOT", response.id(), "VACCINE_LOT_CREATED", null, response, null);
    }

    @Test
    void increasesStockWhenEveryLotAttributeMatches() {
        VaccineLot existing = new VaccineLot(request(5));
        when(lotRepository.findByVaccineNameIgnoreCaseAndLotCodeIgnoreCase("Tríplice Viral", "L-001"))
                .thenReturn(Optional.of(existing));
        when(currentUser.id()).thenReturn(USER_ID);

        VaccineLotResponse response = service.createOrIncrease(request(7));

        assertThat(response.physicalQuantity()).isEqualTo(12);
        verify(lotRepository, never()).save(any());
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void rejectsStockIncreaseWhenExistingLotDataDiverges() {
        VaccineLot existing = new VaccineLot(request(5));
        VaccineLotRequest divergent = new VaccineLotRequest("Tríplice Viral", "Dose", "L-001",
                LocalDate.now().plusYears(2), "Fabricante", "Outro fornecedor", "NF-10",
                new BigDecimal("20.00"), new BigDecimal("50.00"), null, 7);
        when(lotRepository.findByVaccineNameIgnoreCaseAndLotCodeIgnoreCase("Tríplice Viral", "L-001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createOrIncrease(divergent))
                .isInstanceOf(StockDomainException.class)
                .hasMessageContaining("dados divergentes");

        verify(movementRepository, never()).save(any());
    }

    @Test
    void requiresReasonForManualAdjustment() {
        VaccineLot lot = new VaccineLot(request(10));
        when(lotRepository.findById(lot.getId())).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> service.move(lot.getId(),
                new StockMovementRequest(StockMovementType.ADJUSTMENT, 1, " ")))
                .isInstanceOf(StockDomainException.class)
                .hasMessage("Ajuste manual exige motivo.");
    }

    @Test
    void neverAllowsPhysicalStockBelowReservedOrAvailableQuantity() {
        VaccineLot lot = new VaccineLot(request(2));
        when(lotRepository.findById(lot.getId())).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> service.move(lot.getId(),
                new StockMovementRequest(StockMovementType.LOSS, 3, "Frasco quebrado")))
                .isInstanceOf(StockDomainException.class)
                .hasMessageContaining("Saldo disponível insuficiente");
    }

    @Test
    void blocksManualMovementTypesOwnedBySchedulingWorkflow() {
        VaccineLot lot = new VaccineLot(request(10));
        when(lotRepository.findById(lot.getId())).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> service.move(lot.getId(),
                new StockMovementRequest(StockMovementType.APPLICATION, 1, "Aplicação")))
                .isInstanceOf(StockDomainException.class)
                .hasMessageContaining("controlado pela agenda");
    }

    private VaccineLotRequest request(int quantity) {
        return new VaccineLotRequest("Tríplice Viral", "Dose", "L-001", LocalDate.now().plusYears(2),
                "Fabricante", "Fornecedor", "NF-10", new BigDecimal("20.00"),
                new BigDecimal("50.00"), null, quantity);
    }
}
