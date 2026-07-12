package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.session.DesktopSession;
import com.phcpro.desktop.session.DesktopSessionStore;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke de navegação do desktop (GUI-01): constrói a {@link MainFrame} real — o que instancia todos os
 * painéis e, logo, constrói todos os separadores — e a seguir simula o <b>clique em cada item de menu</b>
 * ({@code navigate(card)} → {@code onPanelSelected()}) e o <b>clique em cada separador</b>
 * ({@code setSelectedIndex}), afirmando que nada rebenta.
 *
 * <p>Os painéis {@code clientes} (servido por um cliente HTTP — {@code ComercialApiClient}) e
 * {@code plataforma} (só superadmin) ficam de fora: dependem de um backend/sessão de plataforma a correr,
 * não de serviços em-processo. Em ambiente sem ecrã (headless) o teste é ignorado.
 */
@SpringBootTest(properties = {
        // Mantém os beans do perfil "desktop" (MainFrame, DesktopSessionStore, ...) mas com a base de
        // dados de dev (H2 + Hibernate a criar o schema), em vez do PostgreSQL/Flyway de produção.
        "spring.datasource.url=jdbc:h2:mem:gui-smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("desktop")
class MainFrameNavigationSmokeTest {

    /** Cartões servidos por serviços em-processo (sem chamadas HTTP a um backend). */
    private static final List<String> IN_PROCESS_CARDS = List.of(
            "dashboard", "pos", "comercial", "compras", "stock",
            "financeiro", "hr", "crm", "fiscal", "approvals", "config");

    @Autowired ApplicationContext ctx;
    @Autowired CompanyRepository companyRepository;
    @Autowired DesktopSessionStore sessionStore;

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        sessionStore.clear();
    }

    @Test
    void todosOsMenusESeparadoresRespondemAoClique() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Sem ambiente gráfico — smoke de GUI ignorado.");

        Company pt = companyRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().contains("Portugal"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Dados de demo não semeados (empresa PT em falta)."));
        Long companyId = pt.getId();

        DesktopSession session = new DesktopSession(
                "smoke-token", Instant.now().plusSeconds(3600), "ana", "Ana Costa", false,
                List.of(new DesktopSession.CompanyAccess(companyId, pt.getName(), "ADMIN")));
        session.selectCompany(companyId);
        sessionStore.setSession(session);
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);

        List<String> failures = new ArrayList<>();
        int[] tabsExercised = {0};
        int[] menusExercised = {0};

        EventQueue.invokeAndWait(() -> {
            MainFrame frame = null;
            try {
                frame = ctx.getBean(MainFrame.class); // constrói todos os painéis → todos os separadores
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.applyAuthenticatedUser("Ana Costa", "ADMIN");

                Method navigate = MainFrame.class.getDeclaredMethod("navigate", String.class);
                navigate.setAccessible(true);

                // 1) Clique em cada item de menu → cardLayout.show + onPanelSelected/refreshData.
                for (String card : IN_PROCESS_CARDS) {
                    try {
                        navigate.invoke(frame, card);
                        menusExercised[0]++;
                    } catch (InvocationTargetException e) {
                        failures.add("menu '" + card + "': " + rootMessage(e.getCause()));
                    } catch (Exception e) {
                        failures.add("menu '" + card + "': " + rootMessage(e));
                    }
                }

                // 2) Clique em cada separador de cada JTabbedPane (inclui sub-separadores aninhados).
                for (JTabbedPane pane : collectTabbedPanes(frame.getContentPane())) {
                    for (int i = 0; i < pane.getTabCount(); i++) {
                        try {
                            pane.setSelectedIndex(i);
                            tabsExercised[0]++;
                        } catch (RuntimeException e) {
                            failures.add("separador '" + pane.getTitleAt(i) + "': " + rootMessage(e));
                        }
                    }
                }
            } catch (Throwable t) {
                failures.add("construção da MainFrame: " + rootMessage(t));
            } finally {
                if (frame != null) frame.dispose();
            }
        });

        System.out.println("[GUI-SMOKE] menus exercitados: " + menusExercised[0]
                + " | separadores exercitados: " + tabsExercised[0]);

        if (!failures.isEmpty()) {
            fail("Falhas ao clicar em menus/separadores:\n - " + String.join("\n - ", failures));
        }
        assertTrue(menusExercised[0] == IN_PROCESS_CARDS.size(),
                "Nem todos os menus foram exercitados: " + menusExercised[0] + "/" + IN_PROCESS_CARDS.size());
        assertTrue(tabsExercised[0] >= 40,
                "Esperava >= 40 separadores exercitados, foram " + tabsExercised[0]);
    }

    private static List<JTabbedPane> collectTabbedPanes(Component root) {
        List<JTabbedPane> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(Component c, List<JTabbedPane> out) {
        if (c instanceof JTabbedPane tp) out.add(tp);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) collect(child, out);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r.getClass().getSimpleName() + ": " + r.getMessage();
    }
}
