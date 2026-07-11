@echo off
setlocal
set "SCRIPT_DIR=%~dp0"

if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_201"
if "%MAVEN_HOME%"=="" set "MAVEN_HOME=D:\IDEA\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3"
set "MAVEN_REPO=%SCRIPT_DIR%.m2-repository"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo [ERROR] JDK not found: %JAVA_HOME%
  echo [TIP] Set JAVA_HOME to your JDK path, for example: setx JAVA_HOME "D:\java\JDK"
  exit /b 1
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo [ERROR] Maven not found: %MAVEN_HOME%
  echo [TIP] Install Maven, or set MAVEN_HOME to IntelliJ bundled Maven path.
  exit /b 1
)

if not exist "%MAVEN_REPO%" mkdir "%MAVEN_REPO%"
pushd "%SCRIPT_DIR%"
call "%MAVEN_HOME%\bin\mvn.cmd" -Dmaven.repo.local="%MAVEN_REPO%" %*
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%