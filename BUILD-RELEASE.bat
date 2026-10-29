@echo off
cd /d "%~dp0"
where gradle >nul 2>nul
if errorlevel 1 (
 echo Install Gradle 8.13 and add its bin folder to PATH. See README.md.
 exit /b 1
)
gradle --no-daemon :app:signedRelease
