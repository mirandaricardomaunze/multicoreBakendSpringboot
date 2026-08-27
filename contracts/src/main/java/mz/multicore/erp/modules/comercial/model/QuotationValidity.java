package mz.multicore.erp.modules.comercial.model;

/**
 * Validade padrão de uma cotação, em dias. Constante única — o dia em que passar a ser configurável
 * por empresa, muda-se aqui e nada mais, porque a data resultante fica <b>gravada em cada
 * documento</b> ({@code Quotation.validUntil}) e não é recalculada a partir daqui.
 */
public final class QuotationValidity {

    /** Trinta dias é a praxe comercial em Moçambique para propostas de fornecimento corrente. */
    public static final int DEFAULT_DAYS = 30;

    private QuotationValidity() {}
}
