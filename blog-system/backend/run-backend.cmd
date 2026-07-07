@echo off
setlocal
if "%MYSQL_USER%"=="" set "MYSQL_USER=root"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=root"
call "%~dp0mvn-local.cmd" spring-boot:run
endlocal