package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo modal corporativo para pesquisa e selecção de produtos com suporte duplo:
 * <ul>
 *   <li>Paginação local em memória para catálogos já descarregados;</li>
 *   <li>Paginação remota de servidor (server-side) assíncrona para catálogos volumosos.</li>
 * </ul>
 */
public final class ProductLookupDialog {

    /** Provedor assíncrono para pesquisa e paginação de artigos no servidor. */
    @FunctionalInterface
    public interface ProductPageProvider {
        PageResponse<ProductDTO> fetchPage(String query, Long categoryId, int page, int size) throws Exception;
    }

    /** Opção de categoria para o selector de filtros remotos. */
    public record CategoryOption(Long id, String name) {
        @Override
        public String toString() {
            return name == null ? "" : name;
        }
    }

    private ProductLookupDialog() {}

    // ==========================================
    // MÉTODOS LOCAIS (IN-MEMORY)
    // ==========================================

    /** Exibe o diálogo para selecção de um único produto a partir de lista em memória. */
    public static ProductDTO show(Component parent, List<ProductDTO> products) {
        List<ProductDTO> selected = ProductLookupLocalDialog.show(parent, products, false);
        return selected.isEmpty() ? null : selected.get(0);
    }

    /** Exibe o diálogo para selecção de múltiplos produtos a partir de lista em memória. */
    public static List<ProductDTO> showMulti(Component parent, List<ProductDTO> products) {
        return ProductLookupLocalDialog.show(parent, products, true);
    }

    /** Exibe o diálogo para selecção e repassa os produtos seleccionados a um consumidor. */
    public static void show(Component parent, List<ProductDTO> products, Consumer<List<ProductDTO>> onSelected) {
        List<ProductDTO> selected = showMulti(parent, products);
        if (selected != null && !selected.isEmpty() && onSelected != null) {
            onSelected.accept(selected);
        }
    }

    // ==========================================
    // MÉTODOS REMOTOS (SERVER-SIDE PAGINATION)
    // ==========================================

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única). */
    public static ProductDTO showRemote(Component parent, ProductPageProvider provider) {
        return showRemote(parent, provider, (List<CategoryOption>) null);
    }

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única com categorias). */
    public static ProductDTO showRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories) {
        List<ProductDTO> selected = ProductLookupRemoteDialog.show(parent, provider, categories, false);
        return selected.isEmpty() ? null : selected.get(0);
    }

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (selecção única com ProductCategoryDTO). */
    public static ProductDTO showRemoteCategories(Component parent, ProductPageProvider provider, List<ProductCategoryDTO> categories) {
        return showRemote(parent, provider, ProductLookupTableHelper.toOptions(categories));
    }

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção). */
    public static List<ProductDTO> showMultiRemote(Component parent, ProductPageProvider provider) {
        return showMultiRemote(parent, provider, (List<CategoryOption>) null);
    }

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção com categorias). */
    public static List<ProductDTO> showMultiRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories) {
        return ProductLookupRemoteDialog.show(parent, provider, categories, true);
    }

    /** Exibe o diálogo com pesquisa e paginação remota no servidor (múltipla selecção com ProductCategoryDTO). */
    public static List<ProductDTO> showMultiRemoteCategories(Component parent, ProductPageProvider provider, List<ProductCategoryDTO> categories) {
        return showMultiRemote(parent, provider, ProductLookupTableHelper.toOptions(categories));
    }

    /** Exibe o diálogo com pesquisa remota e entrega os resultados seleccionados a um consumidor. */
    public static void showRemote(Component parent, ProductPageProvider provider, List<CategoryOption> categories, Consumer<List<ProductDTO>> onSelected) {
        List<ProductDTO> selected = showMultiRemote(parent, provider, categories);
        if (selected != null && !selected.isEmpty() && onSelected != null) {
            onSelected.accept(selected);
        }
    }
}
