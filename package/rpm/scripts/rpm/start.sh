#!/bin/sh
nohup $JAVA_HOME/bin/java -jar $SERVER_OPTS /opt/bahmni-reports/lib/bahmni-reports.jar >> /var/log/bahmni-reports/bahmni-reports.log 2>&1 &
echo $! > /var/run/bahmni-reports/bahmni-reports.pid
