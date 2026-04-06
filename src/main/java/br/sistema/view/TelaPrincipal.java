package br.sistema.view;

import br.sistema.util.Cores;
import br.sistema.view.components.Sidebar;
import br.sistema.view.panels.PainelDashboard;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class TelaPrincipal extends JFrame {
    private JPanel containerCentral;

    public TelaPrincipal() {
        setTitle("VacinControl - Tia Karol Vacinas");
        setSize(1350, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel containerGeral = new JPanel(new BorderLayout());

        // Adiciona a Sidebar modular
        containerGeral.add(new Sidebar(this), BorderLayout.WEST);

        // Container dinâmico com o fundo Gradiente
        containerCentral = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(230, 235, 240), getWidth(), getHeight(), new Color(245, 235, 240));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        containerGeral.add(containerCentral, BorderLayout.CENTER);

        add(containerGeral);

        // Inicia abrindo o Dashboard
        trocarTelaCentral(new PainelDashboard(this));
    }

    // MÉTODO MÁGICO DE NAVEGAÇÃO
    public void trocarTelaCentral(JPanel novaTela) {
        containerCentral.removeAll();
        containerCentral.add(novaTela, BorderLayout.CENTER);
        containerCentral.revalidate();
        containerCentral.repaint();
    }

    public static void iniciar() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 20);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextComponent.arc", 15);

            // --- UX: O EFEITO DE HOVER DA TABELA (Pílula e Suave) ---
            UIManager.put("Table.selectionBackground", Cores.VERDE_AQUA);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.hoverBackground", new Color(230, 240, 240)); // Verde-água quase transparente no hover

            UIManager.put("Table.selectionInsets", new Insets(3, 10, 3, 10)); // Impede de grudar na borda
            UIManager.put("Table.selectionArc", 15); // Arredonda a pílula

            UIManager.put("PopupMenu.background", Color.WHITE);
            UIManager.put("MenuItem.selectionBackground", new Color(242, 245, 248));

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            TelaPrincipal tela = new TelaPrincipal();

            // 1. Configura a Quebra de Caixa ao Clicar no X
            br.sistema.util.GerenciadorCaixa.configurarFechamento(tela);

            // 2. Exibe a tela de fundo
            tela.setVisible(true);

            // 3. Pula o Abertura de Caixa
            br.sistema.util.GerenciadorCaixa.iniciarAbertura(tela);

            // 4. Pula o Modal de Alertas Críticos do Estoque
            br.sistema.view.components.ModalAlertasEstoque.verificarEExibir(tela);
        });
    }
}
