package br.sistema.repository;

import br.sistema.model.Vacina;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VacinaDAO {

    public VacinaDAO() {
        criarTabelaSeNaoExistir();
        aplicarMigrations();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS vacinas ("
                + "id SERIAL PRIMARY KEY,"
                + "nome_vacina VARCHAR(255) NOT NULL,"
                + "tipo VARCHAR(100),"
                + "lote VARCHAR(100) NOT NULL,"
                + "validade VARCHAR(20),"
                + "laboratorio VARCHAR(255),"
                + "distribuidor VARCHAR(255),"
                + "numero_nota VARCHAR(100),"
                + "qtd_total INTEGER,"
                + "qtd_disponivel INTEGER,"
                + "valor_compra NUMERIC(10,2),"
                + "valor_venda NUMERIC(10,2),"
                + "observacoes TEXT,"
                + "data_cadastro VARCHAR(50))";

        try (java.sql.Connection conn = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (java.sql.SQLException e) {
            System.out.println("Erro ao criar tabela vacinas no BD: " + e.getMessage());
        }
    }

    private void aplicarMigrations() {
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE vacinas ADD COLUMN tipo TEXT"); } catch (SQLException ignored) {}
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE vacinas ADD COLUMN observacoes TEXT"); } catch (SQLException ignored) {}
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE vacinas ADD COLUMN data_cadastro TEXT"); } catch (SQLException ignored) {}
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE vacinas ADD COLUMN distribuidor VARCHAR(255)"); } catch (SQLException ignored) {}
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute("ALTER TABLE vacinas ADD COLUMN numero_nota VARCHAR(100)"); } catch (SQLException ignored) {}
    }

    public void salvar(Vacina v) {
        String dataHoraAtual = LocalDateTime.now().toString();

        String sql = "INSERT INTO vacinas (nome_vacina, tipo, lote, validade, laboratorio, distribuidor, numero_nota, qtd_total, qtd_disponivel, valor_compra, valor_venda, observacoes, data_cadastro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, v.getNomeVacina());
            pstmt.setString(2, v.getTipo());
            pstmt.setString(3, v.getLote());
            pstmt.setString(4, v.getValidade() != null ? v.getValidade().toString() : null);
            pstmt.setString(5, v.getLaboratorio());
            pstmt.setString(6, v.getDistribuidor());
            pstmt.setString(7, v.getNumeroNota());
            pstmt.setInt(8, v.getQtdTotal());
            pstmt.setInt(9, v.getQtdDisponivel());
            pstmt.setDouble(10, v.getValorCompra());
            pstmt.setDouble(11, v.getValorVenda());
            pstmt.setString(12, v.getObservacoes());
            pstmt.setString(13, dataHoraAtual);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao salvar: " + e.getMessage()); }
    }

    public void atualizar(Vacina v) {
        String sql = "UPDATE vacinas SET nome_vacina = ?, tipo = ?, lote = ?, validade = ?, laboratorio = ?, distribuidor = ?, numero_nota = ?, qtd_total = ?, qtd_disponivel = ?, valor_compra = ?, valor_venda = ?, observacoes = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, v.getNomeVacina());
            pstmt.setString(2, v.getTipo());
            pstmt.setString(3, v.getLote());
            pstmt.setString(4, v.getValidade() != null ? v.getValidade().toString() : null);
            pstmt.setString(5, v.getLaboratorio());
            pstmt.setString(6, v.getDistribuidor());
            pstmt.setString(7, v.getNumeroNota());
            pstmt.setInt(8, v.getQtdTotal());
            pstmt.setInt(9, v.getQtdDisponivel());
            pstmt.setDouble(10, v.getValorCompra());
            pstmt.setDouble(11, v.getValorVenda());
            pstmt.setString(12, v.getObservacoes());
            pstmt.setInt(13, v.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao atualizar: " + e.getMessage()); }
    }

    public List<Vacina> listarTodas() {
        List<Vacina> lista = new ArrayList<>();
        String sql = "SELECT * FROM vacinas ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vacina v = new Vacina();
                v.setId(rs.getInt("id"));
                v.setNomeVacina(rs.getString("nome_vacina"));
                v.setTipo(rs.getString("tipo"));
                v.setLote(rs.getString("lote"));

                String dataStr = rs.getString("validade");
                if (dataStr != null && !dataStr.isEmpty()) v.setValidade(LocalDate.parse(dataStr));

                v.setLaboratorio(rs.getString("laboratorio"));
                v.setDistribuidor(rs.getString("distribuidor"));
                v.setNumeroNota(rs.getString("numero_nota"));
                v.setQtdTotal(rs.getInt("qtd_total"));
                v.setQtdDisponivel(rs.getInt("qtd_disponivel"));
                v.setValorCompra(rs.getDouble("valor_compra"));
                v.setValorVenda(rs.getDouble("valor_venda"));
                v.setObservacoes(rs.getString("observacoes"));

                String cadastroStr = rs.getString("data_cadastro");
                if (cadastroStr != null && !cadastroStr.isEmpty()) v.setDataCadastro(LocalDateTime.parse(cadastroStr));

                lista.add(v);
            }
        } catch (SQLException e) { }
        return lista;
    }

    public void excluir(int id) {
        String sql = "DELETE FROM vacinas WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.executeUpdate();
        } catch (SQLException e) { }
    }

    public List<String> listarLotesParaCombo() {
        List<String> itens = new ArrayList<>();
        itens.add("Selecione a vacina...");
        String sql = "SELECT nome_vacina, lote FROM vacinas WHERE qtd_disponivel > 0 ORDER BY nome_vacina ASC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String nome = rs.getString("nome_vacina");
                String lote = rs.getString("lote");
                itens.add(nome + " (Lote: " + lote + ")");
            }
        } catch (SQLException e) { }
        return itens;
    }

    public Vacina buscarPorLoteCombo(String itemSelecionado) {
        if (itemSelecionado == null || itemSelecionado.equals("Selecione a vacina...")) return null;
        try {
            String nome = itemSelecionado.substring(0, itemSelecionado.indexOf(" (Lote:")).trim();
            String lote = itemSelecionado.substring(itemSelecionado.indexOf("Lote: ") + 6, itemSelecionado.indexOf(")")).trim();

            String sql = "SELECT * FROM vacinas WHERE nome_vacina = ? AND lote = ? AND qtd_disponivel > 0 LIMIT 1";
            try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nome);
                pstmt.setString(2, lote);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Vacina v = new Vacina();
                    v.setId(rs.getInt("id"));
                    v.setNomeVacina(rs.getString("nome_vacina"));
                    v.setLote(rs.getString("lote"));
                    v.setLaboratorio(rs.getString("laboratorio"));
                    v.setQtdDisponivel(rs.getInt("qtd_disponivel"));
                    String dataStr = rs.getString("validade"); if (dataStr != null && !dataStr.isEmpty()) v.setValidade(LocalDate.parse(dataStr));
                    v.setValorVenda(rs.getDouble("valor_venda"));
                    return v;
                }
            }
        } catch (Exception e) { System.out.println("Erro ao fatiar item da combobox: " + e.getMessage()); }
        return null;
    }
}