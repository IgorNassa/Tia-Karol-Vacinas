package br.sistema.view.components;

import br.sistema.model.Vacina;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ModalAlertasEstoque {

    /**
     * Verifica o banco de dados. Se houver problemas, exibe o modal chamativo.
     * Deve ser chamado na sua TelaPrincipal após ela se tornar visível.
     */
    public static void verificarEExibir(JFrame framePai) {
        VacinaDAO dao = new VacinaDAO();
        List<Vacina> todas = dao.listarTodas();

        List<Vacina> baixoEstoque = new ArrayList<>();
        List<Vacina> vencendo = new ArrayList<>();

        LocalDate hoje = LocalDate.now();
        LocalDate limite30Dias = hoje.plusDays(30);

        // Varredura de Inteligência
        for (Vacina v : todas) {
            // Se tem 5 ou menos na geladeira (mas não está zerado)
            if (v.getQtdDisponivel() > 0 && v.getQtdDisponivel() <= 5) {
                baixoEstoque.add(v);
            }

            // Se vai vencer nos próximos 30 dias (ou se já venceu) e ainda tem em estoque
            if (v.getQtdDisponivel() > 0 && v.getValidade() != null && !v.getValidade().isAfter(limite30Dias)) {
                vencendo.add(v);
            }
        }

        // Se estiver tudo perfeito, não incomoda o usuário
        if (baixoEstoque.isEmpty() && vencendo.isEmpty()) return;

        // --- CONSTRUÇÃO DO MODAL CHAMATIVO ---
        JDialog dialog = new JDialog(framePai, "🚨 Alertas Críticos do Sistema", true);
        dialog.setSize(650, 550);
        dialog.setLocationRelativeTo(framePai);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel pnlConteudo = new JPanel(new BorderLayout(0, 20));
        pnlConteudo.setBackground(Color.WHITE);
        pnlConteudo.setBorder(new EmptyBorder(30, 40, 20, 40));

        // Cabeçalho
        JLabel lblTitulo = new JLabel("Atenção Gerencial Necessária");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(220, 53, 69)); // Vermelho Alerta
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        pnlConteudo.add(lblTitulo, BorderLayout.NORTH);

        // Corpo HTML para listas bonitas
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; font-size: 14px;'>");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Sessão de Vencimentos (A mais crítica financeiramente)
        if (!vencendo.isEmpty()) {
            html.append("<h2 style='color: #c0392b; border-bottom: 1px solid #ecc;'>⏳ Vacinas Vencendo (Menos de 30 dias)</h2>");
            html.append("<ul>");
            for (Vacina v : vencendo) {
                long diasRestantes = ChronoUnit.DAYS.between(hoje, v.getValidade());
                String textoDias = diasRestantes < 0 ? "<b>JÁ VENCEU!</b>" : "Faltam " + diasRestantes + " dias";

                html.append("<li><b>").append(v.getNomeVacina()).append("</b> (Lote: ").append(v.getLote()).append(") ");
                html.append("- Vence em: ").append(v.getValidade().format(fmt)).append(" <span style='color: red;'>[").append(textoDias).append("]</span></li>");
            }
            html.append("</ul><br>");
        }

        // Sessão de Baixo Estoque
        if (!baixoEstoque.isEmpty()) {
            html.append("<h2 style='color: #d35400; border-bottom: 1px solid #fbeee6;'>📉 Estoque Baixo (5 ou menos)</h2>");
            html.append("<ul>");
            for (Vacina v : baixoEstoque) {
                html.append("<li><b>").append(v.getNomeVacina()).append("</b> (Lote: ").append(v.getLote()).append(") ");
                html.append("- Apenas <b>").append(v.getQtdDisponivel()).append(" doses</b> na geladeira.</li>");
            }
            html.append("</ul>");
        }

        html.append("</body></html>");

        JLabel lblListas = new JLabel(html.toString());
        lblListas.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane scroll = new JScrollPane(lblListas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        pnlConteudo.add(scroll, BorderLayout.CENTER);

        // Botão de Ciente
        JButton btnCiente = new JButton("Estou Ciente - Fechar Alertas");
        btnCiente.setBackground(Cores.CINZA_GRAFITE);
        btnCiente.setForeground(Color.WHITE);
        btnCiente.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCiente.setPreferredSize(new Dimension(0, 50));
        btnCiente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCiente.addActionListener(e -> dialog.dispose());

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setBackground(Color.WHITE);
        pnlFooter.add(btnCiente, BorderLayout.CENTER);
        pnlConteudo.add(pnlFooter, BorderLayout.SOUTH);

        dialog.add(pnlConteudo);
        dialog.setVisible(true);
    }
}