package br.sistema.view.panels;

import br.sistema.model.Paciente;
import br.sistema.util.Cores;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.Period;

public class DialogExecucaoAplicacao extends JDialog {

    private Paciente paciente;
    private String nomeVacina; // Aqui futuramente você passará o objeto Vacina ou Aplicacao
    private boolean isMenorDeIdade;
    private JButton btnConfirmar;
    private JCheckBox chkConsentimento;

    public DialogExecucaoAplicacao(Window owner, Paciente paciente, String nomeVacina) {
        super(owner, "Execução de Aplicação Médica", ModalityType.APPLICATION_MODAL);
        this.paciente = paciente;
        this.nomeVacina = nomeVacina;

        // Verifica idade para as regras de negócio
        Period idade = Period.between(paciente.getDataNascimento(), LocalDate.now());
        this.isMenorDeIdade = idade.getYears() < 18;

        setSize(700, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // --- CABEÇALHO ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Cores.VERDE_AQUA);
        pnlHeader.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel lblTitulo = new JLabel("Autorização de Aplicação: " + nomeVacina);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        pnlHeader.add(lblTitulo, BorderLayout.CENTER);
        add(pnlHeader, BorderLayout.NORTH);

        // --- CORPO DO PRONTUÁRIO ---
        JPanel pnlCorpo = new JPanel(new GridBagLayout());
        pnlCorpo.setOpaque(false);
        pnlCorpo.setBorder(new EmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0;

        // 1. Resumo do Paciente (Com Avatar e Idade Inteligente)
        JPanel pnlInfo = new JPanel(new BorderLayout(20, 0));
        pnlInfo.setOpaque(false);

        JLabel lblFoto = new JLabel();
        lblFoto.setPreferredSize(new Dimension(100, 100));
        if (paciente.getFoto() != null) {
            ImageIcon icon = new ImageIcon(paciente.getFoto());
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblFoto.setIcon(new ImageIcon(img));
        } else {
            lblFoto.setIcon(carregarIcone("member-list.svg", 80, Cores.CINZA_LABEL));
        }
        lblFoto.setBorder(new LineBorder(new Color(220, 220, 220), 2, true));
        pnlInfo.add(lblFoto, BorderLayout.WEST);

        JPanel pnlDados = new JPanel(new GridLayout(3, 1));
        pnlDados.setOpaque(false);

        JLabel lblNome = new JLabel(paciente.getNome());
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblNome.setForeground(new Color(50, 50, 50));
        pnlDados.add(lblNome);

        JLabel lblIdade = new JLabel("Idade: " + formatarIdadePediatrica(idade));
        lblIdade.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblIdade.setForeground(Cores.CINZA_LABEL);
        pnlDados.add(lblIdade);

        String textoEncaminhamento = paciente.getMedicoEncaminhador().isEmpty() ? "Demanda Espontânea" : "Encaminhado por: " + paciente.getMedicoEncaminhador();
        JLabel lblMedico = new JLabel(textoEncaminhamento);
        lblMedico.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblMedico.setForeground(new Color(100, 100, 100));
        pnlDados.add(lblMedico);

        pnlInfo.add(pnlDados, BorderLayout.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 30, 0);
        pnlCorpo.add(pnlInfo, gbc);

        // 2. BARREIRA DE SEGURANÇA: ALERGIAS (Obrigatório olhar)
        if (paciente.getAlergias() != null && !paciente.getAlergias().trim().isEmpty()) {
            JPanel pnlAlergia = new JPanel(new BorderLayout(15, 0));
            pnlAlergia.setBackground(new Color(255, 235, 238)); // Fundo vermelho claro
            pnlAlergia.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 53, 69), 2, true),
                    new EmptyBorder(15, 20, 15, 20)
            ));

            JLabel lblIconeAlerta = new JLabel();
            lblIconeAlerta.setIcon(carregarIcone("alerta.svg", 32, new Color(220, 53, 69))); // Se não tiver esse icone, ele mostra sem, não trava
            pnlAlergia.add(lblIconeAlerta, BorderLayout.WEST);

            JPanel pnlAlergiaTxt = new JPanel(new GridLayout(2, 1));
            pnlAlergiaTxt.setOpaque(false);
            JLabel lblAlergiaTitulo = new JLabel("ATENÇÃO: PACIENTE POSSUI ALERGIAS REGISTRADAS");
            lblAlergiaTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblAlergiaTitulo.setForeground(new Color(220, 53, 69));
            pnlAlergiaTxt.add(lblAlergiaTitulo);

