package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/** Valida o tenant declarado pelo cliente contra a empresa autenticada no servidor. */
public final class TenantRequestBoundaryGuard {

    private TenantRequestBoundaryGuard() {}

    public static void validateUrlCompany(String queryCompanyId, Map<String, String> pathVariables) {
        Long currentCompanyId = CurrentUserContext.findCurrentCompanyId();
        if (currentCompanyId == null) return;

        if (queryCompanyId != null && !queryCompanyId.isBlank()) {
            requireSameCompany(parseCompanyId(queryCompanyId));
        }
        if (pathVariables != null) {
            String pathCompanyId = pathVariables.get("companyId");
            if (pathCompanyId != null && !pathCompanyId.isBlank()) {
                requireSameCompany(parseCompanyId(pathCompanyId));
            }
        }
    }

    public static void validateBody(Object body) {
        if (body == null || CurrentUserContext.findCurrentCompanyId() == null) return;
        Method accessor;
        try {
            accessor = body.getClass().getMethod("companyId");
        } catch (NoSuchMethodException ignored) {
            return;
        }
        if (!Long.class.equals(accessor.getReturnType()) && accessor.getReturnType() != long.class) return;
        try {
            Object value = accessor.invoke(body);
            if (value != null) requireSameCompany(((Number) value).longValue());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new BusinessRuleException("Não foi possível validar a empresa indicada no pedido.");
        }
    }

    private static Long parseCompanyId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("A empresa indicada no pedido não é válida.");
        }
    }

    private static void requireSameCompany(Long requestedCompanyId) {
        if (!CurrentUserContext.requireCurrentCompanyId().equals(requestedCompanyId)) {
            throw new BusinessRuleException("Operação recusada: os dados pertencem a outra empresa.");
        }
    }
}
