package br.sistema.model;

import java.time.LocalDate;

public class LancamentoOutros {
    private int id;
    private String nome;
    private String tipo; // "Entrada avulsa" ou "Saída avulsa"
    private double valor;
    private LocalDate dataLancamento;

    public LancamentoOutros() {}

    public LancamentoOutros(String nome, String tipo, double valor, LocalDate dataLancamento) {
        this.nome = nome;
        this.tipo = tipo;
        this.valor = valor;
        this.dataLancamento = dataLancamento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public LocalDate getDataLancamento() { return dataLancamento; }
    public void setDataLancamento(LocalDate dataLancamento) { this.dataLancamento = dataLancamento; }
}