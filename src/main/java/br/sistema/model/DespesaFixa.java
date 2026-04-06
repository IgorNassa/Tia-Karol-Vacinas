package br.sistema.model;

import java.time.LocalDate;

public class DespesaFixa {
    private int id;
    private String nome;
    private double valorPadrao;
    private boolean valorVariavel;
    private int diaVencimento;
    private double ultimoValorPago;
    private LocalDate dataUltimoPagamento;

    public DespesaFixa() {}

    public DespesaFixa(String nome, double valorPadrao, boolean valorVariavel, int diaVencimento) {
        this.nome = nome;
        this.valorPadrao = valorPadrao;
        this.valorVariavel = valorVariavel;
        this.diaVencimento = diaVencimento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public double getValorPadrao() { return valorPadrao; }
    public void setValorPadrao(double valorPadrao) { this.valorPadrao = valorPadrao; }
    public boolean isValorVariavel() { return valorVariavel; }
    public void setValorVariavel(boolean valorVariavel) { this.valorVariavel = valorVariavel; }
    public int getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(int diaVencimento) { this.diaVencimento = diaVencimento; }
    public double getUltimoValorPago() { return ultimoValorPago; }
    public void setUltimoValorPago(double ultimoValorPago) { this.ultimoValorPago = ultimoValorPago; }
    public LocalDate getDataUltimoPagamento() { return dataUltimoPagamento; }
    public void setDataUltimoPagamento(LocalDate dataUltimoPagamento) { this.dataUltimoPagamento = dataUltimoPagamento; }
}