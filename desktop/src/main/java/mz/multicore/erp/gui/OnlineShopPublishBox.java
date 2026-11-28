package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.OnlineShopApiClient;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.util.Objects;

/**
 * "Publicar na loja online" box of the product form (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-02). Off by default and
 * always off on a new product until it is saved; on an existing product the state comes from the server and the
 * form saves it only when the box changed. The backend's refusal (e.g. Adiantamento) reaches the form unchanged.
 */
public final class OnlineShopPublishBox {

    public static final String LABEL = "Publicar na loja online";
    public static final String NEW_PRODUCT_HINT = "Grave o artigo antes de o publicar na loja online.";

    private final OnlineShopApiClient client;
    private final JCheckBox box = new JCheckBox(LABEL);
    /** Product whose state is loaded (or loading) and the state the server gave; null while unknown. */
    private Long productId;
    private Boolean serverState;

    private OnlineShopPublishBox(OnlineShopApiClient client) {
        this.client = client;
        box.setOpaque(false);
        box.setSelected(false);
        box.setEnabled(false);
    }

    /** New product: shown off and disabled until the product is saved. */
    public static OnlineShopPublishBox forNewProduct() {
        OnlineShopPublishBox publish = new OnlineShopPublishBox(null);
        publish.box.setToolTipText(NEW_PRODUCT_HINT);
        return publish;
    }

    /** Existing product: call {@link #load(Long)} for each product the form shows. */
    public static OnlineShopPublishBox forProduct(OnlineShopApiClient client) {
        return new OnlineShopPublishBox(client);
    }

    public JCheckBox checkBox() {
        return box;
    }

    /** Reads the product's state from the server; the box stays disabled until it arrives. */
    public void load(Long id) {
        productId = id;
        serverState = null;
        box.setSelected(false);
        box.setEnabled(false);
        if (client == null || id == null) {
            return;
        }
        UIHelper.loadAsync(box, () -> client.isPublished(id), published -> apply(id, published),
                error -> box.setToolTipText(error == null || error.getMessage() == null
                        ? "Não foi possível ler o estado na loja online." : error.getMessage()));
    }

    /** Server answer for {@code id}; ignored when the form already moved to another product. */
    public void apply(Long id, boolean published) {
        if (!Objects.equals(id, productId)) {
            return;
        }
        serverState = published;
        box.setToolTipText(null);
        box.setSelected(published);
        box.setEnabled(true);
    }

    /**
     * Read on the EDT when the form saves: the call to run after the product is saved, or a no-op when the box is
     * unchanged (or its state never arrived).
     */
    public Runnable pendingSave(Long id) {
        if (client == null || serverState == null || !Objects.equals(id, productId)
                || box.isSelected() == serverState) {
            return () -> { };
        }
        boolean published = box.isSelected();
        return () -> client.setPublished(id, published);
    }
}
