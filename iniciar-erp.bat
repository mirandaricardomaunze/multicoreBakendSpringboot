@echo off
title Multicore ERP - Launcher
cd /d "%~dp0"

rem Garantir que JAVA_HOME e Maven estão no PATH
set "JAVA_HOME=C:\Users\miran\.jdks\ms-21.0.10"
set "PATH=C:\Program Files\apache-maven-3.9.14\bin;%JAVA_HOME%\bin;%PATH%"

echo ========================================================
echo             MULTICORE ERP - ARRANQUE LOCAL
echo ========================================================
echo.

where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Maven nao encontrado no PATH do sistema.
    echo Caminho esperado: C:\Program Files\apache-maven-3.9.14\bin
    echo.
    pause
    exit /b 1
)

echo [1/2] A verificar estado do Backend (porta 8080)...
curl -s http://localhost:8080/actuator/health | findstr "UP" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Backend ja se encontra activo na porta 8080!
) else (
    echo       A iniciar o Servidor Backend numa janela dedicada...
    start "Multicore Backend (Porta 8080)" cmd /k "cd /d %~dp0backend && set JAVA_HOME=%JAVA_HOME% && set PATH=%PATH% && mvn spring-boot:run"
    echo       A aguardar arranque dos servicos e base de dados...

    :WAIT_LOOP
    ping 127.0.0.1 -n 4 >nul
    curl -s http://localhost:8080/actuator/health | findstr "UP" >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo       A carregar Hibernate, Flyway e repositorios...
        goto WAIT_LOOP
    )
    echo       Backend pronto e operacional!
)

echo.
echo [2/2] A iniciar a aplicacao Desktop (Interface Grafica Swing)...
set "JAR_PATH=%~dp0desktop\target\multicore-desktop-1.0.0.jar"
if exist "%JAR_PATH%" (
    start "" "C:\Users\miran\.jdks\ms-21.0.10\bin\javaw.exe" -jar "%JAR_PATH%"
    echo       Aplicacao iniciada com sucesso!
    exit /b 0
)

cd /d "%~dp0\desktop"
call mvn spring-boot:run

echo.
echo ========================================================
echo   Aplicacao encerrada.
echo ========================================================
echo.
pause
