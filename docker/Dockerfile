FROM ubuntu:16.04

MAINTAINER CoNWeT Lab. Universidad Politécnica de Madrid

ENV VERSION develop

RUN apt-get update && apt-get install -y python2.7 git maven python-pip mysql-client openjdk-8-jdk wget unzip && \
    pip install sh && \
    wget http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip && \
    unzip glassfish-4.1.zip && \
    wget http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.39.tar.gz && \
    tar -xvf mysql-connector-java-5.1.39.tar.gz && \
    cp ./mysql-connector-java-5.1.39/mysql-connector-java-5.1.39-bin.jar glassfish4/glassfish/domains/domain1/lib && \
    git clone https://github.com/FIWARE-TMForum/business-ecosystem-rss.git

WORKDIR business-ecosystem-rss

RUN git checkout $VERSION

RUN mvn install -DskipTests && \
    mkdir /etc/default/rss/

# Set volume for property files
VOLUME /etc/default/rss

COPY ./entrypoint.py /
COPY ./entrypoint.sh /

EXPOSE 8080 4848 8181
ENTRYPOINT ["/entrypoint.sh"]
