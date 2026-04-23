package br.sistema.repository;

import br.sistema.model.Endereco;
import br.sistema.model.Paciente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PacienteDAO {

    public PacienteDAO() { criarTabelaSeNaoExistir(); }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS pacientes ("
                + "id SERIAL PRIMARY KEY, nome VARCHAR(255) NOT NULL, cpf VARCHAR(20), data_nascimento VARCHAR(20), sexo VARCHAR(20), cartao_sus VARCHAR(50), telefone VARCHAR(20), telefone_2 VARCHAR(20), alergias TEXT, medico_encaminhador VARCHAR(255), "
                + "nome_responsavel VARCHAR(255), cpf_responsavel VARCHAR(20), "
                + "nome_responsavel_2 VARCHAR(255), cpf_responsavel_2 VARCHAR(20), "
                + "foto BYTEA, cep VARCHAR(20), rua VARCHAR(255), numero VARCHAR(20), complemento VARCHAR(255), bairro VARCHAR(100), cidade VARCHAR(100), uf VARCHAR(2), codigo_ibge VARCHAR(20))";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement()) { stmt.execute(sql); } catch (SQLException e) { }
    }

    public void salvar(Paciente p) {
        String sql = "INSERT INTO pacientes (nome, cpf, data_nascimento, sexo, cartao_sus, telefone, telefone_2, alergias, medico_encaminhador, nome_responsavel, cpf_responsavel, nome_responsavel_2, cpf_responsavel_2, foto, cep, rua, numero, complemento, bairro, cidade, uf, codigo_ibge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { preencherStatement(pstmt, p); pstmt.executeUpdate(); } catch (SQLException e) { }
    }

    public void atualizar(Paciente p) {
        String sql = "UPDATE pacientes SET nome=?, cpf=?, data_nascimento=?, sexo=?, cartao_sus=?, telefone=?, telefone_2=?, alergias=?, medico_encaminhador=?, nome_responsavel=?, cpf_responsavel=?, nome_responsavel_2=?, cpf_responsavel_2=?, foto=?, cep=?, rua=?, numero=?, complemento=?, bairro=?, cidade=?, uf=?, codigo_ibge=? WHERE id=?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { preencherStatement(pstmt, p); pstmt.setInt(23, p.getId()); pstmt.executeUpdate(); } catch (SQLException e) { }
    }

    private void preencherStatement(PreparedStatement pstmt, Paciente p) throws SQLException {
        pstmt.setString(1, p.getNome()); pstmt.setString(2, p.getCpf()); pstmt.setString(3, p.getDataNascimento() != null ? p.getDataNascimento().toString() : null); pstmt.setString(4, p.getSexo()); pstmt.setString(5, p.getCartaoSus()); pstmt.setString(6, p.getTelefone()); pstmt.setString(7, p.getTelefone2()); pstmt.setString(8, p.getAlergias()); pstmt.setString(9, p.getMedicoEncaminhador());
        pstmt.setString(10, p.getNomeResponsavel()); pstmt.setString(11, p.getCpfResponsavel());
        pstmt.setString(12, p.getNomeResponsavel2()); pstmt.setString(13, p.getCpfResponsavel2());
        pstmt.setBytes(14, p.getFoto());
        Endereco end = p.getEndereco();
        pstmt.setString(15, end != null ? end.getCep() : null); pstmt.setString(16, end != null ? end.getRua() : null); pstmt.setString(17, end != null ? end.getNumero() : null); pstmt.setString(18, end != null ? end.getComplemento() : null); pstmt.setString(19, end != null ? end.getBairro() : null); pstmt.setString(20, end != null ? end.getCidade() : null); pstmt.setString(21, end != null ? end.getUf() : null); pstmt.setString(22, end != null ? end.getCodigoIbge() : null);
    }

    public List<Paciente> listarTodos() {
        List<Paciente> lista = new ArrayList<>(); String sql = "SELECT * FROM pacientes ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapearPaciente(rs));
        } catch (SQLException e) { } return lista;
    }

    // =========================================================
    // LÓGICA BLINDADA: VERIFICA OS 3 CPFs (PRÓPRIO E DOS 2 RESPs)
    // =========================================================
    public List<Paciente> buscarFamiliaresPorCpfResponsavel(String cpfPrincipal, String cpfResp1, String cpfResp2, int idIgnorar) {
        List<Paciente> lista = new ArrayList<>();
        List<String> cpfsValidos = new ArrayList<>();

        // Adiciona à lista de busca apenas CPFs que estão preenchidos de verdade
        if (cpfPrincipal != null && !cpfPrincipal.trim().isEmpty() && !cpfPrincipal.equals("   .   .   -  ")) cpfsValidos.add(cpfPrincipal);
        if (cpfResp1 != null && !cpfResp1.trim().isEmpty() && !cpfResp1.equals("   .   .   -  ")) cpfsValidos.add(cpfResp1);
        if (cpfResp2 != null && !cpfResp2.trim().isEmpty() && !cpfResp2.equals("   .   .   -  ")) cpfsValidos.add(cpfResp2);

        if (cpfsValidos.isEmpty()) return lista;

        // Monta a query dinamicamente dependendo de quantos CPFs a família tem cadastrados
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM pacientes WHERE id != ? AND (");
        boolean first = true;
        for (int i = 0; i < cpfsValidos.size(); i++) {
            if (!first) queryBuilder.append(" OR ");
            queryBuilder.append("(cpf = ? OR cpf_responsavel = ? OR cpf_responsavel_2 = ?)");
            first = false;
        }
        queryBuilder.append(")");

        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
            pstmt.setInt(1, idIgnorar);
            int paramIndex = 2;
            for (String cpf : cpfsValidos) {
                pstmt.setString(paramIndex++, cpf);
                pstmt.setString(paramIndex++, cpf);
                pstmt.setString(paramIndex++, cpf);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) lista.add(mapearPaciente(rs));
        } catch (SQLException e) { System.out.println("Erro ao buscar familiares: " + e.getMessage()); }

        return lista;
    }

    private Paciente mapearPaciente(ResultSet rs) throws SQLException {
        Paciente p = new Paciente(); p.setId(rs.getInt("id")); p.setNome(rs.getString("nome")); p.setCpf(rs.getString("cpf"));
        String dataStr = rs.getString("data_nascimento"); if (dataStr != null && !dataStr.isEmpty()) p.setDataNascimento(LocalDate.parse(dataStr));
        p.setSexo(rs.getString("sexo")); p.setCartaoSus(rs.getString("cartao_sus")); p.setTelefone(rs.getString("telefone")); p.setTelefone2(rs.getString("telefone_2")); p.setAlergias(rs.getString("alergias")); p.setMedicoEncaminhador(rs.getString("medico_encaminhador"));
        p.setNomeResponsavel(rs.getString("nome_responsavel")); p.setCpfResponsavel(rs.getString("cpf_responsavel"));
        p.setNomeResponsavel2(rs.getString("nome_responsavel_2")); p.setCpfResponsavel2(rs.getString("cpf_responsavel_2"));
        p.setFoto(rs.getBytes("foto"));
        p.setEndereco(new Endereco(rs.getString("cep"), rs.getString("rua"), rs.getString("numero"), rs.getString("complemento"), rs.getString("bairro"), rs.getString("cidade"), rs.getString("uf"), rs.getString("codigo_ibge")));
        return p;
    }

    public void excluir(int id) {
        String sql = "DELETE FROM pacientes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao excluir: " + e.getMessage()); }
    }
}