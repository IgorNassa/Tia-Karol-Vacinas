package br.sistema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Vacina {
    private int id;
    private String nomeVacina;
    private String tipo; // NOVO: Atenuada, Inativada, Conjugada, etc.
    private String lote;
    private LocalDate validade;
    private String laboratorio;
    private int qtdTotal;
    private int qtdDisponivel;
    private double valorCompra;
    private double valorVenda;
    private String observacoes; // NOVO: Para detalhes da caixa ou transporte
    private LocalDateTime dataCadastro; // NOVO: Para rastrear a entrada do lote no sistema

    public Vacina() {}

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomeVacina() { return nomeVacina; }

    // INTELIGÊNCIA DE FORMATAÇÃO: Primeira letra sempre maiúscula, o resto minúscula.
    public void setNomeVacina(String nomeVacina) {
        if (nomeVacina != null && !nomeVacina.trim().isEmpty()) {
            String str = nomeVacina.trim().toLowerCase();
            this.nomeVacina = str.substring(0, 1).toUpperCase() + str.substring(1);
        } else {
            this.nomeVacina = nomeVacina;
        }
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }

    public String getLaboratorio() { return laboratorio; }
    public void setLaboratorio(String laboratorio) { this.laboratorio = laboratorio; }

    public int getQtdTotal() { return qtdTotal; }
    public void setQtdTotal(int qtdTotal) { this.qtdTotal = qtdTotal; }

    public int getQtdDisponivel() { return qtdDisponivel; }
    public void setQtdDisponivel(int qtdDisponivel) { this.qtdDisponivel = qtdDisponivel; }

    public double getValorCompra() { return valorCompra; }
    public void setValorCompra(double valorCompra) { this.valorCompra = valorCompra; }

    public double getValorVenda() { return valorVenda; }
    public void setValorVenda(double valorVenda) { this.valorVenda = valorVenda; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}