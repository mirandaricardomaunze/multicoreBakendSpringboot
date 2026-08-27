package mz.multicore.erp.modules.fiscal.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link SaftXsdValidator} (SV-01/02) — prova o mecanismo de validação contra uma XSD de
 * exemplo (a XSD oficial da AT-MZ é fornecida em runtime via {@code fiscal.saft.xsd-path}).
 */
class SaftXsdValidatorTest {

    private static final String XSD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="AuditFile">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="Header" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

    @Test
    void validXml_noErrors(@TempDir Path dir) throws Exception {
        File xsd = writeXsd(dir);
        List<String> errors = SaftXsdValidator.validate(
                "<AuditFile><Header>ACME</Header></AuditFile>", xsd);
        assertTrue(errors.isEmpty(), "XML conforme não devia ter erros: " + errors);
    }

    @Test
    void invalidXml_reportsErrors(@TempDir Path dir) throws Exception {
        File xsd = writeXsd(dir);
        List<String> errors = SaftXsdValidator.validate(
                "<AuditFile><Wrong>x</Wrong></AuditFile>", xsd);
        assertFalse(errors.isEmpty(), "XML não conforme devia reportar erros");
    }

    private File writeXsd(Path dir) throws Exception {
        File f = dir.resolve("saft-sample.xsd").toFile();
        Files.writeString(f.toPath(), XSD);
        return f;
    }
}
