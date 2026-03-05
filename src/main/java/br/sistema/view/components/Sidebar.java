package br.sistema.view.components;

import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.panels.PainelDashboard;
import br.sistema.view.panels.PainelFormulario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Sidebar extends JPanel {
    private TelaPrincipal frame; // Referência à janela mãe para trocar as telas

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

        add(criarBotaoMenu("Dashboard", "icon_dashboard.png"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Pacientes", "icon_paciente.png"));
        add(Box.createVerticalStrut(5));
        add(criarBotaoMenu("Estoque", "icon_estoque.png"));
    }

    private JButton criarBotaoMenu(String texto, String icone) {
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

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/" + icone));
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            btn.setIcon(new ImageIcon(new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB)));
        }
        btn.setIconTextGap(20);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(241, 160, 140, 105));
                btn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setForeground(Color.WHITE);
                btn.setBackground(new Color(255, 255, 255, 0));
            }
        });

        btn.addActionListener(e -> {
            if (texto.equals("Dashboard")) frame.trocarTelaCentral(new PainelDashboard(frame));
            // Outras telas no futuro
        });

        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Cores.ROSA_KAROL);
        g2.fillRect(0, 0, getWidth(), getHeight());

        GradientPaint shadow = new GradientPaint(getWidth() - 10, 0, new Color(0,0,0,0), getWidth(), 0, new Color(0,0,0,25));
        g2.setPaint(shadow);
        g2.fillRect(getWidth() - 10, 0, 10, getHeight());

        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        g2.dispose();
    }
}