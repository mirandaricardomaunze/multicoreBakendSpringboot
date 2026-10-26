package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemImportDTO;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal para importação de extractos bancários oficiais.
 * Suporta BIM, BCI, Standard Bank e Moza via CSV delimitado com conversão tolerante.
 */
final class BankStatementImportDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BankStatementImportDialog() {}

    static void show(BankReconciliationPanel owner) {
        TreasuryAccountDTO acc = (TreasuryAccountDTO) owner.accountCombo.getSelectedItem();
        if (acc == null) {
            owner.showNotice(FeedbackType.WARNING, "Selecção necessária", "Seleccione a conta bancária antes de importar extracto.");
            return;
        }

        JTextField refField = createField("EXT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")), 360);
        JTextField startField = createField(LocalDate.now().withDayOfMonth(1).toString(), 170);
        JTextField endField = createField(LocalDate.now().toString(), 170);
        JPanel datesPanel = flow(startField, new JLabel("até"), endField);

        JTextField openBalField = createField("0.00", 170);
        JTextField closeBalField = createField("0.00", 170);
        JPanel balPanel = flow(openBalField, new JLabel("Final:"), closeBalField);

        JTextArea csvArea = new JTextArea(8, 35);
        csvArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        csvArea.setBackground(UIHelper.FIELD_BG);
        csvArea.setForeground(UIHelper.TEXT_LIGHT);
        csvArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        csvArea.setText("# Formato: Data;Descrição;Referência;Valor\n"
                + LocalDate.now() + ";Depósito Cliente ABC;DEP-9988;25000.00\n"
                + LocalDate.now() + ";Comissão de Manutenção;;-150.00");

        JScrollPane csvScroll = new JScrollPane(csvArea);
        UIHelper.styleScrollPane(csvScroll);

        ModernButton loadCsvFileBtn = UIHelper.createSecondaryButton("Carregar Ficheiro CSV");
        loadCsvFileBtn.setIcon(UIHelper.icon("fas-folder-open", 13));
        loadCsvFileBtn.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        loadCsvFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros de Extracto (*.csv, *.txt)", "csv", "txt"));
            if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = chooser.getSelectedFile();
                    csvArea.setText(Files.readString(f.toPath()));
                } catch (Exception ex) {
                    ToastManager.show(owner, FeedbackType.ERROR, "Erro ao ler ficheiro: " + ex.getMessage());
                }
            }
        });

        JPanel csvBox = new JPanel(new BorderLayout(0, 5));
        csvBox.setOpaque(false);
        csvBox.add(loadCsvFileBtn, BorderLayout.NORTH);
        csvBox.add(csvScroll, BorderLayout.CENTER);

        JPanel form = UIHelper.createDialogForm(
                "Referência do Extracto:", refField,
                "Período (Início / Fim):", datesPanel,
                "Saldos (Inicial / Final):", balPanel,
                "Linhas do Extracto:", csvBox
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Importar Extracto Bancário",
                "fas-file-import", "Carregue o ficheiro oficial ou cole os movimentos do extracto", form)
                .setConfirmButton("Importar e Validar", "fas-check");

        dlg.setOnSaveAsync(() -> {
            String ref = refField.getText().trim();
            LocalDate sDate = parseDateSafe(startField.getText().trim(), LocalDate.now().withDayOfMonth(1));
            LocalDate eDate = parseDateSafe(endField.getText().trim(), LocalDate.now());
            BigDecimal oBal = parseAmountSafe(openBalField.getText().trim());
            BigDecimal cBal = parseAmountSafe(closeBalField.getText().trim());

            List<BankStatementItemImportDTO> items = parseCsvLines(csvArea.getText());
            if (items.isEmpty()) throw new IllegalArgumentException("Nenhuma linha de movimento válida foi detectada no extracto.");

            ImportBankStatementRequest req = new ImportBankStatementRequest(acc.id(), ref, sDate, eDate, oBal, cBal, items);
            return () -> owner.apiClient.importStatement(req);
        });

        if (dlg.showDialog()) {
            ToastManager.success(owner, "Extracto bancário importado com sucesso!");
            owner.onAccountChanged();
        }
    }

    private static JTextField createField(String text, int width) {
        JTextField f = new JTextField(text);
        UIHelper.styleTextField(f);
        f.setPreferredSize(new Dimension(width, UIHelper.FORM_CONTROL_HEIGHT));
        return f;
    }

    private static JPanel flow(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        for (Component c : comps) p.add(c);
        return p;
    }

    private static List<BankStatementItemImportDTO> parseCsvLines(String raw) {
        List<BankStatementItemImportDTO> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String line : raw.split("\r?\n")) {
            String clean = line.trim();
            if (clean.isEmpty() || clean.startsWith("#") || clean.toLowerCase().startsWith("data")) continue;
            String[] rawTokens = clean.contains(";") ? clean.split(";") : clean.contains(",") ? clean.split(",") : clean.split("\t");
            List<String> tokens = new ArrayList<>();
            for (String t : rawTokens) {
                String token = t.trim();
                if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                    token = token.substring(1, token.length() - 1).trim();
                }
                tokens.add(token);
            }
            if (tokens.size() >= 3) {
                LocalDate date = parseDateSafe(tokens.get(0), LocalDate.now());
                String desc = tokens.get(1);
                String ref = tokens.size() >= 4 && !tokens.get(2).isBlank() ? tokens.get(2) : null;
                BigDecimal amt = tokens.size() >= 4 ? parseAmountSafe(tokens.get(3)) : parseAmountSafe(tokens.get(2));
                result.add(new BankStatementItemImportDTO(date, date, desc, ref, amt, null));
            } else if (tokens.size() == 2) {
                result.add(new BankStatementItemImportDTO(parseDateSafe(tokens.get(0), LocalDate.now()), LocalDate.now(), "(movimento bancário)", null, parseAmountSafe(tokens.get(1)), null));
            }
        }
        return result;
    }

    private static LocalDate parseDateSafe(String text, LocalDate fallback) {
        if (text == null || text.isBlank()) return fallback;
        String clean = text.trim();
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{DateTimeFormatter.ISO_LOCAL_DATE, DATE_FMT, DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("yyyy/MM/dd")}) {
            try { return LocalDate.parse(clean, fmt); } catch (Exception ignored) {}
        }
        return fallback;
    }

    private static BigDecimal parseAmountSafe(String text) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        String clean = text.replaceAll("[^0-9,.-]", "").trim();
        if (clean.isEmpty()) return BigDecimal.ZERO;
        try {
            if (clean.contains(",") && clean.contains(".")) {
                clean = clean.lastIndexOf(',') > clean.lastIndexOf('.') ? clean.replace(".", "").replace(',', '.') : clean.replace(",", "");
            } else if (clean.contains(",")) clean = clean.replace(',', '.');
            return new BigDecimal(clean);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
