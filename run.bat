@echo off
rem Starts DLSCMS. Usage: run.bat [--plain] [--demo] [--script file] [--data folder]
cd /d "%~dp0"
chcp 65001 >nul
if not exist dist\dlscms.jar (
  echo dist\dlscms.jar not found - building ^(needs JDK 17+^)...
  call scripts\build.bat
  if errorlevel 1 exit /b 1
)
java -jar dist\dlscms.jar %*
