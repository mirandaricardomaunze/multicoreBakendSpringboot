@echo off
REM ============================================================
REM  Multicore ERP - arranque do cliente desktop (Swing)
REM  Faz duplo-clique neste ficheiro no Explorador de Ficheiros.
REM  Liberta a porta 8080, arranca o backend e abre a janela de login.
REM ============================================================
title Multicore ERP - Desktop
cd /d "%~dp0"

echo.
echo  [1/3] A libertar a porta 8080 (se estiver ocupada)...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo        a terminar processo %%P que ocupava a porta 8080
    taskkill /F /PID %%P >nul 2>&1
)

echo  [2/3] A verificar o Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo.
    echo  ERRO: 'mvn' nao foi encontrado no PATH.
    echo  Abre um terminal e confirma com:  mvn -version
    echo.
    goto fim
)

echo  [3/3] A arrancar o Multicore ERP...
echo        Aguarde por "Started MulticoreApplication" — a janela de login abre a seguir (~15s).
echo.

call mvn spring-boot:run "-Dspring-boot.run.main-class=com.phcpro.desktop.DesktopApplication"

echo.
echo  ============================================================
echo  A app terminou (ou ocorreu um erro acima).
echo  Se viste um erro, copia as ultimas linhas e mostra ao Claude.
echo  ============================================================
:fim
echo.
pause
