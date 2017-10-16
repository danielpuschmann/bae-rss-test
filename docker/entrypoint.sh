#!/bin/bash

export PATH=$PATH:/glassfish4/glassfish/bin
asadmin start-domain

# Check if the properties files have been included
if [ ! -f /etc/default/rss/oauth.properties ]; then
    echo "Missing oauth.properties file"
    exit 1
fi

if [ ! -f /etc/default/rss/database.properties ]; then
    echo "Missing database.properties file"
    exit 1
fi

# Get MySQL info
MYSQL_HOST=`grep -o 'database\.url=.*' /etc/default/rss/database.properties | grep -oE '//.+:' | grep -oE '[^/:]+'`
MYSQL_PORT=`grep -o 'database\.url=.*' /etc/default/rss/database.properties | grep -oE ':[0-9]+/' | grep -oE '[0-9]+'`

# Check if MySQL is running
exec 8<>/dev/tcp/${MYSQL_HOST}/${MYSQL_PORT}
mysqlStatus=$?

i=1
while [[ ${mysqlStatus} -ne 0 && ${i} -lt 50 ]]; do
    echo "MySQL not running, retrying in 5 seconds"
    sleep 5
    i=${i}+1

    exec 8<>/dev/tcp/${MYSQL_HOST}/${MYSQL_PORT}
    mysqlStatus=$?
done

if [[ ${mysqlStatus} -ne 0 ]]; then
    echo "It has not been possible to connect to MySQL"
    exit 1
fi

exec 8>&- # close output connection
exec 8<&- # close input connection

# Deploy RSS war
echo "Deploying WAR file..."
asadmin deploy --force false --contextroot DSRevenueSharing --name DSRevenueSharing ./fiware-rss/target/DSRevenueSharing.war

echo "RSS deployed"
if [[ $1 == "-bash" ]]; then
  /bin/bash
else
  while true; do sleep 1000; done
fi

