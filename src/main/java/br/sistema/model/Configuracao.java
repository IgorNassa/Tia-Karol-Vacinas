package br.sistema.model;

public class Configuracao {
    private String nomeClinica;
    private String cnpj;
    private String telefone;
    private String endereco;
    private byte[] logo;

    public Configuracao() {}

    public Configuracao(String nomeClinica, String cnpj, String telefone, String endereco, byte[] logo) {
        this.nomeClinica = nomeClinica;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.endereco = endereco;
        this.logo = logo;
    }

    // Getters e Setters
    public String getNomeClinica() { return nomeClinica; } public void setNomeClinica(String nomeClinica) { this.nomeClinica = nomeClinica; }
    public String getCnpj() { return cnpj; } public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getTelefone() { return telefone; } public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; } public void setEndereco(String endereco) { this.endereco = endereco; }
    public byte[] getLogo() { return logo; } public void setLogo(byte[] logo) { this.logo = logo; }
}