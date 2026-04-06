package br.sistema.model;

public class Endereco {
    private String cep;
    private String rua;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String codigoIbge;

    public Endereco() {}

    public Endereco(String cep, String rua, String numero, String complemento, String bairro, String cidade, String uf, String codigoIbge) {
        this.cep = cep;
        this.rua = rua;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.codigoIbge = codigoIbge;
    }

    // Getters e Setters
    public String getCep() { return cep; } public void setCep(String cep) { this.cep = cep; }
    public String getRua() { return rua; } public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; } public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; } public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; } public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; } public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; } public void setUf(String uf) { this.uf = uf; }
    public String getCodigoIbge() { return codigoIbge; } public void setCodigoIbge(String codigoIbge) { this.codigoIbge = codigoIbge; }
}