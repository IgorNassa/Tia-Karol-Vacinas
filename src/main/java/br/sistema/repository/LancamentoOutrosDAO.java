package br.sistema.repository;

import br.sistema.model.LancamentoOutros;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LancamentoOutrosDAO {

    public LancamentoOutrosDAO() { criarTabelaSeNaoExistir(); }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS lancamentos_outros ("
                + "id SERIAL PRIMARY KEY,"
                + "nome VARCHAR(255) NOT NULL,"
                + "tipo VARCHAR(50) NOT NULL,"
                + "valor NUMERIC(10,2),"
                + "data_lancamento VARCHAR(50))";

        try (java.sql.Connection conn = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (java.sql.SQLException e) {
            System.out.println("Erro ao criar tabela lancamentos_outros no Postgres: " + e.getMessage());
        }
    }

    public boolean salvar(LancamentoOutros l) {
        String sql = "INSERT INTO lancamentos_outros (nome, tipo, valor, data_lancamento) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, l.getNome()); pstmt.setString(2, l.getTipo());
            pstmt.setDouble(3, l.getValor()); pstmt.setString(4, l.getDataLancamento().toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean atualizar(LancamentoOutros l) {
        String sql = "UPDATE lancamentos_outros SET nome = ?, tipo = ?, valor = ?, data_lancamento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, l.getNome()); pstmt.setString(2, l.getTipo());
            pstmt.setDouble(3, l.getValor()); pstmt.setString(4, l.getDataLancamento().toString()); pstmt.setInt(5, l.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM lancamentos_outros WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.executeUpdate();
        } catch (SQLException e) { }
    }

    public List<LancamentoOutros> listarTodos() {
        List<LancamentoOutros> lista = new ArrayList<>();
        String sql = "SELECT * FROM lancamentos_outros ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                LancamentoOutros l = new LancamentoOutros();
                l.setId(rs.getInt("id")); l.setNome(rs.getString("nome"));
                l.setTipo(rs.getString("tipo")); l.setValor(rs.getDouble("valor"));
                l.setDataLancamento(LocalDate.parse(rs.getString("data_lancamento")));
                lista.add(l);
            }
        } catch (Exception e) { }
        return lista;
    }
}