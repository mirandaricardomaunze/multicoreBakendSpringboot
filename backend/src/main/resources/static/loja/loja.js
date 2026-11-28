/*
 * Loja online (LOJA_ONLINE_SPEC §3.2–3.3).
 * - Sem bibliotecas externas; a CSP só permite 'self'.
 * - O carrinho vive no localStorage do browser, só com SKUs e quantidades (nunca preços, nomes nem dados do cliente).
 * - Texto vindo da API entra sempre por textContent, nunca como HTML.
 * - Preços, stock e IVA são sempre os do sistema: o total do carrinho é só uma estimativa.
 */
(function () {
  "use strict";

  var el = function (id) { return document.getElementById(id); };

  var codeMatch = /^\/loja\/([a-z0-9-]+)\/?$/.exec(window.location.pathname);
  var code = codeMatch ? codeMatch[1] : "";
  var base = "/api/shop/" + encodeURIComponent(code);

  var products = [];
  var productsBySku = {};
  var cart = loadCart();

  // ---------- carrinho (só SKU + quantidade) ----------

  function cartKey() {
    return "multicore.loja." + code + ".cart";
  }

  function loadCart() {
    var result = {};
    try {
      var raw = window.localStorage.getItem(cartKey());
      var lines = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(lines)) return result;
      lines.forEach(function (line) {
        if (line && typeof line.sku === "string" && line.sku.length <= 60
            && typeof line.quantity === "number" && isFinite(line.quantity) && line.quantity > 0) {
          result[line.sku] = Math.min(Math.floor(line.quantity), 999);
        }
      });
    } catch (e) {
      // Carrinho ilegível ou armazenamento bloqueado: começa vazio.
    }
    return result;
  }

  function cartLines() {
    return Object.keys(cart).map(function (sku) { return { sku: sku, quantity: cart[sku] }; });
  }

  function saveCart() {
    try {
      window.localStorage.setItem(cartKey(), JSON.stringify(cartLines()));
    } catch (e) {
      // Sem armazenamento: o carrinho dura até fechar a página.
    }
  }

  function clearCart() {
    cart = {};
    try {
      window.localStorage.removeItem(cartKey());
    } catch (e) {
      // nada a limpar
    }
  }

  function setQuantity(sku, quantity) {
    if (quantity > 0) {
      cart[sku] = Math.min(quantity, 999);
    } else {
      delete cart[sku];
    }
    saveCart();
    renderCart();
  }

  // ---------- formatos (como o sistema: 1.460,00 MT) ----------

  function groupThousands(digits) {
    return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  }

  function formatMoney(value) {
    var number = Number(value || 0);
    var negative = number < 0;
    var parts = Math.abs(number).toFixed(2).split(".");
    return (negative ? "-" : "") + groupThousands(parts[0]) + "," + parts[1] + " MT";
  }

  function formatQty(value) {
    var number = Number(value || 0);
    var negative = number < 0;
    var fixed = Math.abs(number).toFixed(3).replace(/0+$/, "").replace(/\.$/, "");
    var parts = fixed.split(".");
    return (negative ? "-" : "") + groupThousands(parts[0]) + (parts[1] ? "," + parts[1] : "");
  }

  // ---------- API ----------

  function api(method, path, body) {
    var headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    return fetch(base + path, {
      method: method,
      headers: headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "omit",
      cache: "no-store"
    }).then(function (response) {
      return response.text().then(function (text) {
        var data = null;
        try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
        if (!response.ok) {
          throw new Error((data && data.message) || "Não foi possível falar com a loja. Tente de novo.");
        }
        return data;
      });
    });
  }

  // ---------- mensagens e elementos ----------

  function showMessage(text, ok) {
    var box = el("message");
    box.textContent = text || "";
    box.classList.toggle("ok", !!ok);
    box.hidden = !text;
    if (text) box.scrollIntoView({ block: "nearest" });
  }

  function node(tag, className, text) {
    var item = document.createElement(tag);
    if (className) item.className = className;
    if (text !== undefined && text !== null) item.textContent = String(text);
    return item;
  }

  function option(value, label) {
    var item = document.createElement("option");
    item.value = value;
    item.textContent = label;
    return item;
  }

  function clear(target) {
    while (target.firstChild) target.removeChild(target.firstChild);
  }

  // ---------- montra ----------

  function renderCategories() {
    var select = el("category");
    var names = [];
    products.forEach(function (product) {
      if (product.category && names.indexOf(product.category) < 0) names.push(product.category);
    });
    names.sort();
    clear(select);
    select.appendChild(option("", "Todas as categorias"));
    names.forEach(function (name) { select.appendChild(option(name, name)); });
  }

  function renderProducts() {
    var list = el("products");
    var term = el("search").value.trim().toLowerCase();
    var category = el("category").value;
    clear(list);
    var shown = products.filter(function (product) {
      var matchesTerm = !term || String(product.name || "").toLowerCase().indexOf(term) >= 0
          || String(product.sku || "").toLowerCase().indexOf(term) >= 0;
      return matchesTerm && (!category || product.category === category);
    });
    shown.forEach(function (product) {
      var card = node("li", "product");
      card.appendChild(node("span", "product-name", product.name));
      if (product.category) card.appendChild(node("span", "product-category", product.category));
      card.appendChild(node("span", "badge " + (product.available ? "available" : "sold-out"),
          product.available ? "Disponível" : "Esgotado"));
      card.appendChild(node("span", "product-price", formatMoney(product.priceWithVat)));
      var add = node("button", null, "Adicionar");
      add.type = "button";
      add.disabled = !product.available;
      add.addEventListener("click", function () {
        setQuantity(product.sku, (cart[product.sku] || 0) + 1);
        showMessage(product.name + " adicionado ao carrinho.", true);
      });
      card.appendChild(add);
      list.appendChild(card);
    });
    el("products-empty").hidden = shown.length > 0;
  }

  function renderCart() {
    var list = el("cart-lines");
    var total = 0;
    var count = 0;
    clear(list);
    cartLines().forEach(function (line) {
      var product = productsBySku[line.sku];
      var item = node("li");
      var row = node("div", "row");
      var label = node("span", null, product ? product.name : line.sku);
      if (!product) {
        label.appendChild(node("span", "sub", "Já não está à venda"));
      } else if (!product.available) {
        label.appendChild(node("span", "sub", "Esgotado"));
      }
      row.appendChild(label);

      var qty = node("span", "qty");
      var minus = node("button", "secondary small", "-");
      minus.type = "button";
      minus.setAttribute("aria-label", "Menos um");
      minus.addEventListener("click", function () { setQuantity(line.sku, line.quantity - 1); });
      var plus = node("button", "secondary small", "+");
      plus.type = "button";
      plus.setAttribute("aria-label", "Mais um");
      plus.disabled = !product || !product.available;
      plus.addEventListener("click", function () { setQuantity(line.sku, line.quantity + 1); });
      qty.appendChild(minus);
      qty.appendChild(node("span", null, formatQty(line.quantity)));
      qty.appendChild(plus);
      row.appendChild(qty);
      item.appendChild(row);
      list.appendChild(item);

      count += line.quantity;
      if (product) total += Number(product.priceWithVat || 0) * line.quantity;
    });
    if (!list.firstChild) list.appendChild(node("li", "empty", "O carrinho está vazio."));
    el("cart-total").textContent = count > 0 ? "Total estimado: " + formatMoney(total) : "";
    el("cart-count").textContent = "Carrinho (" + formatQty(count) + ")";
  }

  // ---------- loja e checkout ----------

  function renderStorefront(shop) {
    el("shop-name").textContent = shop.name || "Loja online";
    document.title = (shop.name || "Loja") + " · Loja online";
    el("shop-contact").textContent = shop.contactPhone ? "Contacto: " + shop.contactPhone : "";

    var pickup = el("pickup");
    clear(pickup);
    (shop.pickupStores || []).forEach(function (store) {
      pickup.appendChild(option(String(store.id), store.name));
    });

    var zones = el("zone");
    clear(zones);
    (shop.deliveryZones || []).forEach(function (zone) {
      zones.appendChild(option(zone.name, zone.name + " — " + formatMoney(zone.fee)));
    });

    var fulfilment = el("fulfilment");
    fulfilment.options[0].hidden = !pickup.options.length;
    fulfilment.options[1].hidden = !zones.options.length;
    if (!pickup.options.length && zones.options.length) fulfilment.value = "ENTREGA";
    toggleFulfilment();
  }

  function toggleFulfilment() {
    var delivery = el("fulfilment").value === "ENTREGA";
    el("pickup-fields").hidden = delivery;
    el("delivery-fields").hidden = !delivery;
  }

  function placeOrder(event) {
    event.preventDefault();
    if (!cartLines().length) {
      showMessage("O carrinho está vazio.");
      return;
    }
    var delivery = el("fulfilment").value === "ENTREGA";
    var submit = el("checkout-submit");
    submit.disabled = true;
    api("POST", "/orders", {
      customerName: el("customer-name").value,
      phone: el("phone").value,
      nuit: el("nuit").value || null,
      fulfilment: el("fulfilment").value,
      pickupWarehouseId: delivery ? null : Number(el("pickup").value) || null,
      deliveryZone: delivery ? el("zone").value : null,
      deliveryAddress: delivery ? el("address").value : null,
      notes: el("notes").value || null,
      lines: cartLines(),
      website: el("website").value
    }).then(function (order) {
      clearCart();
      renderCart();
      renderOrder(order);
      el("lookup-number").value = order.number || "";
      el("lookup-phone").value = el("phone").value;
      showMessage("Encomenda " + order.number + " registada. Paga ao levantar ou receber.", true);
    }).catch(function (error) {
      showMessage(error.message);
      loadProducts();
    }).then(function () {
      submit.disabled = false;
    });
  }

  function lookupOrder(event) {
    event.preventDefault();
    var submit = el("lookup-submit");
    submit.disabled = true;
    api("POST", "/orders/lookup", {
      number: el("lookup-number").value.trim(),
      phone: el("lookup-phone").value.trim()
    }).then(function (order) {
      showMessage("");
      renderOrder(order);
    }).catch(function (error) {
      el("order-view").hidden = true;
      showMessage(error.message);
    }).then(function () {
      submit.disabled = false;
    });
  }

  function renderOrder(order) {
    el("order-number").textContent = order.number || "";
    el("order-status").textContent = order.statusLabel ? "Estado: " + order.statusLabel : "";
    var list = el("order-lines");
    clear(list);
    (order.lines || []).forEach(function (line) {
      var row = node("div", "row");
      row.appendChild(node("span", null, line.name || line.sku));
      row.appendChild(node("span", null, formatQty(line.quantity)));
      var item = node("li");
      item.appendChild(row);
      list.appendChild(item);
    });
    el("order-total").textContent = "Total: " + formatMoney(order.totalAmount);
    el("order-view").hidden = false;
    el("order-view").scrollIntoView({ block: "nearest" });
  }

  // ---------- arranque ----------

  function loadProducts() {
    return api("GET", "/products").then(function (list) {
      products = (list || []).slice().sort(function (a, b) {
        var byCategory = String(a.category || "").localeCompare(String(b.category || ""));
        return byCategory || String(a.name || "").localeCompare(String(b.name || ""));
      });
      productsBySku = {};
      products.forEach(function (product) { productsBySku[product.sku] = product; });
      var selected = el("category").value;
      renderCategories();
      el("category").value = selected;
      renderProducts();
      renderCart();
    });
  }

  function start() {
    el("search").addEventListener("input", renderProducts);
    el("category").addEventListener("change", renderProducts);
    el("fulfilment").addEventListener("change", toggleFulfilment);
    el("checkout-form").addEventListener("submit", placeOrder);
    el("lookup-form").addEventListener("submit", lookupOrder);

    if (!code) {
      showMessage("Loja indisponível.");
      return;
    }
    api("GET", "").then(function (shop) {
      renderStorefront(shop);
      el("shop-view").hidden = false;
      return loadProducts();
    }).catch(function (error) {
      showMessage(error.message);
    });
  }

  start();
})();
