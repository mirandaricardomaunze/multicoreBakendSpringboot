# Procedimento de Arranque da Aplicação Multicore ERP

## Como abrir a aplicação

A aplicação tem **dois componentes** que têm de estar a correr em simultâneo:
1. **Backend** (servidor Spring Boot na porta 8080)
2. **Desktop** (cliente Swing — abre a janela de login)

---

## Opção A — Atalho no Ambiente de Trabalho (recomendado para o utilizador)

Dar duplo-clique no ficheiro:
```
C:\Users\miran\Desktop\Multicore ERP.bat
```
ou no atalho `Multicore ERP.lnk` no Desktop.

Este ficheiro:
1. Verifica se o backend já está ativo em `http://localhost:8080/actuator/health`
2. Se não estiver, lança o backend automaticamente e aguarda
3. Lança o desktop JAR com `javaw.exe` (sem consola preta)

---

## Opção B — Lançamento manual por PowerShell (para o agente usar)

### Passo 1 — Verificar se o backend está UP
```powershell
curl.exe -s http://localhost:8080/actuator/health
# Resultado esperado: {"status":"UP"}
```

### Passo 2 — Se não estiver UP, iniciar o backend
```powershell
# Executar como daemon (em background)
mvn spring-boot:run -pl backend
# Aguardar até ver: "Started MulticoreApplication in X seconds"
```
- JDK: `C:\Users\miran\.jdks\ms-21.0.10`
- Maven: `C:\Program Files\apache-maven-3.9.14\bin`
- Backend PID fica registado no log de tarefa

### Passo 3 — Lançar o Desktop
```powershell
Start-Process -FilePath "C:\Users\miran\.jdks\ms-21.0.10\bin\java.exe" `
    -ArgumentList "-jar", "C:\Users\miran\Desktop\manager\desktop\target\multicore-desktop-1.0.0.jar"
```
- A janela de login abre em ~2 segundos
- Usar os chips de demo: **Admin (Total)**, **Maria (Gestão)**, **João (Caixa)**, **Ana (RH)**

---

## Ficheiros relevantes

| Ficheiro | Descrição |
|---|---|
| `C:\Users\miran\Desktop\Multicore ERP.bat` | Launcher completo (backend + desktop) |
| `C:\Users\miran\Desktop\manager\abrir-desktop.bat` | Só lança o desktop (assume backend UP) |
| `C:\Users\miran\Desktop\manager\desktop\target\multicore-desktop-1.0.0.jar` | Fat JAR do desktop |

---

## Credenciais de demonstração

| Utilizador | Senha | Papel |
|---|---|---|
| `admin` | `admin` | Admin Total |
| `maria` | `maria` | Gestão |
| `joao` | `joao` | Caixa |
| `ana` | `ana` | RH |

---

## Notas técnicas importantes

- O **JAR do desktop precisa de ser re-empacotado** após alterações ao código:
  ```powershell
  mvn package -pl desktop -DskipTests
  ```
- O backend usa **H2 em memória** — os dados reiniciam a cada arranque do backend
- O backend **não precisa de PostgreSQL** em desenvolvimento (H2 automático)
- O desktop comunica com o backend via HTTP em `http://localhost:8080`
