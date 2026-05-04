package br.sistema.repository;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.model.Vacina;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AplicacaoDAO {

    public AplicacaoDAO() {
        criarTabelaSeNaoExistir();
        aplicarMigrationsFinanceiras();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS aplicacoes_v2 ("
                + "id SERIAL PRIMARY KEY, paciente_id INTEGER NOT NULL, vacina_id INTEGER NOT NULL, data_hora TIMESTAMP NOT NULL, status VARCHAR(50) NOT NULL, forma_pagamento VARCHAR(50), valor NUMERIC(10,2), reacoes TEXT, observacoes_adicionais TEXT, FOREIGN KEY(paciente_id) REFERENCES pacientes(id), FOREIGN KEY(vacina_id) REFERENCES vacinas(id))";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute(sql); } catch (SQLException e) { }
    }

    private void aplicarMigrationsFinanceiras() {
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE aplicacoes_v2 ADD COLUMN valor_bruto NUMERIC(10,2)"); } catch (SQLException ignored) {}
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE aplicacoes_v2 ADD COLUMN desconto NUMERIC(10,2)"); } catch (SQLException ignored) {}
    }

    public int buscarQuantidadeReservada(int vacinaId) {
        int qtd = 0;
        String sql = "SELECT COUNT(*) FROM aplicacoes_v2 WHERE vacina_id = ? AND status = 'Agendado'";
        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, vacinaId);
            try (ResultSet rs = p.executeQuery()) { if (rs.next()) qtd = rs.getInt(1); }
        } catch (SQLException e) {}
        return qtd;
    }

    public boolean salvarEmLote(List<Aplicacao> aplicacoes) {
        String sqlApp = "INSERT INTO aplicacoes_v2 (paciente_id, vacina_id, data_hora, status, forma_pagamento, valor, valor_bruto, desconto, reacoes, observacoes_adicionais) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlEstoque = "UPDATE vacinas SET qtd_disponivel = qtd_disponivel - 1 WHERE id = ?";
        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection(); conn.setAutoCommit(false);
            try (PreparedStatement stmtApp = conn.prepareStatement(sqlApp); PreparedStatement stmtEst = conn.prepareStatement(sqlEstoque)) {
                for (Aplicacao app : aplicacoes) {
                    stmtApp.setInt(1, app.getPaciente().getId());
                    stmtApp.setInt(2, app.getVacina().getId());
                    stmtApp.setTimestamp(3, java.sql.Timestamp.valueOf(app.getDataHora()));
                    stmtApp.setString(4, app.getStatus());
                    stmtApp.setString(5, app.getFormaPagamento());
                    stmtApp.setDouble(6, app.getValor());
                    stmtApp.setDouble(7, app.getValorBruto());
                    stmtApp.setDouble(8, app.getDesconto());
                    stmtApp.setString(9, app.getReacoes() != null ? app.getReacoes() : "");
                    stmtApp.setString(10, app.getObservacoesAdicionais() != null ? app.getObservacoesAdicionais() : "");
                    stmtApp.executeUpdate();

                    if (app.getStatus().equalsIgnoreCase("Aplicado")) {
                        stmtEst.setInt(1, app.getVacina().getId());
                        stmtEst.executeUpdate();
                    }
                }
            }
            conn.commit(); return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch(Exception ex) {}
            return false;
        } finally { if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch(Exception ex) {} }
    }

    public boolean salvar(Aplicacao app) {
        String sql = "INSERT INTO aplicacoes_v2 (paciente_id, vacina_id, data_hora, status, forma_pagamento, valor, valor_bruto, desconto, reacoes, observacoes_adicionais) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlEstoque = "UPDATE vacinas SET qtd_disponivel = qtd_disponivel - 1 WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); PreparedStatement stmtEst = conn.prepareStatement(sqlEstoque)) {
            stmt.setInt(1, app.getPaciente().getId());
            stmt.setInt(2, app.getVacina().getId());
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(app.getDataHora()));
            stmt.setString(4, app.getStatus());
            stmt.setString(5, app.getFormaPagamento());
            stmt.setDouble(6, app.getValor());
            stmt.setDouble(7, app.getValorBruto());
            stmt.setDouble(8, app.getDesconto());
            stmt.setString(9, app.getReacoes());
            stmt.setString(10, app.getObservacoesAdicionais());
            stmt.executeUpdate();

            if (app.getStatus().equalsIgnoreCase("Aplicado")) {
                stmtEst.setInt(1, app.getVacina().getId());
                stmtEst.executeUpdate();
            }
            return true;
        } catch (SQLException e) { return false; }
    }

    public boolean atualizar(Aplicacao app) {
        String sqlAntigo = "SELECT status FROM aplicacoes_v2 WHERE id = ?";
        String statusAntigo = "";
        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement(sqlAntigo)) {
            p.setInt(1, app.getId());
            ResultSet rs = p.executeQuery();
            if (rs.next()) statusAntigo = rs.getString("status");
        } catch (Exception e) {}

        String sql = "UPDATE aplicacoes_v2 SET paciente_id=?, vacina_id=?, data_hora=?, status=?, forma_pagamento=?, valor=?, valor_bruto=?, desconto=?, reacoes=?, observacoes_adicionais=? WHERE id=?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, app.getPaciente().getId());
            stmt.setInt(2, app.getVacina().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(app.getDataHora()));
            stmt.setString(4, app.getStatus());
            stmt.setString(5, app.getFormaPagamento());
            stmt.setDouble(6, app.getValor());
            stmt.setDouble(7, app.getValorBruto());
            stmt.setDouble(8, app.getDesconto());
            stmt.setString(9, app.getReacoes());
            stmt.setString(10, app.getObservacoesAdicionais());
            stmt.setInt(11, app.getId());
            stmt.executeUpdate();

            if (!statusAntigo.equalsIgnoreCase("Aplicado") && app.getStatus().equalsIgnoreCase("Aplicado")) {
                String sqlEstoque = "UPDATE vacinas SET qtd_disponivel = qtd_disponivel - 1 WHERE id = ?";
                try(PreparedStatement pEst = conn.prepareStatement(sqlEstoque)){
                    pEst.setInt(1, app.getVacina().getId()); pEst.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) { return false; }
    }

    public boolean atualizarStatusEPagamento(int id, String status, String formaPagamento) {
        String sqlAntigo = "SELECT status, vacina_id FROM aplicacoes_v2 WHERE id = ?";
        String statusAntigo = "";
        int idVacina = -1;
        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement(sqlAntigo)) {
            p.setInt(1, id);
            ResultSet rs = p.executeQuery();
            if (rs.next()) { statusAntigo = rs.getString("status"); idVacina = rs.getInt("vacina_id"); }
        } catch (Exception e) {}

        String sql = "UPDATE aplicacoes_v2 SET status = ?, forma_pagamento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status); stmt.setString(2, formaPagamento); stmt.setInt(3, id);
            int rows = stmt.executeUpdate();

            if (rows > 0 && !statusAntigo.equalsIgnoreCase("Aplicado") && status.equalsIgnoreCase("Aplicado") && idVacina != -1) {
                String sqlEstoque = "UPDATE vacinas SET qtd_disponivel = qtd_disponivel - 1 WHERE id = ?";
                try(PreparedStatement pEst = conn.prepareStatement(sqlEstoque)){
                    pEst.setInt(1, idVacina); pEst.executeUpdate();
                }
            }
            return rows > 0;
        } catch (SQLException e) { return false; }
    }

    public List<Aplicacao> listarTodas() {
        List<Aplicacao> lista = new ArrayList<>();
        String sql = "SELECT a.*, p.nome as nome_paciente, v.nome_vacina, v.lote FROM aplicacoes_v2 a JOIN pacientes p ON a.paciente_id = p.id JOIN vacinas v ON a.vacina_id = v.id ORDER BY a.data_hora DESC";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Aplicacao app = new Aplicacao(); app.setId(rs.getInt("id"));
                Paciente p = new Paciente(); p.setId(rs.getInt("paciente_id")); p.setNome(rs.getString("nome_paciente")); app.setPaciente(p);
                Vacina v = new Vacina(); v.setId(rs.getInt("vacina_id")); v.setNomeVacina(rs.getString("nome_vacina")); v.setLote(rs.getString("lote")); app.setVacina(v);
                if (rs.getTimestamp("data_hora") != null) app.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                app.setStatus(rs.getString("status"));
                app.setFormaPagamento(rs.getString("forma_pagamento"));
                app.setValor(rs.getDouble("valor"));

                // Trata caso a coluna seja nula em bancos velhos
                app.setValorBruto(rs.getDouble("valor_bruto") == 0 ? rs.getDouble("valor") : rs.getDouble("valor_bruto"));
                app.setDesconto(rs.getDouble("desconto"));

                app.setReacoes(rs.getString("reacoes"));
                app.setObservacoesAdicionais(rs.getString("observacoes_adicionais"));
                lista.add(app);
            }
        } catch (SQLException e) { } return lista;
    }

    public boolean excluir(int id) {
        String sql = "DELETE FROM aplicacoes_v2 WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}