            JLabel lblAlergiaDesc = new JLabel(paciente.getAlergias().toUpperCase());
            lblAlergiaDesc.setFont(new Font("Segoe UI", Font.BOLD, 16));
            pnlAlergiaTxt.add(lblAlergiaDesc);

            pnlAlergia.add(pnlAlergiaTxt, BorderLayout.CENTER);
            gbc.gridy = 1; gbc.insets = new Insets(0, 0, 30, 0);
            pnlCorpo.add(pnlAlergia, gbc);
        }

        // 3. BARREIRA DE SEGURANÇA: RESPONSÁVEL LEGAL
        if (isMenorDeIdade) {
            JPanel pnlResponsavel = new JPanel(new BorderLayout());
            pnlResponsavel.setOpaque(false);
            pnlResponsavel.setBorder(BorderFactory.createTitledBorder(
                    new LineBorder(new Color(200, 200, 200)), "Termo de Consentimento - Paciente Menor de Idade",
                    0, 0, new Font("Segoe UI", Font.BOLD, 12), Cores.CINZA_LABEL
            ));

            chkConsentimento = new JCheckBox("  Confirmo a autorização verbal/assinada do responsável: " + paciente.getNomeResponsavel());
            chkConsentimento.setFont(new Font("Segoe UI", Font.BOLD, 14));
            chkConsentimento.setForeground(new Color(50, 50, 50));
            chkConsentimento.setCursor(new Cursor(Cursor.HAND_CURSOR));
            chkConsentimento.setBorder(new EmptyBorder(15, 15, 15, 15));

            // Regra: Bloqueia o botão de salvar até marcar a caixa
            chkConsentimento.addActionListener(e -> btnConfirmar.setEnabled(chkConsentimento.isSelected()));

            pnlResponsavel.add(chkConsentimento, BorderLayout.CENTER);
            gbc.gridy = 2; gbc.insets = new Insets(0, 0, 20, 0);
            pnlCorpo.add(pnlResponsavel, gbc);
        }

        add(pnlCorpo, BorderLayout.CENTER);

        // --- RODAPÉ COM AÇÕES ---
        JPanel pnlRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        pnlRodape.setBackground(new Color(250, 250, 250));
        pnlRodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelar.setPreferredSize(new Dimension(120, 45));
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        pnlRodape.add(btnCancelar);

        btnConfirmar = new JButton("Confirmar Aplicação");
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnConfirmar.setBackground(Cores.VERDE_AQUA);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setPreferredSize(new Dimension(220, 45));
        btnConfirmar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Aplica a regra de bloqueio inicial se for menor de idade
        if (isMenorDeIdade) {
            btnConfirmar.setEnabled(false);
            btnConfirmar.setToolTipText("Confirme a autorização do responsável antes de aplicar.");
        }

        btnConfirmar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrarAplicacao();
            }
        });
        pnlRodape.add(btnConfirmar);

        add(pnlRodape, BorderLayout.SOUTH);
    }

    private void registrarAplicacao() {
        // Aqui nós faremos o INSERT na tabela de Aplicações e daremos baixa no Estoque!
        JOptionPane.showMessageDialog(this, "Aplicação registrada e vinculada ao Prontuário com sucesso!", "Sucesso Clínico", JOptionPane.INFORMATION_MESSAGE);
        dispose(); // Fecha o modal
    }

    // Algoritmo Pediátrico de Idade
    private String formatarIdadePediatrica(Period idade) {
        if (idade.getYears() == 0) {
            if (idade.getMonths() == 0) {
                return idade.getDays() + " dias";
            }
            return idade.getMonths() + " meses e " + idade.getDays() + " dias";
        } else if (idade.getYears() == 1) {
            return "1 ano e " + idade.getMonths() + " meses";
        }
        return idade.getYears() + " anos";
    }

    private FlatSVGIcon carregarIcone(String nomeArquivo, int tamanho, Color cor) {
        try {
            java.net.URL imgURL = getClass().getResource("/icons/" + nomeArquivo);
            if (imgURL != null) return (FlatSVGIcon) new FlatSVGIcon(imgURL).derive(tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
            return (FlatSVGIcon) new FlatSVGIcon("icons/" + nomeArquivo, tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
        } catch (Exception e) { return null; }
    }
}