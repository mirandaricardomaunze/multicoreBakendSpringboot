@echo off
title Multicore ERP - Desktop
cd /d "%~dp0\desktop"

rem Garantir que JAVA_HOME e Maven estão no PATH
if not defined JAVA_HOME set "JAVA_HOME=C:\Users\miran\.jdks\ms-21.0.10"
if exist "C:\Program Files\apache-maven-3.9.14\bin" set "PATH=C:\Program Files\apache-maven-3.9.14\bin;%JAVA_HOME%\bin;%PATH%"

echo ========================================================
echo   MULTICORE ERP - INICIANDO APLICACAO DESKTOP
echo ========================================================
echo Java: %JAVA_HOME%
echo.

where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Maven nao encontrado no PATH do sistema.
    echo Caminho esperado: C:\Program Files\apache-maven-3.9.14\bin
    echo.
    pause
    exit /b 1
)

call mvn spring-boot:run

echo.
echo ========================================================
echo   Aplicacao Desktop encerrada.
echo ========================================================
echo.
pause
