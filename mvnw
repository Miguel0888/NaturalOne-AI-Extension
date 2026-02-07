#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.2.0
# ----------------------------------------------------------------------------
BASEDIR=$(cd "$(dirname "$0")"; pwd -P)

WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Maven wrapper..."
  (cd "$BASEDIR" && javac "$BASEDIR/.mvn/wrapper/MavenWrapperDownloader.java" && java -cp "$BASEDIR/.mvn/wrapper" MavenWrapperDownloader "$BASEDIR")
fi

exec java -classpath "$WRAPPER_JAR" $WRAPPER_LAUNCHER "$@"
