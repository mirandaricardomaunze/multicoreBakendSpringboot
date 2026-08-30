package mz.multicore.erp.gui.components;

import javax.swing.JTextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Input canónico de data ISO usado pelos contratos HTTP (`yyyy-MM-dd`). */
public class DateField extends JTextField {

    public DateField() { this(null); }

    public DateField(LocalDate value) {
        super(value == null ? "" : value.toString());
        // Dez colunas ANTES de estilizar. O styleTextField congela a largura no momento em que
        // corre; um DateField construído vazio ficava do tamanho do vazio e nunca mais crescia,
        // mesmo que lhe pusessem a data a seguir com setText. Era o que se via no Balancete e no
        // Razão da Contabilidade: dois campos de ~20 px onde devia estar "2026-08-01".
        // Mesma causa do selector de "Por página" que truncava em todas as tabelas — medir cedo
        // de mais é o defeito, não a largura em si.
        setColumns(10);
        UIHelper.styleTextField(this);
        setToolTipText("Formato: yyyy-MM-dd");
        getAccessibleContext().setAccessibleDescription("Data no formato ano-mês-dia");
    }

    /**
     * Só ISO, de propósito — e o {@code CanonicalFormFieldsTest} carrega essa decisão.
     *
     * <p>Aceitar também dd/MM/yyyy parece simpático e é uma armadilha: o formato humano é ambíguo
     * entre dia e mês para quem integra, e o campo existe para alimentar contratos HTTP que são
     * ISO. Quem mostra datas ao utilizador formata-as; quem as recebe exige o formato canónico.
     *
     * <p>A consequência disto é que <b>quem pré-preenche um DateField tem de escrever ISO</b>. O
     * Balancete e o Razão da Contabilidade escreviam dd/MM/yyyy e por isso nasciam partidos.
     */
    public LocalDate value() {
        try {
            LocalDate parsed = LocalDate.parse(getText().trim());
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (DateTimeParseException ex) {
            String message = "Introduza uma data válida no formato yyyy-MM-dd.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        }
    }
}
