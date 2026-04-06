package br.sistema.view.panels;

import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.prefs.Preferences;

public class PainelConfiguracoes extends JPanel {
    private TelaPrincipal frame;

    // Repositório nativo para guardar as configurações (Nome da clinica, etc)
    private Preferences prefs = Preferences.userNodeForPackage(PainelConfiguracoes.class);

    // Campos de Texto
    private JTextField txtNomeClinica, txtCNPJ, txtEndereco, txtTelefone, txtEmail;

    // Componentes do Logo
    private JLabel lblLogoPreview;
    private String caminhoLogoAtual = null;

    // Tamanho padrão para os inputs (Evita esticar)
    private final Dimension fieldSize = new Dimension(0, 42);

    public PainelConfiguracoes(TelaPrincipal frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(30, 40, 30, 40));

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout());
        cardVidro.setBorder(new EmptyBorder(35, 45, 35, 45));

        // HEADER
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(0, 0, 30, 0));
        JLabel titulo = new JLabel("Configurações da Clínica"); titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32)); titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JButton btnSalvarTopo = criarBotaoElegante(" Salvar Alterações", Cores.VERDE_AQUA, "disco.svg");
        btnSalvarTopo.addActionListener(e -> salvarConfiguracoes());
        header.add(btnSalvarTopo, BorderLayout.EAST);

        // --- CORPO (Grid 3 Colunas) ---
        JPanel gridCorpo = new JPanel(new GridLayout(1, 3, 35, 0)); gridCorpo.setOpaque(false);

        gridCorpo.add(montarColunaMarca());    // Coluna 1
        gridCorpo.add(montarColunaDados());    // Coluna 2
        gridCorpo.add(montarColunaSistema());  // Coluna 3

        // TRUQUE DE LAYOUT: Coloca o grid no NORTH de um BorderLayout para alinhar tudo ao TOPO
        JPanel pnlAlinhamentoTop = new JPanel(new BorderLayout()); pnlAlinhamentoTop.setOpaque(false);
        pnlAlinhamentoTop.add(gridCorpo, BorderLayout.NORTH);

        cardVidro.add(header, BorderLayout.NORTH);
        cardVidro.add(pnlAlinhamentoTop, BorderLayout.CENTER); add(cardVidro, BorderLayout.CENTER);

        carregarConfiguracoes();
    }

    // =========================================================
    // COLUNA 1: SUA MARCA
    // =========================================================
    private JPanel montarColunaMarca() {
        JPanel wrapper = criarCardSeccao("Sua Marca"); wrapper.setLayout(new BorderLayout(0, 20));

        // Conteúdo Interno com BoxLayout para empilhar no topo
        JPanel inner = new JPanel(); inner.setOpaque(false); inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel lblInfo = new JLabel("<html><center>Este logo aparecerá nos<br>relatórios e impressões.</center></html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblInfo.setForeground(Cores.CINZA_LABEL); lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lblInfo); inner.add(Box.createVerticalStrut(15));

        // CONTAINER DO PREVIEW (Moldura)
        JPanel pnlPreview = new JPanel(new BorderLayout());
        pnlPreview.setBackground(new Color(248, 250, 252));
        pnlPreview.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 235, 240), 1, true), new EmptyBorder(15, 15, 15, 15)));
        pnlPreview.setMaximumSize(new Dimension(200, 200)); // Tamanho fixo da moldura
        pnlPreview.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblLogoPreview = new JLabel(); lblLogoPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogoPreview.setPreferredSize(new Dimension(170, 170));

        // Define o logo padrão Karol
        setDefaultLogo();

        pnlPreview.add(lblLogoPreview, BorderLayout.CENTER);
        inner.add(pnlPreview); inner.add(Box.createVerticalStrut(20));

        // Botões de Ação do Logo
        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); pnlAcoes.setOpaque(false); pnlAcoes.setAlignmentX(Component.CENTER_ALIGNMENT);
        // AJUSTE: Ícone alterado para lapis-de-blog.svg
        JButton btnUpload = criarBotaoElegante("Alterar", Cores.CINZA_GRAFITE, "lapis-de-blog.svg");
        btnUpload.setPreferredSize(new Dimension(120, 42)); // Menor para caber lado a lado

        JButton btnRemove = criarBotaoElegante("Remover", new Color(220, 53, 69), "trash.svg");
        btnRemove.setPreferredSize(new Dimension(120, 42));

        btnUpload.addActionListener(e -> escolherImagem());
        btnRemove.addActionListener(e -> removerImagem());

        pnlAcoes.add(btnUpload); pnlAcoes.add(btnRemove);
        inner.add(pnlAcoes);

        wrapper.add(inner, BorderLayout.CENTER); return wrapper;
    }

    // =========================================================
    // COLUNA 2: DADOS DA CLÍNICA (Inputs alinhados ao topo)
    // =========================================================
    private JPanel montarColunaDados() {
        JPanel wrapper = criarCardSeccao("Informações Básicas"); wrapper.setLayout(new BorderLayout());

        // BoxLayout força os componentes a ficarem no topo e não esticarem verticalmente
        JPanel inner = new JPanel(); inner.setOpaque(false); inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        txtNomeClinica = criarCampoTexto("Ex: Karol Vacinas");
        txtCNPJ = criarCampoTexto("00.000.000/0001-00");
        txtEndereco = criarCampoTexto("Rua X, 123 - Centro");

        inner.add(montarBlocoInput("NOME DA CLÍNICA (RAZÃO SOCIAL)", txtNomeClinica)); inner.add(Box.createVerticalStrut(20));
        inner.add(montarBlocoInput("CNPJ", txtCNPJ)); inner.add(Box.createVerticalStrut(20));
        inner.add(montarBlocoInput("ENDEREÇO COMPLETO", txtEndereco));

        wrapper.add(inner, BorderLayout.NORTH); return wrapper;
    }

    // =========================================================
    // COLUNA 3: CONTATOS (Sem Backup, alinhados ao topo)
    // =========================================================
    private JPanel montarColunaSistema() {
        JPanel wrapper = criarCardSeccao("Contato"); wrapper.setLayout(new BorderLayout());

        JPanel inner = new JPanel(); inner.setOpaque(false); inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        txtTelefone = criarCampoTexto("(00) 00000-0000");
        txtEmail = criarCampoTexto("contato@clinica.com.br");

        inner.add(montarBlocoInput("TELEFONE PRINCIPAL", txtTelefone)); inner.add(Box.createVerticalStrut(20));
        inner.add(montarBlocoInput("EMAIL DE ATENDIMENTO", txtEmail));

        wrapper.add(inner, BorderLayout.NORTH); return wrapper;
    }

    // =========================================================
    // LÓGICA DE IMAGEM E SALVAMENTO
    // =========================================================

    private void setDefaultLogo() {
        try {
            // Tenta carregar logoKarol.png da pasta icons
            URL imgUrl = getClass().getResource("/icons/logoKarol.png");
            if(imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                Image img = icon.getImage().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                lblLogoPreview.setIcon(new ImageIcon(img));
                lblLogoPreview.setText(null);
            } else { setFallbackText(); }
        } catch(Exception e){ setFallbackText(); }
    }

    private void setFallbackText() { lblLogoPreview.setIcon(null); lblLogoPreview.setText("Logo Karol"); lblLogoPreview.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblLogoPreview.setForeground(new Color(200, 200, 200)); }

    private void carregarConfiguracoes() {
        txtNomeClinica.setText(prefs.get("clinica_nome", "Sua Clínica"));
        txtCNPJ.setText(prefs.get("clinica_cnpj", ""));
        txtEndereco.setText(prefs.get("clinica_endereco", ""));
        txtTelefone.setText(prefs.get("clinica_telefone", ""));
        txtEmail.setText(prefs.get("clinica_email", ""));

        caminhoLogoAtual = prefs.get("clinica_logo_path", null);
        if (caminhoLogoAtual != null && new File(caminhoLogoAtual).exists()) {
            try {
                ImageIcon icon = new ImageIcon(caminhoLogoAtual);
                Image img = icon.getImage().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                lblLogoPreview.setIcon(new ImageIcon(img));
                lblLogoPreview.setText(null);
            } catch(Exception e) { setDefaultLogo(); }
        } else { setDefaultLogo(); } // Se não houver config, carrega a padrão Karol
    }

    private void salvarConfiguracoes() {
        prefs.put("clinica_nome", txtNomeClinica.getText());
        prefs.put("clinica_cnpj", txtCNPJ.getText());
        prefs.put("clinica_endereco", txtEndereco.getText());
        prefs.put("clinica_telefone", txtTelefone.getText());
        prefs.put("clinica_email", txtEmail.getText());
        if(caminhoLogoAtual != null) prefs.put("clinica_logo_path", caminhoLogoAtual);
        else prefs.remove("clinica_logo_path");
        JOptionPane.showMessageDialog(frame, "Configurações atualizadas com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // CORREÇÃO DO TRAVAMENTO: USANDO FILEDIALOG NATIVO
    // =========================================================
    private void escolherImagem() {
        // O FileDialog chama a janela nativa do Windows, que é instantânea e não trava!
        FileDialog fd = new FileDialog(frame, "Selecione a Logo da Clínica", FileDialog.LOAD);
        fd.setFile("*.png;*.jpg;*.jpeg");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();

        if (dir != null && file != null) {
            caminhoLogoAtual = dir + file;
            ImageIcon icon = new ImageIcon(caminhoLogoAtual);
            Image img = icon.getImage().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
            lblLogoPreview.setIcon(new ImageIcon(img));
            lblLogoPreview.setText(null);
        }
    }

    private void removerImagem() {
        caminhoLogoAtual = null;
        setDefaultLogo(); // Volta para a padrão Karol
    }

    // =========================================================
    // COMPONENTES ESTÉTICOS PADRONIZADOS (Refatorados)
    // =========================================================
    private JPanel criarCardSeccao(String titulo) {
        JPanel p = new JPanel(); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(235, 238, 242), 1, true), " " + titulo + " ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 15), Cores.ROSA_KAROL));
        p.setBorder(BorderFactory.createCompoundBorder(p.getBorder(), new EmptyBorder(25, 22, 25, 22))); return p;
    }

    private JTextField criarCampoTexto(String placeholder) { JTextField f = new JTextField(); f.setMaximumSize(new Dimension(Integer.MAX_VALUE, fieldSize.height)); f.setPreferredSize(fieldSize); f.putClientProperty("JTextField.placeholderText", placeholder); f.setFont(new Font("Segoe UI", Font.PLAIN, 15)); f.setBackground(Color.WHITE); f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(225, 228, 232), 1, true), new EmptyBorder(0, 10, 0, 10))); return f; }
    private JPanel montarBlocoInput(String t, JTextField c) { JPanel p = new JPanel(new BorderLayout(0, 6)); p.setOpaque(false); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); p.add(l, BorderLayout.NORTH); p.add(c, BorderLayout.CENTER); return p; }

    private JButton criarBotaoElegante(String texto, Color bg, String icone) {
        JButton btn = new JButton(texto); btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFont(new Font("Segoe UI", Font.BOLD, 14)); btn.setPreferredSize(new Dimension(160, 42)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false); btn.setBorder(new LineBorder(bg.darker(), 1, true));
        if (icone != null) { try { btn.setIcon(new FlatSVGIcon("icons/" + icone, 17, 17).setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE))); } catch(Exception ignored){} }
        return btn;
    }
}