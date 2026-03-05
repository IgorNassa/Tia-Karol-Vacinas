package br.sistema.repository;

import br.sistema.model.Estoque;
import br.sistema.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {
    public void salvar(Estoque estoque) {
        String sql = "INSERT INTO Estoque (nome_vacina, lote, validade, laboratorio, qtd_total, qtd_disponivel, valor_compra, valor_venda) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estoque.getNomeVacina());
            stmt.setString(2, estoque.getLote());
            stmt.setString(3, estoque.getValidade().toString());
            stmt.setString(4, estoque.getLaboratorio());
            stmt.setInt(5, estoque.getQtdTotal());
            stmt.setInt(6, estoque.getQtdDisponivel());
            stmt.setDouble(7, estoque.getValorCompra());
            stmt.setDouble(8, estoque.getValorVenda());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}