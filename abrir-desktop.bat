@echo off
title Multicore Desktop ERP
cd /d "%~dp0"

set "JAVA_EXE=C:\Users\miran\.jdks\ms-21.0.10\bin\javaw.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=javaw"

set "JAR_PATH=%~dp0desktop\target\multicore-desktop-1.0.0.jar"

if exist "%JAR_PATH%" (
    start "" "%JAVA_EXE%" -Djava.awt.headless=false -Dspring.profiles.active=desktop -jar "%JAR_PATH%"
    exit /b 0
)

rem Fallback Maven se o JAR nao existir
set "JAVA_HOME=C:\Users\miran\.jdks\ms-21.0.10"
set "PATH=C:\Program Files\apache-maven-3.9.14\bin;%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0\desktop"
call mvn spring-boot:run
