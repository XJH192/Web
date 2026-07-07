@echo off
setlocal
set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%blog-system\backend"
set "FRONTEND_DIR=%ROOT%demo-site"

if "%MYSQL_USER%"=="" set "MYSQL_USER=root"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=root"

echo ===============================================
echo xjh Blog System
echo Backend:  http://127.0.0.1:8080/api
echo Frontend: http://127.0.0.1:4000/login.html
echo Database: mydataset
echo ===============================================
echo.
echo If your MySQL password is not root, close this window and run:
echo   set MYSQL_PASSWORD=your_password
echo   npm run start:all
echo.
echo Make sure you have run this SQL in Navicat first:
echo   %ROOT%blog-system\database\mydataset_navicat.sql
echo.

start "xjh Backend 8080" /D "%BACKEND_DIR%" cmd /k run-backend.cmd

timeout /t 8 /nobreak >nul

start "xjh Frontend 4000" /D "%FRONTEND_DIR%" cmd /k "npm run clean && npm run generate && npm run server"

timeout /t 5 /nobreak >nul
start "" "http://127.0.0.1:4000/login.html"

endlocal