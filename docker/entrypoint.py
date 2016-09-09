#!/usr/bin/env python
# Credits of this code to @Rock_Neurotiko
from os import getenv
import sys, time, socket
from sh import asadmin, cat

rss = {"url": "https://github.com/FIWARE-TMForum/business-ecosystem-rss.git",
       "branch": "develop",
       "bbdd": "RSS",
       "war": "./fiware-rss/target/DSRevenueSharing.war",
       "name": "rss",
       "root": "DSRevenueSharing"}

if getenv("RSS_CLIENT_ID") is None:
    print("RSS_CLIENT_ID is not set")
    sys.exit()

if getenv("RSS_SECRET") is None:
    print("RSS_SECRET is not set")
    sys.exit()

if getenv("RSS_URL") is None:
    print("RSS_URL is not set")
    sys.exit()

text = ""
with open("/etc/default/rss/oauth.properties") as f:
    text = f.read()
text = text.replace("config.client_id=", "config.client_id={}".format(getenv("RSS_CLIENT_ID")))
text = text.replace("config.client_secret=", "config.client_secret={}".format(getenv("RSS_SECRET")))
text = text.replace("config.callbackURL=", "config.callbackURL={}/fiware-rss/callback".format(getenv('RSS_URL')))
with open("/etc/default/rss/oauth.properties", "w") as f:
    f.write(text)

while True:
    try:
        print("Intentado conectar:.... ")
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        host = 'db'
        port = 3306
        sock.connect((host, int(port)))
        sock.close()
        break
    except:
        continue

print("\nstarted\n")
#try:
asadmin("deploy", "--force", "false", "--contextroot", rss.get('root'), "--name", rss.get('root'), rss.get('war'))
#except:
#    pass
#with open("{}/glassfish/domains/domain1/logs/server.log".format(getenv("GLASSFISH_HOME"))) as f:
#    print(f.read())
