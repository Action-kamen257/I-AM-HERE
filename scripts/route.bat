@echo off
rem Antigravity Auto Router CLI Dispatcher
set VENV_PY=D:\AI\Antigravity-Local\venv\Scripts\python.exe
if not exist "%VENV_PY%" (
    set VENV_PY=python
)
"%VENV_PY%" "%~dp0model_router.py" %*
