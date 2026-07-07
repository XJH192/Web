@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_201"
set "MAVEN_HOME=D:\IDEA\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3"
set "MAVEN_REPO=%~dp0.m2-repository"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo JAVA_HOME=%JAVA_HOME%
echo MAVEN_HOME=%MAVEN_HOME%
echo MAVEN_REPO=%MAVEN_REPO%
java -version
javac -version
call "%MAVEN_HOME%\bin\mvn.cmd" -Dmaven.repo.local="%MAVEN_REPO%" -version
endlocal