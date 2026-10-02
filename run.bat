@echo off
setlocal
cd /d "%~dp0"
if not exist dist\mathpath.jar (
  call build.bat
  if errorlevel 1 exit /b 1
)
java -jar dist\mathpath.jar
