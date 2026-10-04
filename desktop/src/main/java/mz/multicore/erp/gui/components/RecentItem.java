package mz.multicore.erp.gui.components;

/**
 * Representa um registo de atividade ou entidade visitada recentemente no ERP.
 * Imutável e com suporte a cálculo ergonómico de tempo decorrido.
 */
public record RecentItem(
        String id,
        String category,
        String title,
        String subtitle,
        String targetView,
        Long recordId,
        String iconCode,
        long timestampMillis
) {
    public RecentItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID do item recente não pode ser vazio.");
        }
        if (title == null || title.isBlank()) {
            title = "Item Sem Título";
        }
        if (category == null || category.isBlank()) {
            category = "Geral";
        }
        if (iconCode == null || iconCode.isBlank()) {
            iconCode = "fas-circle";
        }
        if (timestampMillis <= 0) {
            timestampMillis = System.currentTimeMillis();
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
