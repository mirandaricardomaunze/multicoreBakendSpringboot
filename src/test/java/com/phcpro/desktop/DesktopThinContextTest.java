package com.phcpro.desktop;

import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.POSApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova a arquitetura de <b>cliente-fino</b>: o contexto Spring do desktop arranca
 * <b>sem base de dados</b> e <b>sem os serviços/repositórios de backend</b> — só a UI
 * e os clientes HTTP. É esta propriedade que permite fechar o PostgreSQL ao exterior.
 */
@SpringBootTest(classes = DesktopApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("desktop")
class DesktopThinContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void desktopContextLoadsWithoutDatabaseOrBackendServices() {
        // 1. Nenhum DataSource — o desktop não liga à base de dados.
        assertThat(context.getBeanNamesForType(DataSource.class))
                .as("o desktop cliente-fino não deve ter DataSource")
                .isEmpty();

        // 2. Os clientes HTTP existem — a UI consegue falar com o backend.
        assertThat(context.getBeansOfType(ComercialApiClient.class)).isNotEmpty();
        assertThat(context.getBeansOfType(POSApiClient.class)).isNotEmpty();

        // 3. Nenhum serviço/repositório de backend foi carregado no contexto do desktop.
        assertThat(context.containsBean("comercialService")).isFalse();
        assertThat(context.containsBean("inventoryService")).isFalse();
        assertThat(context.containsBean("POSService")).isFalse();
        assertThat(context.containsBean("productRepository")).isFalse();
    }
}
