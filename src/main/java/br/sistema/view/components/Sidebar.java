package br.sistema.view.components;

import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.panels.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Sidebar extends JPanel {
    private TelaPrincipal frame;

    public Sidebar(TelaPrincipal frame) {
        this.frame = frame;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(280, 0));

        // Logo
        try {
            ImageIcon iconLogo = new ImageIcon(getClass().getResource("/icons/logo_karol.png"));
            Image img = iconLogo.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            JLabel lblLogo = new JLabel(new ImageIcon(img));
            lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblLogo.setBorder(new EmptyBorder(40, 0, 50, 0));
            add(lblLogo);
        } catch (Exception e) {
            JLabel fall = new JLabel("LOGO TIA KAROL");
            fall.setFont(new Font("Segoe UI", Font.BOLD, 20));
            fall.setForeground(Color.WHITE);
            fall.setAlignmentX(Component.CENTER_ALIGNMENT);
            fall.setBorder(new EmptyBorder(60, 0, 60, 0));
            add(fall);
        }

        // Mapeamento dos SVGs e criação dos botões
        add(criarBotaoMenu("Home", "casa.svg"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Painel de Aplicações", "vacinas.svg"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Pacientes", "member-list.svg"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Estoque", "frigorifico.svg"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Financeiro", "usd-circle.svg"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Configurações", "configuracoes.svg")); // Ou engrenagem.svg
    }

    private JButton criarBotaoMenu(String texto, String arquivoSvg) {
        JButton btn = new JButton(texto);
        btn.setMaximumSize(new Dimension(260, 55));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(255, 255, 255, 0));
        btn.setBorder(new EmptyBorder(0, 30, 0, 0));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);

        // Carrega o SVG e aplica cor Branca padrão
        FlatSVGIcon iconOriginal = new FlatSVGIcon("icons/" + arquivoSvg, 24, 24);
        if (iconOriginal != null) {
            iconOriginal.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
            btn.setIcon(iconOriginal);
        }
        btn.setIconTextGap(20);

        // Efeito visual de Hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(248, 178, 161, 121));
                btn.setForeground(Cores.VERDE_AQUA);
                if (iconOriginal != null) iconOriginal.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Cores.VERDE_AQUA));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setForeground(Color.WHITE);
                btn.setBackground(new Color(255, 255, 255, 0));
                if (iconOriginal != null) iconOriginal.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
            }
        });

        // ROTEADOR DE TELAS (Aqui está o segredo!)
        btn.addActionListener(e -> {
            if (texto.equals("Home")) {
                frame.trocarTelaCentral(new PainelDashboard(frame));
            } else if (texto.equals("Painel de Aplicações")) {
                frame.trocarTelaCentral(new PainelAplicacoes(frame));
            } else if (texto.equals("Pacientes")) {
                frame.trocarTelaCentral(new PainelPacientes(frame));
            } else if (texto.equals("Estoque")) {
                frame.trocarTelaCentral(new PainelEstoque(frame));
            } else if (texto.equals("Financeiro")) {
                frame.trocarTelaCentral(new PainelFinanceiro(frame));
            } else if (texto.equals("Configurações")) {
                frame.trocarTelaCentral(new PainelConfiguracoes(frame));
            }
        });

        return btn; // Faltava retornar o botão para o painel!
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor( new Color(248, 178, 161));
        g2.fillRect(0, 0, getWidth(), getHeight());

        GradientPaint shadow = new GradientPaint(getWidth() - 10, 0, new Color(0,0,0,0), getWidth(), 0, new Color(0,0,0,25));
        g2.setPaint(shadow);
        g2.fillRect(getWidth() - 10, 0, 10, getHeight());

        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        g2.dispose();
    }
}