package br.sistema.model;

import java.time.LocalDate;

public class Estoque {
    private int id;
    private String nomeVacina;
    private String lote;
    private LocalDate validade;
    private String laboratorio;
    private int qtdTotal;
    private int qtdDisponivel;
    private double valorCompra;
    private double valorVenda;

    public Estoque() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomeVacina() { return nomeVacina; }
    public void setNomeVacina(String nomeVacina) { this.nomeVacina = nomeVacina; }
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
}