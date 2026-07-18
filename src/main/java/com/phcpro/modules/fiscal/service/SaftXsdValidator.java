package com.phcpro.modules.fiscal.service;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida um XML (SAF-T) contra uma XSD do W3C XML Schema. Lógica pura (IO só na XSD) — testável com
 * uma XSD de exemplo. Recolhe todos os erros num só passo (não pára no primeiro). Endurecido contra
 * XXE (sem acesso a DTD/schema externos referenciados pelo XML).
 */
public final class SaftXsdValidator {

    private SaftXsdValidator() {}

    /** Devolve a lista de erros (vazia = válido). Lança em falha de configuração/leitura da XSD. */
    public static List<String> validate(String xml, File xsdFile) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(xsdFile);
        Validator validator = schema.newValidator();
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignored) {
            // propriedades não suportadas nalgumas implementações — não é fatal.
        }

        List<String> errors = new ArrayList<>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) { /* avisos não invalidam */ }
            @Override public void error(SAXParseException e) { errors.add(format(e)); }
            @Override public void fatalError(SAXParseException e) { errors.add(format(e)); }
        });
        try {
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            if (errors.isEmpty()) errors.add(e.getMessage());
        }
        return errors;
    }

    private static String format(SAXParseException e) {
        return "Linha " + e.getLineNumber() + ", coluna " + e.getColumnNumber() + ": " + e.getMessage();
    }
}
