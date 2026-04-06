package br.sistema.repository;

import br.sistema.model.DespesaFixa;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DespesaFixaDAO {

    public DespesaFixaDAO() { criarTabelaSeNaoExistir(); }

    private void criarTabelaSeNaoExistir() {
        String sqlDespesas = "CREATE TABLE IF NOT EXISTS despesas_fixas ("
                + "id SERIAL PRIMARY KEY,"
                + "nome VARCHAR(255) NOT NULL,"
                + "valor_padrao NUMERIC(10,2),"
                + "valor_variavel INTEGER,"
                + "dia_vencimento INTEGER,"
                + "ultimo_valor_pago NUMERIC(10,2),"
                + "data_ultimo_pagamento VARCHAR(50))"; // Deixei VARCHAR pra não quebrar sua formatação antiga

        String sqlHistorico = "CREATE TABLE IF NOT EXISTS historico_despesas ("
                + "id SERIAL PRIMARY KEY,"
                + "despesa_id INTEGER NOT NULL,"
                + "valor_pago NUMERIC(10,2),"
                + "data_pagamento VARCHAR(50),"
                + "FOREIGN KEY(despesa_id) REFERENCES despesas_fixas(id))";

        try (java.sql.Connection conn = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sqlDespesas);
            stmt.execute(sqlHistorico);
        } catch (java.sql.SQLException e) {
            System.out.println("Erro ao criar tabelas de despesas no Postgres: " + e.getMessage());
        }
    }

    public boolean salvar(DespesaFixa d) {
        String sql = "INSERT INTO despesas_fixas (nome, valor_padrao, valor_variavel, dia_vencimento, ultimo_valor_pago) VALUES (?, ?, ?, ?, 0.0)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getNome()); pstmt.setDouble(2, d.getValorPadrao());
            pstmt.setInt(3, d.isValorVariavel() ? 1 : 0); pstmt.setInt(4, d.getDiaVencimento());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean atualizar(DespesaFixa d) {
        String sql = "UPDATE despesas_fixas SET nome = ?, valor_padrao = ?, valor_variavel = ?, dia_vencimento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getNome()); pstmt.setDouble(2, d.getValorPadrao());
            pstmt.setInt(3, d.isValorVariavel() ? 1 : 0); pstmt.setInt(4, d.getDiaVencimento()); pstmt.setInt(5, d.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<DespesaFixa> listarTodas() {
        List<DespesaFixa> lista = new ArrayList<>();
        String sql = "SELECT * FROM despesas_fixas ORDER BY dia_vencimento ASC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                DespesaFixa d = new DespesaFixa();
                d.setId(rs.getInt("id")); d.setNome(rs.getString("nome"));
                d.setValorPadrao(rs.getDouble("valor_padrao")); d.setValorVariavel(rs.getInt("valor_variavel") == 1);
                d.setDiaVencimento(rs.getInt("dia_vencimento")); d.setUltimoValorPago(rs.getDouble("ultimo_valor_pago"));
                String dt = rs.getString("data_ultimo_pagamento");
                if (dt != null && !dt.isEmpty()) d.setDataUltimoPagamento(LocalDate.parse(dt));
                lista.add(d);
            }
        } catch (Exception e) { }
        return lista;
    }

    public void registrarPagamento(int id, double valorPago) {
        String dataHoje = LocalDate.now().toString();
        String sqlUpdate = "UPDATE despesas_fixas SET ultimo_valor_pago = ?, data_ultimo_pagamento = ? WHERE id = ?";
        String sqlInsert = "INSERT INTO historico_despesas (despesa_id, valor_pago, data_pagamento) VALUES (?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstUp = conn.prepareStatement(sqlUpdate); PreparedStatement pstIns = conn.prepareStatement(sqlInsert)) {
            pstUp.setDouble(1, valorPago); pstUp.setString(2, dataHoje); pstUp.setInt(3, id); pstUp.executeUpdate();
            pstIns.setInt(1, id); pstIns.setDouble(2, valorPago); pstIns.setString(3, dataHoje); pstIns.executeUpdate();
        } catch (SQLException e) { }
    }

    public List<Object[]> listarHistorico(int despesaId) {
        List<Object[]> historico = new ArrayList<>();
        String sql = "SELECT id, valor_pago, data_pagamento FROM historico_despesas WHERE despesa_id = ? ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, despesaId);
            ResultSet rs = pstmt.executeQuery();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            while (rs.next()) {
                int idHist = rs.getInt("id");
                String dataStr = LocalDate.parse(rs.getString("data_pagamento")).format(fmt);
                String valorStr = String.format("R$ %.2f", rs.getDouble("valor_pago"));
                historico.add(new Object[]{idHist, dataStr, valorStr});
            }
        } catch (SQLException e) { }
        return historico;
    }

    public void excluirHistorico(int idHistorico, int idDespesa) {
        String sqlDelete = "DELETE FROM historico_despesas WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
            pstmt.setInt(1, idHistorico);
            pstmt.executeUpdate();

            String sqlLast = "SELECT valor_pago, data_pagamento FROM historico_despesas WHERE despesa_id = ? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement pstLast = conn.prepareStatement(sqlLast)) {
                pstLast.setInt(1, idDespesa);
                ResultSet rs = pstLast.executeQuery();
                if (rs.next()) {
                    String up = "UPDATE despesas_fixas SET ultimo_valor_pago = ?, data_ultimo_pagamento = ? WHERE id = ?";
                    try (PreparedStatement pu = conn.prepareStatement(up)) {
                        pu.setDouble(1, rs.getDouble("valor_pago")); pu.setString(2, rs.getString("data_pagamento")); pu.setInt(3, idDespesa); pu.executeUpdate();
                    }
                } else {
                    String up = "UPDATE despesas_fixas SET ultimo_valor_pago = 0, data_ultimo_pagamento = NULL WHERE id = ?";
                    try (PreparedStatement pu = conn.prepareStatement(up)) { pu.setInt(1, idDespesa); pu.executeUpdate(); }
                }
            }
        } catch (SQLException e) { }
    }

    // --- NOVO MÉTODO: EXCLUIR DESPESA E SEU HISTÓRICO ---
    public void excluir(int id) {
        String sqlHist = "DELETE FROM historico_despesas WHERE despesa_id = ?";
        String sqlDesp = "DELETE FROM despesas_fixas WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstHist = conn.prepareStatement(sqlHist);
             PreparedStatement pstDesp = conn.prepareStatement(sqlDesp)) {

            pstHist.setInt(1, id);
            pstHist.executeUpdate();

            pstDesp.setInt(1, id);
            pstDesp.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao excluir despesa: " + e.getMessage()); }
    }
}