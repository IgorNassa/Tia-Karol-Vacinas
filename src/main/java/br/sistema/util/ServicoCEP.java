package br.sistema.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServicoCEP {

    // Retorna um array de Strings: [0]=Rua, [1]=Bairro, [2]=Cidade, [3]=UF, [4]=IBGE
    public static String[] buscarCep(String cep) {
        cep = cep.replaceAll("[^0-9]", "");
        if (cep.length() != 8) return null;

        try {
            URL url = new URL("https://viacep.com.br/ws/" + cep + "/json/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) response.append(output);

            String json = response.toString();

            // Verificação simples se o CEP não existe
            if (json.contains("\"erro\": true")) return null;

            // Extração "Raiz" do JSON para não precisar instalar bibliotecas externas agora
            String logradouro = extrairValorJSON(json, "logradouro");
            String bairro = extrairValorJSON(json, "bairro");
            String localidade = extrairValorJSON(json, "localidade");
            String uf = extrairValorJSON(json, "uf");
            String ibge = extrairValorJSON(json, "ibge");

            return new String[]{logradouro, bairro, localidade, uf, ibge};

        } catch (Exception e) {
            return null;
        }
    }

    private static String extrairValorJSON(String json, String chave) {
        String busca = "\"" + chave + "\": \"";
        int inicio = json.indexOf(busca);
        if (inicio == -1) return "";
        inicio += busca.length();
        int fim = json.indexOf("\"", inicio);
        return json.substring(inicio, fim);
    }
}