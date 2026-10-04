package mz.multicore.erp.desktop.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopApiConfigSecurityTest {

    @Test
    void requiresHttpsForRemoteServers() {
        assertEquals("https://erp.example.co.mz", new DesktopApiConfig("https://erp.example.co.mz/").baseUrl());
        assertThrows(IllegalArgumentException.class, () -> new DesktopApiConfig("http://erp.example.co.mz"));
        assertThrows(IllegalArgumentException.class, () -> new DesktopApiConfig("http://localhost.evil.example"));
    }

    @Test
    void keepsLocalDevelopmentAvailable() {
        assertEquals("http://localhost:8080", new DesktopApiConfig("http://localhost:8080").baseUrl());
        assertEquals("http://127.0.0.1:8080", new DesktopApiConfig("http://127.0.0.1:8080").baseUrl());
    }
}
