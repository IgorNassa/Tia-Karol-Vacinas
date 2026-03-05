package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PainelFormulario extends JPanel {
    private TelaPrincipal frame;

    // Transformamos os inputs em atributos da classe para o botão conseguir ler depois
    private JTextField txtPaciente;
    private JTextField txtDataHora;
    private JComboBox<String> cbVacina;
    private JComboBox<String> cbStatus;
    private JComboBox<String> cbPagamento;
    private JTextField txtValor;

    public PainelFormulario(TelaPrincipal frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new GridBagLayout());

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setPreferredSize(new Dimension(800, 780));
        cardVidro.setLayout(new GridBagLayout());
        cardVidro.setBorder(new EmptyBorder(30, 60, 40, 70));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel btnVoltar = new JLabel("← Voltar para o Painel");
        btnVoltar.setForeground(Cores.VERDE_AQUA);
        btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnVoltar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnVoltar.setForeground(Cores.ROSA_KAROL); }
            @Override
            public void mouseExited(MouseEvent e) { btnVoltar.setForeground(Cores.VERDE_AQUA); }
            @Override
            public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelDashboard(frame)); }
        });

        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 15, 0);
        cardVidro.add(btnVoltar, gbc);

        JLabel lblTitulo = new JLabel("Agendar ou Registrar Aplicação", JLabel.CENTER);
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32));
        lblTitulo.setForeground(Cores.ROSA_KAROL);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 30, 0);
        cardVidro.add(lblTitulo, gbc);

        // LINHA 1: Paciente e Data
        JPanel gridTopo = new JPanel(new GridLayout(1, 2, 30, 0));
        gridTopo.setOpaque(false);

        JPanel pPaciente = new JPanel(new BorderLayout(0, 8)); pPaciente.setOpaque(false);
        pPaciente.add(criarLabelElite("NOME DO PACIENTE / CPF"), BorderLayout.NORTH);
        txtPaciente = criarCampoTexto("Pesquise o paciente...");
        pPaciente.add(txtPaciente, BorderLayout.CENTER);

        JPanel pData = new JPanel(new BorderLayout(0, 8)); pData.setOpaque(false);
        pData.add(criarLabelElite("DATA / HORA"), BorderLayout.NORTH);
        txtDataHora = criarCampoTexto("dd/mm/aaaa hh:mm");
        pData.add(txtDataHora, BorderLayout.CENTER);

        gridTopo.add(pPaciente);
        gridTopo.add(pData);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 20, 0);
        cardVidro.add(gridTopo, gbc);

        // LINHA 2: Vacina e Status
        JPanel gridMeio = new JPanel(new GridLayout(1, 2, 30, 0));
        gridMeio.setOpaque(false);

        JPanel pVacina = new JPanel(new BorderLayout(0, 8)); pVacina.setOpaque(false);
        pVacina.add(criarLabelElite("VACINA (LOTE)"), BorderLayout.NORTH);
        cbVacina = new JComboBox<>(new String[]{"Selecione a vacina...", "Hexavalente", "Meningocócica B", "Febre Amarela", "Rotavírus"});
        cbVacina.setPreferredSize(new Dimension(0, 50));
        cbVacina.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cbVacina.setBackground(Color.WHITE);
        aplicarHoverMinimalista(cbVacina);
        pVacina.add(cbVacina, BorderLayout.CENTER);

        JPanel pStatus = new JPanel(new BorderLayout(0, 8)); pStatus.setOpaque(false);
        pStatus.add(criarLabelElite("STATUS"), BorderLayout.NORTH);
        cbStatus = new JComboBox<>(new String[]{"Agendado", "Aplicado"});
        cbStatus.setPreferredSize(new Dimension(0, 50));
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cbStatus.setBackground(Color.WHITE);
        aplicarHoverMinimalista(cbStatus);
        pStatus.add(cbStatus, BorderLayout.CENTER);

        gridMeio.add(pVacina);
        gridMeio.add(pStatus);
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 20, 0);
        cardVidro.add(gridMeio, gbc);

        // LINHA 3: Pagamento e Valor
        JPanel gridValores = new JPanel(new GridLayout(1, 2, 30, 0));
        gridValores.setOpaque(false);

        JPanel pPagamento = new JPanel(new BorderLayout(0, 8)); pPagamento.setOpaque(false);
        pPagamento.add(criarLabelElite("FORMA DE PAGAMENTO"), BorderLayout.NORTH);
        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão", "Dinheiro"});
        cbPagamento.setPreferredSize(new Dimension(0, 50));
        cbPagamento.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cbPagamento.setBackground(Color.WHITE);
        aplicarHoverMinimalista(cbPagamento);
        pPagamento.add(cbPagamento, BorderLayout.CENTER);

        JPanel pValor = new JPanel(new BorderLayout(0, 8)); pValor.setOpaque(false);
        pValor.add(criarLabelElite("VALOR FINAL COBRADO (R$)"), BorderLayout.NORTH);
        txtValor = criarCampoTexto("0,00");
        txtValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtValor.setBackground(new Color(250, 255, 252));
        aplicarHoverMinimalista(txtValor);
        pValor.add(txtValor, BorderLayout.CENTER);

        gridValores.add(pPagamento);
        gridValores.add(pValor);
        gbc.gridy = 4; gbc.insets = new Insets(10, 0, 40, 0);
        cardVidro.add(gridValores, gbc);

        // BOTÃO SALVAR (Agora chama a função que grava no banco)
        JButton btnSalvar = new JButton("Confirmar Agendamento / Registro");
        btnSalvar.setPreferredSize(new Dimension(0, 60));
        btnSalvar.setBackground(Cores.VERDE_AQUA);
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSalvar.addActionListener(e -> salvarRegistro()); // A MÁGICA ACONTECE AQUI!

        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 0, 0);
        cardVidro.add(btnSalvar, gbc);

        add(cardVidro);
    }

    // --- LÓGICA DE SALVAR NO BANCO DE DADOS ---
    private void salvarRegistro() {
        // 1. Pega os textos das caixas
        String paciente = txtPaciente.getText();
        String dataHora = txtDataHora.getText();
        String vacina = (String) cbVacina.getSelectedItem();
        String status = (String) cbStatus.getSelectedItem();
        String pagamento = (String) cbPagamento.getSelectedItem();

        // Validação super rápida para não salvar vazio
        if (paciente.isEmpty() || paciente.equals("Pesquise o paciente...")) {
            JOptionPane.showMessageDialog(frame, "Por favor, informe o nome do paciente.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Trata o valor (Transforma "150,00" em número que o banco entende: 150.00)
        double valorTratado = 0.0;
        try {
            String valorTexto = txtValor.getText().replace("R$", "").trim().replace(".", "").replace(",", ".");
            if (!valorTexto.isEmpty()) {
                valorTratado = Double.parseDouble(valorTexto);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Digite um valor numérico válido.", "Erro no Valor", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Cria o objeto do Modelo
        Aplicacao novaApp = new Aplicacao(paciente, dataHora, vacina, status, pagamento, valorTratado);

        // 4. Manda o DAO salvar no SQLite
        AplicacaoDAO dao = new AplicacaoDAO();
        dao.salvar(novaApp);

        // 5. Avisa que deu certo e volta pra tela inicial
        JOptionPane.showMessageDialog(frame, "Registro salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        frame.trocarTelaCentral(new PainelDashboard(frame));
    }

    // --- MÉTODOS AUXILIARES DE UI ---
    private JLabel criarLabelElite(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Cores.CINZA_LABEL);
        l.setBorder(new EmptyBorder(0, 2, 0, 0));
        return l;
    }

    private JTextField criarCampoTexto(String placeholder) {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(0, 50));
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JTextField.showClearButton", true);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        f.setBackground(Color.WHITE);
        aplicarHoverMinimalista(f);
        return f;
    }

    private void aplicarHoverMinimalista(JComponent comp) {
        Color corNormal = comp.getBackground();
        Color corHover = new Color(242, 248, 248);

        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (comp.isEnabled() && !comp.hasFocus()) { comp.setBackground(corHover); }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!comp.hasFocus()) { comp.setBackground(corNormal); }
            }
        });

        if(comp instanceof JTextField) {
            ((JTextField)comp).addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) { comp.setBackground(Color.WHITE); }
                public void focusLost(java.awt.event.FocusEvent evt) { comp.setBackground(corNormal); }
            });
        }
    }
}