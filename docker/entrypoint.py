#!/usr/bin/env python
# Credits of this code to @Rock_Neurotiko
import time
from os import getenv
import sys, time, socket
from sh import asadmin, cat

rss = {
    "war": "./fiware-rss/target/DSRevenueSharing.war",
    "root": "DSRevenueSharing"
}

DBUSER = getenv("MYSQL_DBUSR", "root")
DBPASSWD = getenv("MYSQL_DBPASSWORD", "toor")
DBHOST = getenv("MYSQL_HOST", "db")
DBPORT = getenv("MYSQL_PORT", "3306")

text = ""
with open("/etc/default/rss/database.properties", "rw") as f:
    text = f.read()

    text.replace("database.url=jdbc:mysql://localhost:3306/RSS", "database.url=jdbc:mysql://{}:{}/RSS".format(DBHOST, DBPORT))\
        .replace("database.username=root", "database.username={}".format(DBUSER))\
        .replace("database.password=root", "database.password={}".format(DBPWD))

    f.write(text)

for i in range(20):
    try:
        time.sleep(1)
        print("Trying to connect to the database:.... ")
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((DBHOST, int(DBPORT)))
        sock.close()
        break
    except:
        continue

print("\nstarted\n")
asadmin("deploy", "--force", "false", "--contextroot", rss.get('root'), "--name", rss.get('root'), rss.get('war'))
