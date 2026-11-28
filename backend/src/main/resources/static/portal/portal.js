/*
 * Portal do Gestor (PORTAL_GESTOR_SPEC §3).
 * - Sem bibliotecas externas; a CSP só permite 'self'.
 * - O token vive em sessionStorage: fechar o separador termina a sessão no browser.
 * - Texto vindo da API entra sempre por textContent, nunca como HTML.
 */
(function () {
  "use strict";

  var STORAGE_KEY = "multicore.portal.session";
  var REFRESH_MS = 5 * 60 * 1000;

  var session = loadSession();
  var pendingLogin = null; // utilizador/senha enquanto se espera o código 2FA
  var refreshTimer = null;
  var lastRefreshAt = 0; // hora (ms) da última actualização bem-sucedida do painel

  var el = function (id) { return document.getElementById(id); };

  // ---------- sessão ----------

  function loadSession() {
    try {
      var raw = window.sessionStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function saveSession(value) {
    session = value;
    try {
      if (value) {
        window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
      } else {
        window.sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch (e) {
      // Sem armazenamento de sessão: o portal funciona até fechar ou recarregar a página.
    }
  }

  // ---------- formatos (como o sistema: 1.460,00 MT e dd/mm/aaaa) ----------

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

  function formatDate(iso) {
    if (!iso) return "";
    var match = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(String(iso));
    if (!match) return String(iso);
    var date = match[3] + "/" + match[2] + "/" + match[1];
    return match[4] ? date + " " + match[4] + ":" + match[5] : date;
  }

  // ---------- API ----------

  function api(method, path, body) {
    var headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (session && session.token) headers["Authorization"] = "Bearer " + session.token;
    if (session && session.companyId) headers["X-Company-Id"] = String(session.companyId);

    return fetch(path, {
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
          var error = new Error((data && data.message) || defaultError(response.status));
          error.status = response.status;
          throw error;
        }
        return data;
      });
    });
  }

  function defaultError(status) {
    if (status === 401) return "A sessão terminou. Entre de novo.";
    if (status === 403) return "Sem permissão para esta operação.";
    return "Não foi possível falar com o servidor. Tente de novo.";
  }

  function handleError(error) {
    if (error && error.status === 401) {
      endSession(error.message);
      return;
    }
    showMessage(error && error.message ? error.message : defaultError(0));
  }

  // ---------- mensagens e vistas ----------

  function showMessage(text, ok) {
    var box = el("message");
    box.textContent = text || "";
    box.classList.toggle("ok", !!ok);
    box.hidden = !text;
  }

  function showLogin() {
    stopRefresh();
    el("app-view").hidden = true;
    el("login-view").hidden = false;
    el("logout").hidden = true;
    el("who").textContent = "";
    el("code-field").hidden = !pendingLogin;
    el("password").value = "";
    el("code").value = "";
  }

  function showApp() {
    el("login-view").hidden = true;
    el("app-view").hidden = false;
    el("logout").hidden = false;
    el("who").textContent = session.displayName || session.username || "";

    var select = el("company");
    select.textContent = "";
    session.companies.forEach(function (company) {
      var option = document.createElement("option");
      option.value = String(company.id);
      option.textContent = company.name;
      option.selected = company.id === session.companyId;
      select.appendChild(option);
    });
    select.disabled = session.companies.length < 2;

    refresh();
    startRefresh();
  }

  function startRefresh() {
    stopRefresh();
    refreshTimer = window.setInterval(function () {
      if (document.visibilityState === "visible") refresh();
    }, REFRESH_MS);
  }

  function stopRefresh() {
    if (refreshTimer) window.clearInterval(refreshTimer);
    refreshTimer = null;
  }

  // Telemóvel bloqueado ou separador escondido não actualiza; ao voltar, se o painel já tem
  // 5 minutos ou mais, actualiza logo em vez de esperar pelo próximo ciclo.
  function onVisibilityChange() {
    if (document.visibilityState !== "visible" || !refreshTimer) return;
    if (Date.now() - lastRefreshAt >= REFRESH_MS) refresh();
  }

  function endSession(message) {
    saveSession(null);
    pendingLogin = null;
    showLogin();
    showMessage(message || "");
  }

  // ---------- entrar / sair ----------

  function onLogin(event) {
    event.preventDefault();
    var username = pendingLogin ? pendingLogin.username : el("username").value.trim();
    var password = pendingLogin ? pendingLogin.password : el("password").value;
    var code = el("code").value.trim();
    if (pendingLogin && !code) {
      showMessage("Escreva o código de verificação.");
      return;
    }

    var submit = el("login-submit");
    submit.disabled = true;
    showMessage("");
    api("POST", "/api/auth/login", {
      username: username,
      password: password,
      totpCode: pendingLogin ? code : null,
      channel: "PORTAL"
    }).then(function (response) {
      if (response.twoFactorRequired) {
        pendingLogin = { username: username, password: password };
        el("code-field").hidden = false;
        el("code").value = "";
        el("code").focus();
        showMessage("Escreva o código da aplicação de verificação.", true);
        return;
      }
      pendingLogin = null;
      if (response.passwordChangeRequired) {
        session = { token: response.token };
        logoutQuietly();
        showMessage("Mude a senha no programa da loja antes de usar o portal.");
        return;
      }
      var companies = response.companies || [];
      saveSession({
        token: response.token,
        username: response.username,
        displayName: response.displayName,
        companies: companies,
        companyId: companies.length ? companies[0].id : null
      });
      showApp();
    }).catch(function (error) {
      pendingLogin = null;
      el("code-field").hidden = true;
      handleError(error);
    }).then(function () {
      submit.disabled = false;
    });
  }

  function logoutQuietly() {
    if (!session || !session.token) return Promise.resolve();
    return api("POST", "/api/auth/logout").catch(function () { /* a sessão expira sozinha */ });
  }

  function onLogout() {
    logoutQuietly().then(function () { endSession("Sessão terminada."); });
  }

  function onCompanyChange() {
    session.companyId = Number(el("company").value);
    saveSession(session);
    refresh();
  }

  // ---------- painel ----------

  function refresh() {
    if (!session || !session.companyId) return;
    var button = el("refresh");
    button.disabled = true;
    showMessage("");
    Promise.all([
      api("GET", "/api/portal/summary?companyId=" + encodeURIComponent(session.companyId)),
      api("GET", "/api/approvals/pending")
    ]).then(function (results) {
      renderSummary(results[0]);
      renderApprovals(results[1] || []);
      lastRefreshAt = Date.now();
      el("updated-at").textContent = "Actualizado às " + new Date().toTimeString().slice(0, 5)
          + " · actualiza sozinho a cada 5 minutos";
    }).catch(handleError).then(function () {
      button.disabled = false;
    });
  }

  function item(main, side, sub) {
    var li = document.createElement("li");
    var row = document.createElement("div");
    row.className = "row";
    var left = document.createElement("span");
    left.textContent = main;
    var right = document.createElement("span");
    right.textContent = side || "";
    row.appendChild(left);
    row.appendChild(right);
    li.appendChild(row);
    if (sub) {
      var small = document.createElement("span");
      small.className = "sub";
      small.textContent = sub;
      li.appendChild(small);
    }
    return li;
  }

  function fillList(id, rows, emptyText, build) {
    var list = el(id);
    list.textContent = "";
    if (!rows || rows.length === 0) {
      var li = document.createElement("li");
      li.className = "empty";
      li.textContent = emptyText;
      list.appendChild(li);
      return;
    }
    rows.forEach(function (row) { list.appendChild(build(row)); });
  }

  function renderSummary(summary) {
    el("sales-today").textContent = formatMoney(summary.salesToday);
    el("sales-month").textContent = formatMoney(summary.salesMonth);
    el("overdue-total").textContent = formatMoney(summary.overdueTotal);
    el("pending-count").textContent = String(summary.pendingApprovals || 0);

    fillList("sales-by-store", summary.salesByStore, "Sem vendas este mês.", function (store) {
      return item(store.store, formatMoney(store.month), "Hoje: " + formatMoney(store.today));
    });
    fillList("open-tills", summary.openTills, "Nenhuma caixa aberta.", function (till) {
      return item(till.operator || "Sem operador", "desde " + formatDate(till.openedAt));
    });
    fillList("low-stock", summary.lowStock, "Nenhum artigo abaixo do mínimo.", function (stock) {
      return item(stock.name, formatQty(stock.quantity) + " / mín. " + formatQty(stock.minStock),
          [stock.sku, stock.warehouse].filter(Boolean).join(" · "));
    });
    fillList("overdue-clients", summary.overdueClients, "Sem facturas vencidas.", function (client) {
      return item(client.clientName, formatMoney(client.amount),
          client.invoices + (client.invoices === 1 ? " factura" : " facturas")
              + " · até " + client.maxDaysOverdue + " dias de atraso");
    });
  }

  // ---------- aprovações ----------

  function renderApprovals(approvals) {
    fillList("approvals", approvals, "Nenhum pedido pendente.", function (approval) {
      var li = item(approval.documentType + (approval.description ? " · " + approval.description : ""),
          formatMoney(approval.amount),
          "Pedido por " + (approval.submitter || "-") + " em " + formatDate(approval.createdAt));

      var actions = document.createElement("div");
      actions.className = "actions";
      var approve = button("Aprovar", "", function () { decide(approval.id, "approve", null, li); });
      var reject = button("Rejeitar", "secondary", function () { toggleRejectForm(approval.id, li); });
      actions.appendChild(approve);
      actions.appendChild(reject);
      li.appendChild(actions);
      return li;
    });
  }

  function button(label, className, onClick) {
    var b = document.createElement("button");
    b.type = "button";
    b.textContent = label;
    if (className) b.className = className;
    b.addEventListener("click", onClick);
    return b;
  }

  function toggleRejectForm(id, li) {
    var existing = li.querySelector(".reject-form");
    if (existing) {
      existing.remove();
      return;
    }
    var form = document.createElement("form");
    form.className = "reject-form";
    var label = document.createElement("label");
    label.textContent = "Motivo da rejeição";
    var reason = document.createElement("textarea");
    reason.maxLength = 500;
    reason.required = true;
    label.appendChild(reason);
    form.appendChild(label);
    var confirm = document.createElement("button");
    confirm.type = "submit";
    confirm.className = "danger";
    confirm.textContent = "Confirmar rejeição";
    form.appendChild(confirm);
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var text = reason.value.trim();
      if (!text) {
        showMessage("Escreva o motivo da rejeição.");
        return;
      }
      decide(id, "reject", text, li);
    });
    li.appendChild(form);
    reason.focus();
  }

  function decide(id, action, reason, li) {
    var buttons = li.querySelectorAll("button");
    buttons.forEach(function (b) { b.disabled = true; });
    var body = action === "reject" ? { comments: reason } : { comments: "Aprovado no portal do gestor." };
    api("POST", "/api/approvals/" + encodeURIComponent(id) + "/" + action, body).then(function () {
      refresh();
      showMessage(action === "reject" ? "Pedido rejeitado." : "Pedido aprovado.", true);
    }).catch(function (error) {
      buttons.forEach(function (b) { b.disabled = false; });
      handleError(error);
    });
  }

  // ---------- arranque ----------

  document.addEventListener("DOMContentLoaded", function () {
    el("login-form").addEventListener("submit", onLogin);
    el("logout").addEventListener("click", onLogout);
    el("refresh").addEventListener("click", refresh);
    el("company").addEventListener("change", onCompanyChange);
    document.addEventListener("visibilitychange", onVisibilityChange);

    if (session && session.token && session.companies && session.companies.length) {
      showApp();
    } else {
      showLogin();
    }
  });
})();
