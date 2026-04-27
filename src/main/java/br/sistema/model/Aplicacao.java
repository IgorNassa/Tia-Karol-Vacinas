package br.sistema.model;

import java.time.LocalDateTime;

public class Aplicacao {
    private int id;
    private Paciente paciente;
    private Vacina vacina;
    private LocalDateTime dataHora;
    private String status;
    private String formaPagamento;
    private double valor; // Valor Final (Líquido)
    private double valorBruto;
    private double desconto;
    private String distribuidor;
    private String numeroNota;
    private String reacoes;
    private String observacoesAdicionais;

    public Aplicacao() {}

    public Aplicacao(Paciente paciente, Vacina vacina, LocalDateTime dataHora, String status, String formaPagamento, double valor) {
        this.paciente = paciente;
        this.vacina = vacina;
        this.dataHora = dataHora;
        this.status = status;
        this.formaPagamento = formaPagamento;
        this.valor = valor;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Vacina getVacina() { return vacina; }
    public void setVacina(Vacina vacina) { this.vacina = vacina; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public double getValorBruto() { return valorBruto; }
    public void setValorBruto(double valorBruto) { this.valorBruto = valorBruto; }
    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; }
    public String getDistribuidor() { return distribuidor; }
    public void setDistribuidor(String distribuidor) { this.distribuidor = distribuidor; }
    public String getNumeroNota() { return numeroNota; }
    public void setNumeroNota(String numeroNota) { this.numeroNota = numeroNota; }
    public String getReacoes() { return reacoes; }
    public void setReacoes(String reacoes) { this.reacoes = reacoes; }
    public String getObservacoesAdicionais() { return observacoesAdicionais; }
    public void setObservacoesAdicionais(String observacoesAdicionais) { this.observacoesAdicionais = observacoesAdicionais; }
}