FROM ubuntu:16.04

RUN apt-get update

RUN apt-get upgrade -y -t jessie-backports ca-certificates-java

RUN apt-get install -y python2.7 git maven python-pip mysql-client openjdk-8-jdk && \
    pip install sh && \
    wget http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.39.tar.gz && \
    tar -xvf mysql-connector-java-5.1.39.tar.gz && \
    cp ./mysql-connector-java-5.1.39/mysql-connector-java-5.1.39-bin.jar glassfish/domains/domain1/lib && \
    git clone https://github.com/FIWARE-TMForum/business-ecosystem-rss.git

WORKDIR business-ecosystem-rss

RUN git checkout develop

RUN mvn install -DskipTests; \
    mkdir /etc/default/rss/;

# Set properties files
COPY database.properties /etc/default/rss/database.properties

COPY oauth.properties /etc/default/rss/oauth.properties

COPY ./entrypoint.py /
COPY ./entrypoint.sh /

EXPOSE 8080 4848 8181
ENTRYPOINT ["/entrypoint.sh"]
