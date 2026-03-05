package br.sistema.model;

public class Aplicacao {
    private int id;
    private String paciente;
    private String dataHora;
    private String vacina;
    private String status; // "Agendado" ou "Aplicado"
    private String formaPagamento;
    private double valor;

    // Construtor vazio (Necessário para o DAO)
    public Aplicacao() {}

    // Construtor completo (Facilita na hora de salvar)
    public Aplicacao(String paciente, String dataHora, String vacina, String status, String formaPagamento, double valor) {
        this.paciente = paciente;
        this.dataHora = dataHora;
        this.vacina = vacina;
        this.status = status;
        this.formaPagamento = formaPagamento;
        this.valor = valor;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPaciente() { return paciente; }
    public void setPaciente(String paciente) { this.paciente = paciente; }

    public String getDataHora() { return dataHora; }
    public void setDataHora(String dataHora) { this.dataHora = dataHora; }

    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}