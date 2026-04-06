package br.sistema.repository;

import br.sistema.model.Configuracao;
import java.sql.*;

public class ConfiguracaoDAO {

    public ConfiguracaoDAO() { criarTabelaSeNaoExistir(); }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS configuracoes ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1), " // Continua igual, perfeito!
                + "nome_clinica VARCHAR(255), "
                + "cnpj VARCHAR(20), "
                + "telefone VARCHAR(20), "
                + "endereco TEXT, "
                + "logo BYTEA)"; // BLOB vira BYTEA

        try (java.sql.Connection conn = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (java.sql.SQLException e) {
            System.out.println("Erro ao criar tabela configuracoes no Postgres: " + e.getMessage());
        }
    }

    public void salvarOuAtualizar(Configuracao config) {
        // Usa o INSERT OR REPLACE do SQLite para sempre manter apenas o ID 1
        String sql = "INSERT OR REPLACE INTO configuracoes (id, nome_clinica, cnpj, telefone, endereco, logo) VALUES (1, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, config.getNomeClinica());
            pstmt.setString(2, config.getCnpj());
            pstmt.setString(3, config.getTelefone());
            pstmt.setString(4, config.getEndereco());
            pstmt.setBytes(5, config.getLogo());
            pstmt.executeUpdate();
        } catch (SQLException e) { }
    }

    public Configuracao obterConfiguracao() {
        String sql = "SELECT * FROM configuracoes WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return new Configuracao(rs.getString("nome_clinica"), rs.getString("cnpj"),
                        rs.getString("telefone"), rs.getString("endereco"), rs.getBytes("logo"));
            }
        } catch (SQLException e) { }
        return null; // Retorna null se não houver configuração salva ainda
    }
}