#!/usr/bin/env sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
JAVA_OPTS=""
exec java $JAVA_OPTS -jar $CLASSPATH "$@"
