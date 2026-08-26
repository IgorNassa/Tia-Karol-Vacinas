package br.com.tiakarol.api.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class JdbcLegacySnapshotReader {
    private static final Set<String> REQUIRED_TABLES = Set.of("pacientes", "vacinas", "aplicacoes_v2",
            "lancamentos_outros", "despesas_fixas", "historico_despesas");
    private static final Set<String> ALLOWED_TABLES = Set.of("pacientes", "vacinas", "aplicacoes_v2",
            "lancamentos_outros", "despesas_fixas", "historico_despesas", "configuracoes");

    LegacySnapshot read(String url, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            assertRequiredTables(connection);
            LegacySnapshot snapshot = new LegacySnapshot(readPatients(connection), readVaccines(connection),
                    readApplications(connection), readOtherEntries(connection), readRecurringExpenses(connection),
                    readExpensePayments(connection), tableExists(connection, "configuracoes")
                    ? count(connection, "configuracoes") : 0L);
            connection.rollback();
            return snapshot;
        }
    }

    private void assertRequiredTables(Connection connection) throws SQLException {
        Set<String> found = new java.util.HashSet<>();
        try (var statement = connection.prepareStatement("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name = ANY (?)
                """)) {
            var array = connection.createArrayOf("text", REQUIRED_TABLES.toArray());
            statement.setArray(1, array);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) found.add(result.getString(1));
            }
        }
        if (!found.containsAll(REQUIRED_TABLES)) {
            Set<String> missing = new java.util.TreeSet<>(REQUIRED_TABLES);
            missing.removeAll(found);
            throw new SQLException("Tabelas obrigatórias ausentes no legado: " + String.join(", ", missing));
        }
    }

    private List<LegacySnapshot.PatientRow> readPatients(Connection connection) throws SQLException {
        List<LegacySnapshot.PatientRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                SELECT id, nome, cpf, data_nascimento, telefone, alergias,
                       nome_responsavel, cpf_responsavel, nome_responsavel_2, cpf_responsavel_2,
                       COALESCE(octet_length(foto), 0) AS photo_bytes
                FROM pacientes ORDER BY id
                """)) {
            while (result.next()) rows.add(new LegacySnapshot.PatientRow(result.getLong("id"),
                    result.getString("nome"), result.getString("cpf"), result.getString("data_nascimento"),
                    result.getString("telefone"), result.getString("alergias"),
                    result.getString("nome_responsavel"), result.getString("cpf_responsavel"),
                    result.getString("nome_responsavel_2"), result.getString("cpf_responsavel_2"),
                    result.getLong("photo_bytes")));
        }
        return rows;
    }

    private List<LegacySnapshot.VaccineRow> readVaccines(Connection connection) throws SQLException {
        List<LegacySnapshot.VaccineRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                SELECT id, nome_vacina, tipo, lote, validade, laboratorio, distribuidor, numero_nota,
                       qtd_total, qtd_disponivel, valor_compra, valor_venda
                FROM vacinas ORDER BY id
                """)) {
            while (result.next()) rows.add(new LegacySnapshot.VaccineRow(result.getLong("id"),
                    result.getString("nome_vacina"), result.getString("tipo"), result.getString("lote"),
                    result.getString("validade"), result.getString("laboratorio"), result.getString("distribuidor"),
                    result.getString("numero_nota"), result.getObject("qtd_total", Integer.class),
                    result.getObject("qtd_disponivel", Integer.class), result.getBigDecimal("valor_compra"),
                    result.getBigDecimal("valor_venda")));
        }
        return rows;
    }

    private List<LegacySnapshot.ApplicationRow> readApplications(Connection connection) throws SQLException {
        List<LegacySnapshot.ApplicationRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                SELECT id, paciente_id, vacina_id, data_hora, status, forma_pagamento,
                       valor, valor_bruto, desconto FROM aplicacoes_v2 ORDER BY id
                """)) {
            while (result.next()) {
                var timestamp = result.getTimestamp("data_hora");
                rows.add(new LegacySnapshot.ApplicationRow(result.getLong("id"), result.getLong("paciente_id"),
                        result.getLong("vacina_id"), timestamp == null ? null : timestamp.toLocalDateTime(),
                        result.getString("status"), result.getString("forma_pagamento"), result.getBigDecimal("valor"),
                        result.getBigDecimal("valor_bruto"), result.getBigDecimal("desconto")));
            }
        }
        return rows;
    }

    private List<LegacySnapshot.OtherEntryRow> readOtherEntries(Connection connection) throws SQLException {
        List<LegacySnapshot.OtherEntryRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "SELECT id, nome, tipo, valor, data_lancamento FROM lancamentos_outros ORDER BY id")) {
            while (result.next()) rows.add(new LegacySnapshot.OtherEntryRow(result.getLong("id"),
                    result.getString("nome"), result.getString("tipo"), result.getBigDecimal("valor"),
                    result.getString("data_lancamento")));
        }
        return rows;
    }

    private List<LegacySnapshot.RecurringExpenseRow> readRecurringExpenses(Connection connection) throws SQLException {
        List<LegacySnapshot.RecurringExpenseRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                SELECT id, nome, valor_padrao, valor_variavel, dia_vencimento,
                       ultimo_valor_pago, data_ultimo_pagamento FROM despesas_fixas ORDER BY id
                """)) {
            while (result.next()) rows.add(new LegacySnapshot.RecurringExpenseRow(result.getLong("id"),
                    result.getString("nome"), result.getBigDecimal("valor_padrao"),
                    result.getObject("valor_variavel", Integer.class), result.getObject("dia_vencimento", Integer.class),
                    result.getBigDecimal("ultimo_valor_pago"), result.getString("data_ultimo_pagamento")));
        }
        return rows;
    }

    private List<LegacySnapshot.ExpensePaymentRow> readExpensePayments(Connection connection) throws SQLException {
        List<LegacySnapshot.ExpensePaymentRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "SELECT id, despesa_id, valor_pago, data_pagamento FROM historico_despesas ORDER BY id")) {
            while (result.next()) rows.add(new LegacySnapshot.ExpensePaymentRow(result.getLong("id"),
                    result.getLong("despesa_id"), result.getBigDecimal("valor_pago"),
                    result.getString("data_pagamento")));
        }
        return rows;
    }

    private long count(Connection connection, String table) throws SQLException {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Tabela não permitida.");
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Tabela não permitida.");
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = current_schema() AND table_name = ?
                )
                """)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }
}
