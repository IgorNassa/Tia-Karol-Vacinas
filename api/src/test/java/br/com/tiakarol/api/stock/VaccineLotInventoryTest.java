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
class VaccineLotInventoryTest {
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID APPOINTMENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private VaccineLotRepository lotRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditService auditService;

    private VaccineLotInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new VaccineLotInventory(lotRepository, movementRepository, currentUser, auditService);
    }

    @Test
    void reservationChangesOnlyReservedAndAvailableQuantities() {
        VaccineLot lot = lot(LocalDate.now().plusYears(1), 2);
        prepare(lot);

        inventory.reserve(lot.getId(), 1, APPOINTMENT_ID);

        assertThat(lot.getBalance().getPhysicalQuantity()).isEqualTo(2);
        assertThat(lot.getBalance().getReservedQuantity()).isEqualTo(1);
        assertThat(lot.getBalance().getAvailableQuantity()).isEqualTo(1);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void applicationConsumesPhysicalAndReservedDoseTogether() {
        VaccineLot lot = lot(LocalDate.now().plusYears(1), 2);
        prepare(lot);
        inventory.reserve(lot.getId(), 1, APPOINTMENT_ID);

        inventory.apply(lot.getId(), 1, APPOINTMENT_ID);

        assertThat(lot.getBalance().getPhysicalQuantity()).isEqualTo(1);
        assertThat(lot.getBalance().getReservedQuantity()).isZero();
    }

    @Test
    void cancellationReturnsReservationWithoutIncreasingPhysicalStock() {
        VaccineLot lot = lot(LocalDate.now().plusYears(1), 2);
        prepare(lot);
        inventory.reserve(lot.getId(), 1, APPOINTMENT_ID);

        inventory.release(lot.getId(), 1, APPOINTMENT_ID, "Cancelamento solicitado");

        assertThat(lot.getBalance().getPhysicalQuantity()).isEqualTo(2);
        assertThat(lot.getBalance().getReservedQuantity()).isZero();
        assertThat(lot.getBalance().getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void expiredLotCanNeverBeReserved() {
        VaccineLot lot = lot(LocalDate.now().minusDays(1), 2);
        when(lotRepository.findForUpdateById(lot.getId())).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> inventory.reserve(lot.getId(), 1, APPOINTMENT_ID))
                .isInstanceOf(StockDomainException.class)
                .hasMessageContaining("Lote vencido");

        verify(movementRepository, never()).save(any());
    }

    private void prepare(VaccineLot lot) {
        when(lotRepository.findForUpdateById(lot.getId())).thenReturn(Optional.of(lot));
        when(currentUser.id()).thenReturn(USER_ID);
    }

    private VaccineLot lot(LocalDate expirationDate, int quantity) {
        Vaccine vaccine = new Vaccine(new VaccineRequest("Vacina", "Dose", "Fabricante"));
        return new VaccineLot(vaccine, new VaccineLotRequest(vaccine.getId(), "LOTE-1", expirationDate,
                "Fornecedor", "NF-1", BigDecimal.TEN, new BigDecimal("20.00"),
                null, quantity));
    }
}
