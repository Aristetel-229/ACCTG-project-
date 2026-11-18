@echo off
REM ============================================================
REM  TALA Accounting System - compile and run (Windows)
REM  Needs: JDK 11 or newer, MySQL running (e.g. XAMPP),
REM         and mysql-connector-j-<version>.jar inside the lib folder.
REM ============================================================
cd /d "%~dp0"

if not exist lib\*.jar (
    echo.
    echo  The lib folder has no MySQL connector jar yet.
    echo  Download "mysql-connector-j" ^(Platform Independent ZIP^) from
    echo  https://dev.mysql.com/downloads/connector/j/  and put the .jar in lib\
    echo.
    pause
    exit /b 1
)

echo Compiling...
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out -cp "lib/*" @sources.txt
if errorlevel 1 (
    del sources.txt
    echo Compile failed. See the messages above.
    pause
    exit /b 1
)
del sources.txt

echo Starting TALA...
REM src\main\resources holds the logo (images\logo.png)
java -cp "out;src/main/resources;lib/*" tala.Main
