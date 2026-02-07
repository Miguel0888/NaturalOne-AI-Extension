@ECHO OFF
SETLOCAL

SET "BASEDIR=%~dp0"
REM Remove trailing backslash
IF "%BASEDIR:~-1%"=="\" SET "BASEDIR=%BASEDIR:~0,-1%"

SET "WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Downloading Maven wrapper...
  PUSHD "%BASEDIR%"
  "%JAVA_HOME%\bin\javac.exe" "%BASEDIR%\.mvn\wrapper\MavenWrapperDownloader.java" 2>NUL
  IF ERRORLEVEL 1 (
    javac "%BASEDIR%\.mvn\wrapper\MavenWrapperDownloader.java"
  )
  "%JAVA_HOME%\bin\java.exe" -cp "%BASEDIR%\.mvn\wrapper" MavenWrapperDownloader "%BASEDIR%" 2>NUL
  IF ERRORLEVEL 1 (
    java -cp "%BASEDIR%\.mvn\wrapper" MavenWrapperDownloader "%BASEDIR%"
  )
  POPD
)

java -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
ENDLOCAL
