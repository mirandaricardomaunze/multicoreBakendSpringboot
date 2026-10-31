package mz.multicore.erp.modules.printing;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class QuotationPrintServiceDecompositionHarnessTest {
 @Test void thermalRendererRemainsExtracted() throws Exception {
  Path dir=Path.of("src/main/java/mz/multicore/erp/modules/printing");
  String facade=Files.readString(dir.resolve("QuotationPrintService.java"));
  assertTrue(facade.contains("thermalRenderer.render("));
  assertFalse(facade.contains("buildReceipt(doc ->"));
  assertTrue(Files.readAllLines(dir.resolve("QuotationThermalRenderer.java")).size()<=100);
  assertTrue(facade.contains("a4Renderer.render("));
  assertTrue(Files.readAllLines(dir.resolve("QuotationPrintService.java")).size()<=60);
  assertTrue(Files.readAllLines(dir.resolve("QuotationA4Renderer.java")).size()<=240);
 }
}
