package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Fluxo único de impressão de documentos sensíveis no desktop.
 * A primeira via usa o endpoint normal; as seguintes pedem autorização de outro responsável.
 */
public final class SensitivePrintCoordinator {
    private SensitivePrintCoordinator() {}

    public static void print(Component owner,
                             PrintApiClient printApiClient,
                             SensitiveDocumentType type,
                             Long documentId,
                             String documentLabel,
                             String fileName,
                             Callable<byte[]> firstPrint,
                             Runnable afterPrint) {
        print(owner, printApiClient, type, documentId, documentLabel, fileName, firstPrint,
                pdf -> PrintPreviewDialog.show(owner, pdf, fileName), afterPrint);
    }

    public static void print(Component owner,
                             PrintApiClient printApiClient,
                             SensitiveDocumentType type,
                             Long documentId,
                             String documentLabel,
                             String fileName,
                             Callable<byte[]> firstPrint,
                             Consumer<byte[]> output,
                             Runnable afterPrint) {
        JComponent anchor;
        if (owner instanceof JComponent component) {
            anchor = component;
        } else if (owner instanceof javax.swing.RootPaneContainer rootPaneContainer) {
            anchor = rootPaneContainer.getRootPane();
        } else {
            anchor = new javax.swing.JPanel();
        }
        if (printApiClient == null) {
            render(anchor, "A gerar o documento…", firstPrint, output, afterPrint);
            return;
        }
        UIHelper.runWithProgress(anchor, "A verificar o histórico de impressão…",
                () -> printApiClient.getPrintStatus(type, documentId),
                status -> continuePrint(anchor, printApiClient, type, documentId, documentLabel,
                        firstPrint, output, afterPrint, status),
                error -> showError(anchor, "Impressão não disponível", error));
    }

    private static void continuePrint(JComponent owner,
                                      PrintApiClient printApiClient,
                                      SensitiveDocumentType type,
                                      Long documentId,
                                      String documentLabel,
                                      Callable<byte[]> firstPrint,
                                      Consumer<byte[]> output,
                                      Runnable afterPrint,
                                      DocumentPrintStatusDTO status) {
        if (status == null || status.printCount() == 0) {
            render(owner, "A gerar a primeira via…", firstPrint, output, afterPrint);
            return;
        }

        ReprintAuthorizationRequest request = requestAuthorization(owner, documentLabel, status);
        if (request == null) return;
        render(owner, "A validar a autorização e gerar a reimpressão…",
                () -> printApiClient.reprint(type, documentId, request), output, afterPrint);
    }

    private static ReprintAuthorizationRequest requestAuthorization(
            JComponent owner, String documentLabel, DocumentPrintStatusDTO status) {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField reason = new JTextField();
        UIHelper.styleTextField(username);
        UIHelper.stylePasswordField(password);
        UIHelper.styleTextField(reason);

        JComponent form = UIHelper.createDialogForm(
                "Documento:", readOnly(documentLabel),
                "Vias já emitidas:", readOnly(String.valueOf(status.printCount())),
                "Outro gerente/administrador:", username,
                "Senha:", password,
                "Motivo da reimpressão:", reason);
        ReprintAuthorizationRequest[] result = new ReprintAuthorizationRequest[1];
        ModernFormDialog dialog = new ModernFormDialog(
                SwingUtilities.getWindowAncestor(owner),
                "Autorizar Reimpressão", "fas-user-shield",
                "A nova via ficará marcada e registada na auditoria.", form)
                .setConfirmButton("Autorizar e Reimprimir", "fas-print")
                .setSize(620, 510);
        dialog.setOnSave(() -> {
            String approver = username.getText().trim();
            String secret = new String(password.getPassword());
            String justification = reason.getText().trim();
            if (approver.isBlank()) fail(username, "Informe o utilizador autorizador.");
            String loggedUser = mz.multicore.erp.architecture.security.CurrentUserContext.getUsername();
            if (loggedUser != null && !loggedUser.isBlank() && approver.equalsIgnoreCase(loggedUser)) {
                fail(username, "A reimpressão exige autorização presencial de outro utilizador (gerente ou administrador). Não pode autorizar a sua própria reimpressão.");
            }
            if (secret.isBlank()) fail(password, "Informe a senha do autorizador.");
            if (justification.isBlank()) fail(reason, "Indique o motivo da reimpressão.");
            result[0] = new ReprintAuthorizationRequest(
                    approver, secret, justification, terminalName());
        });
        return dialog.showDialog() ? result[0] : null;
    }

    private static JTextField readOnly(String value) {
        JTextField field = new JTextField(value == null ? "—" : value);
        UIHelper.styleTextField(field);
        field.setEditable(false);
        return field;
    }

    private static void fail(JComponent field, String message) {
        field.requestFocusInWindow();
        throw new IllegalArgumentException(message);
    }

    private static void render(JComponent owner, String progress, Callable<byte[]> task,
                               Consumer<byte[]> output, Runnable afterPrint) {
        UIHelper.runWithProgress(owner, progress, task,
                pdf -> {
                    output.accept(pdf);
                    if (afterPrint != null) afterPrint.run();
                },
                error -> showError(owner, "Não foi possível imprimir", error));
    }

    private static String terminalName() {
        String name = System.getenv("COMPUTERNAME");
        return name == null || name.isBlank() ? "POSTO-DESKTOP" : name.trim();
    }

    private static String message(Throwable error) {
        return error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "Não foi possível concluir a operação."
                : error.getMessage();
    }

    private static void showError(Component owner, String title, Throwable error) {
        ModernMessageDialog.show(SwingUtilities.getWindowAncestor(owner), FeedbackType.ERROR,
                title, message(error));
    }
}
