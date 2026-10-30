REM Turn off echoing of each command so the console stays clean.
@echo off
REM Set the console window title to TALA.
title TALA
REM Move into the folder where this .bat file lives (/d also switches drive) so Maven finds pom.xml.
cd /d "%~dp0"
REM Quietly (-q) clean old builds, compile the code, then run the main class through the exec plugin (see pom.xml).
mvn -q clean compile exec:java
REM Keep the window open after the app closes so errors can be read.
pause
