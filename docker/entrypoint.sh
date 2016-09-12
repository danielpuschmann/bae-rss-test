#!/bin/bash
asadmin start-domain

cat /etc/default/rss/database.properties

python /entrypoint.py


while true; do sleep 1000; done


if [[ $1 == "-bash" ]]; then
  /bin/bash
fi
