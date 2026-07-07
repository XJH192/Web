# Run this in PowerShell when global java/javac/mvn mapping is broken.
# It fixes only the current PowerShell window, not the whole Windows system.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_201'
$env:MAVEN_HOME = 'D:\IDEA\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3'
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
java -version
javac -version
mvn -version