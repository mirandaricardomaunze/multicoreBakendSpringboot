package mz.multicore.erp.gui.components;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa um rascunho de formulário persistido localmente.
 * Imutável e com suporte a cálculo de tempo decorrido para exibição em banners de recuperação.
 */
public record FormDraft(
        String formKey,
        String title,
        Map<String, String> payload,
        long timestampMillis,
        String author
) {
    public FormDraft {
        if (formKey == null || formKey.isBlank()) {
            throw new IllegalArgumentException("formKey do rascunho não pode ser vazia.");
        }
        if (title == null || title.isBlank()) {
            title = "Rascunho de Formulário";
        }
        if (payload == null) {
            payload = Collections.emptyMap();
        } else {
            payload = Collections.unmodifiableMap(new HashMap<>(payload));
        }
        if (timestampMillis <= 0) {
            timestampMillis = System.currentTimeMillis();
        }
        if (author == null) {
            author = "operador";
        }
    }

    public String formattedTimeAgo() {
        return formatTimeAgo(System.currentTimeMillis());
    }

    public String formatTimeAgo(long now) {
        long diff = Math.max(0, now - timestampMillis);
        long minutes = diff / (60 * 1000);
        if (minutes < 1) {
            return "Agora mesmo";
        }
        if (minutes < 60) {
            return "há " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "há " + hours + " h";
        }
        long days = hours / 24;
        return "há " + days + " d";
    }
}
