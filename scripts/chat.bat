@echo off
rem Launch Local GLM-4.7-Flash Interactive Terminal Chat
set VENV_PY=D:\AI\Antigravity-Local\venv\Scripts\python.exe
if not exist "%VENV_PY%" (
    set VENV_PY=python
)
"%VENV_PY%" "%~dp0interactive_chat.py"
