package com.phcpro.architecture.concurrency;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Reexecuta uma operação de escrita quando falha por <b>conflito de concorrência</b> — lock optimista
 * ({@code @Version} no stock/lote/tesouraria) ou lock pessimista não adquirido — com um curto backoff.
 *
 * <p><b>Cada tentativa é uma NOVA transação.</b> O {@code Supplier} deve invocar um método
 * {@code @Transactional} <b>através do proxy do bean</b> (ex.: chamada do controller para o service),
 * nunca por auto-invocação; assim a tentativa falhada faz rollback completo antes da seguinte começar.
 *
 * <p>Não substitui os locks existentes (numeração pessimista gapless, {@code @Version}); é a
 * <b>rede de segurança</b> para o caso raro de dois postos escreverem a MESMA linha no mesmo instante
 * por um caminho que não serializa. {@link ConcurrencyFailureException} é a superclasse comum das
 * falhas transitórias (optimista, pessimista, deadlock) — só essas se repetem; erros de negócio ou de
 * integridade propagam de imediato.
 */
@Component
public class ConcurrencyRetry {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 40L;

    public <T> T run(Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return operation.get();
            } catch (ConcurrencyFailureException conflict) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw conflict;
                }
                backoff(BASE_BACKOFF_MS * attempt);
            }
        }
    }

    private void backoff(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido durante o retry de concorrência.", ie);
        }
    }
}
