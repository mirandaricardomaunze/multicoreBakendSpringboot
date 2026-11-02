package mz.multicore.erp.modules.pos.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.ClientIpResolver;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.pos.dto.DiscountAuthorizationDTOs;
import mz.multicore.erp.modules.pos.service.PosDiscountAuthorizationService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Troca o PIN de um gerente por uma autorização de desconto válida para uma venda. */
@RestController
@RequestMapping("/api/pos/discount-authorization")
public class PosDiscountAuthorizationController {

    private final AppUserService appUserService;
    private final PosDiscountAuthorizationService authorizationService;
    private final ClientIpResolver clientIpResolver;

    public PosDiscountAuthorizationController(AppUserService appUserService,
                                              PosDiscountAuthorizationService authorizationService,
                                              ClientIpResolver clientIpResolver) {
        this.appUserService = appUserService;
        this.authorizationService = authorizationService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public DiscountAuthorizationDTOs.Response authorize(@RequestBody @Valid DiscountAuthorizationDTOs.Request request,
                                                        HttpServletRequest httpRequest) {
        var result = appUserService.verifyManagerPin(request.pin(), clientIpResolver.resolve(httpRequest));
        String token = result.approved()
                ? authorizationService.issue(CurrentUserContext.getCurrentCompanyId(), result.managerName(), request.maxDiscountPercent())
                : null;
        return new DiscountAuthorizationDTOs.Response(result.approved(), result.managerName(), result.message(), token);
    }
}
