# Swing Desktop + Spring Boot Backend

This project is being separated into two runtime entrypoints while the codebase is migrated gradually.

## Runtime entrypoints

- Backend/API server:
  `com.phcpro.MulticoreApplication`

- Desktop Swing client:
  `com.phcpro.desktop.DesktopApplication`

## Current state

The backend entrypoint starts Spring Boot without opening Swing windows. This is the entrypoint intended for online deployment.

The desktop entrypoint starts Spring Boot with the `desktop` profile and then opens the Swing login and main window. This preserves the current local workflow while the desktop is migrated toward HTTP API clients.

## Target architecture

```text
Installed Swing desktop app
  -> HTTPS API
  -> Spring Boot backend online
  -> PostgreSQL online
```

Swing should keep UI, local printing, scanner input, and local preferences. Spring Boot should own business rules, authentication, permissions, persistence, stock, sales, fiscal logic, reporting, audit logs, and backups.

## Next migration steps

1. Add a desktop API configuration for the backend URL.
2. Add an HTTP client layer under `com.phcpro.desktop.client`.
3. Convert login to call a backend authentication endpoint.
4. Convert product and stock reads to API calls.
5. Convert POS checkout and stock movements to API calls.
6. Move the desktop client into its own Maven module or repository after the API boundary is stable.
