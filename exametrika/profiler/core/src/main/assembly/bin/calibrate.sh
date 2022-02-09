#!/bin/sh
# ----------------------------------------------------
# Calibration utility launching script
# ---------------------------------------------------

# set EXA_HOME
if [ "x$EXA_HOME" = "x" ]; then
CURRENT=`dirname $0`
EXA_HOME=`cd $CURRENT/..;pwd`
export EXA_HOME
fi

# set java executable
if [ "x$JAVA_HOME" != "x" ]; then 
	JAVA="$JAVA_HOME/bin/java"
else
	JAVA=java
fi

EXA_BOOT_CONFIG=$EXA_HOME/conf/boot.conf
export EXA_BOOT_CONFIG

#DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"

# run program
$JAVA $DEBUG -Dcom.exametrika.home=$EXA_HOME -javaagent:$EXA_HOME/lib/boot.core.jar=,calibrate -jar $EXA_HOME/lib/profiler.boot.jar
