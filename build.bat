@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Install JDK 17 or newer and add its bin directory to PATH.
  exit /b 1
)
if not exist build\classes mkdir build\classes
if not exist dist mkdir dist
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -sourcepath src\main\java -d build\classes src\main\java\com\mathpath\MathPath.java
if errorlevel 1 exit /b 1
jar --create --file dist\mathpath.jar --main-class com.mathpath.MathPath -C build\classes .
if errorlevel 1 exit /b 1
echo Built dist\mathpath.jar
