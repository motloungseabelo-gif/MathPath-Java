@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build\classes -sourcepath src\test\java -d build\test-classes src\test\java\com\mathpath\TestSuite.java src\test\java\com\mathpath\UiTest.java src\test\java\com\mathpath\DesktopSmokeTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp build\classes;build\test-classes com.mathpath.TestSuite
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp build\classes;build\test-classes com.mathpath.UiTest
if errorlevel 1 exit /b 1
if /i "%~1"=="--desktop" java -cp dist\mathpath.jar;build\test-classes com.mathpath.DesktopSmokeTest
