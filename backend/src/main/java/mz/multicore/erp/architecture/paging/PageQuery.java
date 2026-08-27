package mz.multicore.erp.architecture.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Normalização dos parâmetros de paginação vindos do HTTP.
 *
 * <p><b>Fonte única</b> dos limites: um cliente que peça {@code size=1000000} não pode obrigar o
 * servidor a materializar a tabela inteira — era exactamente o problema que a paginação veio
 * resolver. Página negativa vale 0; tamanho fora de gama é encaixado em [1, {@value #MAX_SIZE}].
 */
public final class PageQuery {

    /** Tamanho por omissão — uma página de tabela no desktop. */
    public static final int DEFAULT_SIZE = 50;

    /** Tecto rígido. Acima disto o pedido é encaixado, não recusado. */
    public static final int MAX_SIZE = 200;

    private PageQuery() {}

    public static Pageable of(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
