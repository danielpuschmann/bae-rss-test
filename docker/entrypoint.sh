#!/bin/bash

export PATH=$PATH:/glassfish4/glassfish/bin
asadmin start-domain

python /entrypoint.py

if [[ $1 == "-bash" ]]; then
  /bin/bash
else
  while true; do sleep 1000; done
fi

