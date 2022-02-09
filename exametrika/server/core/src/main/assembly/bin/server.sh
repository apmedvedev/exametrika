#!/bin/sh
# ----------------------------------------------------
# Server launching script
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

EXA_BOOT_CONFIG=$EXA_HOME/conf/server.conf
export EXA_BOOT_CONFIG

#TODO: удалить
DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"

# run program
$JAVA -Xmx4g $DEBUG -Dcom.exametrika.home=$EXA_HOME -jar $EXA_HOME/lib/boot.core.jar $EXA_HOME/conf/exametrika-server.conf server
