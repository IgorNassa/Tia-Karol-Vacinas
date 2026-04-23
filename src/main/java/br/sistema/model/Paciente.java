package br.sistema.model;

import java.time.LocalDate;
import java.time.Period;

public class Paciente {
    private int id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String sexo;
    private String cartaoSus;
    private String telefone;
    private String telefone2;
    private String alergias;
    private String medicoEncaminhador;

    // Responsáveis Legais
    private String nomeResponsavel;
    private String cpfResponsavel;
    private String nomeResponsavel2;
    private String cpfResponsavel2;

    private byte[] foto;
    private Endereco endereco;

    public Paciente() {}

    public Paciente(String nome, String cpf, LocalDate dataNascimento, String sexo, String cartaoSus,
                    String telefone, String telefone2, String alergias, String medicoEncaminhador,
                    String nomeResponsavel, String cpfResponsavel, String nomeResponsavel2, String cpfResponsavel2,
                    byte[] foto, Endereco endereco) {
        this.nome = nome; this.cpf = cpf; this.dataNascimento = dataNascimento;
        this.sexo = sexo; this.cartaoSus = cartaoSus; this.telefone = telefone;
        this.telefone2 = telefone2; this.alergias = alergias; this.medicoEncaminhador = medicoEncaminhador;
        this.nomeResponsavel = nomeResponsavel; this.cpfResponsavel = cpfResponsavel;
        this.nomeResponsavel2 = nomeResponsavel2; this.cpfResponsavel2 = cpfResponsavel2;
        this.foto = foto; this.endereco = endereco;
    }

    // NOVO: Cálculo Automático de Idade
    public String getIdadeCalculada() {
        if (this.dataNascimento == null) return "N/I";
        Period p = Period.between(this.dataNascimento, LocalDate.now());
        if (p.getYears() > 0) return p.getYears() + " anos";
        if (p.getMonths() > 0) return p.getMonths() + " meses";
        return p.getDays() + " dias";
    }

    // Getters e Setters Padrões
    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getNome() { return nome; } public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; } public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; } public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getSexo() { return sexo; } public void setSexo(String sexo) { this.sexo = sexo; }
    public String getCartaoSus() { return cartaoSus; } public void setCartaoSus(String cartaoSus) { this.cartaoSus = cartaoSus; }
    public String getTelefone() { return telefone; } public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getTelefone2() { return telefone2; } public void setTelefone2(String telefone2) { this.telefone2 = telefone2; }
    public String getAlergias() { return alergias; } public void setAlergias(String alergias) { this.alergias = alergias; }
    public String getMedicoEncaminhador() { return medicoEncaminhador; } public void setMedicoEncaminhador(String medicoEncaminhador) { this.medicoEncaminhador = medicoEncaminhador; }
    public String getNomeResponsavel() { return nomeResponsavel; } public void setNomeResponsavel(String nomeResponsavel) { this.nomeResponsavel = nomeResponsavel; }
    public String getCpfResponsavel() { return cpfResponsavel; } public void setCpfResponsavel(String cpfResponsavel) { this.cpfResponsavel = cpfResponsavel; }
    public String getNomeResponsavel2() { return nomeResponsavel2; } public void setNomeResponsavel2(String nomeResponsavel2) { this.nomeResponsavel2 = nomeResponsavel2; }
    public String getCpfResponsavel2() { return cpfResponsavel2; } public void setCpfResponsavel2(String cpfResponsavel2) { this.cpfResponsavel2 = cpfResponsavel2; }
    public byte[] getFoto() { return foto; } public void setFoto(byte[] foto) { this.foto = foto; }
    public Endereco getEndereco() { return endereco; } public void setEndereco(Endereco endereco) { this.endereco = endereco; }
}