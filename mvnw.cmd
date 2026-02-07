@ECHO OFF
SETLOCAL

REM Resolve project base dir (directory of this script)
SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_PROPS=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"
SET "WRAPPER_DOWNLOADER=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\MavenWrapperDownloader.java"

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Downloading Maven wrapper...
  "%JAVA_HOME%\bin\java.exe" -cp "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" MavenWrapperDownloader "%WRAPPER_PROPS%"
)

"%JAVA_HOME%\bin\java.exe" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  -classpath "%WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

ENDLOCAL
