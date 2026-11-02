@echo off
rem Abre o desktop ligado a base de revisao (porta 18099), criada por scripts\empresa-exemplo.py.
set DESKTOP_API_BASE_URL=http://localhost:18099
call "%~dp0run_gui.bat"
