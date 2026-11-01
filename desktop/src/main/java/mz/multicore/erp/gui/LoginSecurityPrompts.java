package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/** Janelas do login reforçado. O login corre em segundo plano, por isso cada uma volta ao EDT. */
final class LoginSecurityPrompts implements AuthApiClient.SecurityPrompts {

    private final Window owner;

    LoginSecurityPrompts(Window owner) {
        this.owner = owner;
    }

    @Override
    public String twoFactorCode() {
        return onEdt(() -> ask("Dupla Autenticação", "Introduza o código da aplicação do telemóvel.", "Código:", new JTextField()));
    }

    @Override
    public String newPassword() {
        return onEdt(() -> ask("Definir Nova Senha", "Esta senha foi definida por outra pessoa. Escolha a sua.",
                "Nova senha:", new JPasswordField()));
    }

    @Override
    public String setupCode(String secret) {
        return onEdt(() -> ask("Configurar Dupla Autenticação",
                "Na aplicação do telemóvel, adicione a chave abaixo e introduza o código gerado.",
                "Código:", new JTextField(), secret));
    }

    private String ask(String title, String subtitle, String label, JTextField field) {
        return ask(title, subtitle, label, field, null);
    }

    private String ask(String title, String subtitle, String label, JTextField field, String secret) {
        UIHelper.styleTextField(field);
        JPanel form;
        if (secret == null) {
            form = UIHelper.createDialogForm(label, field);
        } else {
            JTextField key = new JTextField(secret);
            key.setEditable(false);
            UIHelper.styleTextField(key);
            form = UIHelper.createDialogForm("Chave:", key, label, field);
        }
        boolean confirmed = new ModernFormDialog(owner, title, "fas-shield-alt", subtitle, form)
                .setConfirmButton("Continuar", "fas-check").showDialog();
        return confirmed ? field.getText() : null;
    }

    private static String onEdt(Supplier<String> prompt) {
        String[] result = {null};
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = prompt.get());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Não foi possível pedir os dados de segurança.", ex);
        }
        return result[0];
    }
}
