package br.com.tiakarol.api.report;

import br.com.tiakarol.api.finance.FinancialDomainException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReportService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_PERIOD_DAYS = 366;
    private final JdbcTemplate jdbc;

    ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    FinancialSummaryResponse financialSummary(LocalDate fromDate, LocalDate toDate) {
        validatePeriod(fromDate, toDate);
        OffsetDateTime from = start(fromDate);
        OffsetDateTime until = start(toDate.plusDays(1));
        BigDecimal appointmentIncome = amount("""
                select coalesce(sum(amount), 0) from app.payment_entries
                where active = true and payment_method <> 'PENDING'
                  and received_at >= ? and received_at < ?
                """, from, until);
        BigDecimal otherIncome = amount("""
                select coalesce(sum(amount), 0) from app.financial_entries
                where active = true and entry_type = 'INCOME' and occurred_on between ? and ?
                """, fromDate, toDate);
        BigDecimal expenses = amount("""
                select coalesce(sum(amount), 0) from app.financial_entries
                where active = true and entry_type = 'EXPENSE' and occurred_on between ? and ?
                """, fromDate, toDate);
        FinancialProjectionResponse projections = projections(fromDate, toDate);
        BigDecimal realizedBalance = appointmentIncome.add(otherIncome).subtract(expenses);
        BigDecimal projectedBalance = realizedBalance.add(projections.receivables())
                .subtract(projections.recurringExpenses());
        List<FinancialSummaryResponse.MethodTotal> methods = jdbc.query("""
                select method, sum(amount) amount from (
                    select payment_method method, amount
                    from app.payment_entries
                    where active = true and payment_method <> 'PENDING'
                      and received_at >= ? and received_at < ?
                    union all
                    select coalesce(payment_method, 'UNSPECIFIED') method, amount
                    from app.financial_entries
                    where active = true and entry_type = 'INCOME' and occurred_on between ? and ?
                ) income group by method order by method
                """, (rs, row) -> new FinancialSummaryResponse.MethodTotal(rs.getString("method"),
                rs.getBigDecimal("amount")), from, until, fromDate, toDate);
        return new FinancialSummaryResponse(fromDate, toDate, appointmentIncome, otherIncome, expenses,
                realizedBalance, projections.receivables(), projections.recurringExpenses(), projectedBalance,
                methods);
    }

    @Transactional(readOnly = true)
    FinancialProjectionResponse projections(LocalDate fromDate, LocalDate toDate) {
        validatePeriod(fromDate, toDate);
        OffsetDateTime from = start(fromDate);
        OffsetDateTime until = start(toDate.plusDays(1));
        List<FinancialProjectionResponse.Item> items = new ArrayList<>(jdbc.query("""
                select a.id, p.full_name, v.name vaccine_name,
                       (a.scheduled_at at time zone 'America/Sao_Paulo')::date expected_on,
                       greatest(a.final_amount - coalesce(sum(pe.amount)
                           filter (where pe.active = true and pe.payment_method <> 'PENDING'), 0), 0) amount
                from app.appointments a
                join app.patients p on p.id = a.patient_id
                join app.vaccine_lots vl on vl.id = a.vaccine_lot_id
                join app.vaccines v on v.id = vl.vaccine_id
                left join app.payment_entries pe on pe.appointment_id = a.id
                where a.status in ('SCHEDULED', 'CONFIRMED', 'APPLIED')
                  and a.scheduled_at >= ? and a.scheduled_at < ?
                group by a.id, p.full_name, v.name, a.scheduled_at, a.final_amount
                having greatest(a.final_amount - coalesce(sum(pe.amount)
                    filter (where pe.active = true and pe.payment_method <> 'PENDING'), 0), 0) > 0
                """, (rs, row) -> new FinancialProjectionResponse.Item("APPOINTMENT",
                rs.getObject("id", UUID.class), rs.getString("full_name") + " — " + rs.getString("vaccine_name"),
                rs.getDate("expected_on").toLocalDate(), rs.getBigDecimal("amount")), from, until));
        items.addAll(jdbc.query("""
                select r.id, r.name, due.expected_on, r.default_amount amount
                from app.recurring_expenses r
                cross join lateral (
                    select make_date(extract(year from month)::int, extract(month from month)::int,
                        least(r.due_day, extract(day from (month + interval '1 month - 1 day'))::int)) expected_on
                    from generate_series(date_trunc('month', ?::date), date_trunc('month', ?::date),
                        interval '1 month') month
                ) due
                where r.active = true and due.expected_on between ? and ?
                  and not exists (
                      select 1 from app.financial_entries fe
                      where fe.recurring_expense_id = r.id and fe.active = true
                        and date_trunc('month', fe.occurred_on) = date_trunc('month', due.expected_on)
                  )
                """, (rs, row) -> new FinancialProjectionResponse.Item("RECURRING_EXPENSE",
                rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getDate("expected_on").toLocalDate(), rs.getBigDecimal("amount")),
                fromDate, toDate, fromDate, toDate));
        items.sort(Comparator.comparing(FinancialProjectionResponse.Item::expectedOn)
                .thenComparing(FinancialProjectionResponse.Item::source));
        BigDecimal receivables = items.stream().filter(item -> item.source().equals("APPOINTMENT"))
                .map(FinancialProjectionResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recurring = items.stream().filter(item -> item.source().equals("RECURRING_EXPENSE"))
                .map(FinancialProjectionResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinancialProjectionResponse(fromDate, toDate, receivables, recurring, List.copyOf(items));
    }

    @Transactional(readOnly = true)
    CashFlowReportResponse cashFlow(LocalDate fromDate, LocalDate toDate) {
        validatePeriod(fromDate, toDate);
        OffsetDateTime from = start(fromDate);
        OffsetDateTime until = start(toDate.plusDays(1));
        List<CashFlowReportResponse.Item> rows = jdbc.query("""
                select source, source_id, entry_type, description, payment_method, amount, occurred_on
                from (
                    select 'APPOINTMENT_PAYMENT' source, pe.id source_id, 'INCOME' entry_type,
                           'Pagamento do atendimento ' || pe.appointment_id description,
                           pe.payment_method, pe.amount,
                           (pe.received_at at time zone 'America/Sao_Paulo')::date occurred_on,
                           pe.received_at ordering_date
                    from app.payment_entries pe
                    where pe.active = true and pe.payment_method <> 'PENDING'
                      and pe.received_at >= ? and pe.received_at < ?
                    union all
                    select 'FINANCIAL_ENTRY' source, fe.id source_id, fe.entry_type,
                           fe.description, coalesce(fe.payment_method, 'UNSPECIFIED'), fe.amount,
                           fe.occurred_on, fe.occurred_on::timestamptz ordering_date
                    from app.financial_entries fe
                    where fe.active = true and fe.occurred_on between ? and ?
                ) ledger order by ordering_date desc, source_id
                """, (rs, row) -> new CashFlowReportResponse.Item(rs.getString("source"),
                rs.getObject("source_id", UUID.class), rs.getString("entry_type"), rs.getString("description"),
                rs.getString("payment_method"), rs.getBigDecimal("amount"),
                rs.getDate("occurred_on").toLocalDate()), from, until, fromDate, toDate);
        BigDecimal income = rows.stream().filter(row -> row.type().equals("INCOME"))
                .map(CashFlowReportResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = rows.stream().filter(row -> row.type().equals("EXPENSE"))
                .map(CashFlowReportResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashFlowReportResponse(fromDate, toDate, income, expenses, income.subtract(expenses), rows);
    }

    @Transactional(readOnly = true)
    ApplicationReportResponse applications(LocalDate fromDate, LocalDate toDate, UUID patientId) {
        validatePeriod(fromDate, toDate);
        OffsetDateTime from = start(fromDate);
        OffsetDateTime until = start(toDate.plusDays(1));
        String patientFilter = patientId == null ? "" : " and a.patient_id = ?\n";
        List<Object> arguments = new ArrayList<>(List.of(from, until));
        if (patientId != null) arguments.add(patientId);
        List<ApplicationReportResponse.Item> rows = jdbc.query("""
                select a.id, a.patient_id, p.full_name, v.id vaccine_id, v.name vaccine_name,
                       vl.lot_code, a.applied_at, a.application_location, a.final_amount,
                       coalesce(sum(pe.amount) filter (where pe.active = true
                           and pe.payment_method <> 'PENDING'), 0) received_amount,
                       case when bool_or(pe.active = true and pe.payment_method = 'PENDING') then 'PENDING'
                            when coalesce(sum(pe.amount) filter (where pe.active = true
                                and pe.payment_method <> 'PENDING'), 0) >= a.final_amount then 'PAID'
                            else 'PENDING' end payment_status
                from app.appointments a
                join app.patients p on p.id = a.patient_id
                join app.vaccine_lots vl on vl.id = a.vaccine_lot_id
                join app.vaccines v on v.id = vl.vaccine_id
                left join app.payment_entries pe on pe.appointment_id = a.id
                where a.status = 'APPLIED' and a.applied_at >= ? and a.applied_at < ?
                """ + patientFilter + """
                group by a.id, a.patient_id, p.full_name, v.id, v.name, vl.lot_code,
                         a.applied_at, a.application_location, a.final_amount
                order by a.applied_at desc
                """, (rs, row) -> new ApplicationReportResponse.Item(rs.getObject("id", UUID.class),
                rs.getObject("patient_id", UUID.class), rs.getString("full_name"),
                rs.getObject("vaccine_id", UUID.class), rs.getString("vaccine_name"), rs.getString("lot_code"),
                rs.getObject("applied_at", OffsetDateTime.class), rs.getString("application_location"),
                rs.getBigDecimal("final_amount"), rs.getBigDecimal("received_amount"),
                rs.getString("payment_status")), arguments.toArray());
        BigDecimal total = rows.stream().map(ApplicationReportResponse.Item::finalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ApplicationReportResponse(fromDate, toDate, patientId, rows.size(), total, rows);
    }

    @Transactional(readOnly = true)
    StockReportResponse stock() {
        List<StockReportResponse.Item> rows = jdbc.query("""
                select vl.id lot_id, v.id vaccine_id, v.name vaccine_name, vl.lot_code,
                       vl.expiration_date, vl.active, sb.physical_quantity, sb.reserved_quantity,
                       sb.physical_quantity - sb.reserved_quantity available_quantity,
                       vl.purchase_price * sb.physical_quantity acquisition_value,
                       vl.sale_price * (sb.physical_quantity - sb.reserved_quantity) potential_sale_value
                from app.vaccine_lots vl
                join app.vaccines v on v.id = vl.vaccine_id
                join app.stock_balances sb on sb.vaccine_lot_id = vl.id
                order by v.name, vl.expiration_date, vl.lot_code
                """, (rs, row) -> new StockReportResponse.Item(rs.getObject("lot_id", UUID.class),
                rs.getObject("vaccine_id", UUID.class), rs.getString("vaccine_name"), rs.getString("lot_code"),
                rs.getDate("expiration_date").toLocalDate(), rs.getBoolean("active"),
                rs.getInt("physical_quantity"), rs.getInt("reserved_quantity"),
                rs.getInt("available_quantity"), rs.getBigDecimal("acquisition_value"),
                rs.getBigDecimal("potential_sale_value")));
        int physical = rows.stream().mapToInt(StockReportResponse.Item::physicalQuantity).sum();
        int reserved = rows.stream().mapToInt(StockReportResponse.Item::reservedQuantity).sum();
        int available = rows.stream().mapToInt(StockReportResponse.Item::availableQuantity).sum();
        BigDecimal acquisition = rows.stream().map(StockReportResponse.Item::acquisitionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sale = rows.stream().map(StockReportResponse.Item::potentialSaleValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockReportResponse(physical, reserved, available, acquisition, sale, rows);
    }

    private BigDecimal amount(String sql, Object... arguments) {
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, arguments);
        return result == null ? BigDecimal.ZERO : result;
    }

    private OffsetDateTime start(LocalDate date) {
        return date.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new FinancialDomainException("A data final deve ser igual ou posterior à data inicial.");
        }
        if (fromDate.plusDays(MAX_PERIOD_DAYS).isBefore(toDate)) {
            throw new FinancialDomainException("O período máximo para relatórios é de 366 dias.");
        }
    }
}
