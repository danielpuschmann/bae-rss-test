FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y unzip wget python2.7 python-pip git maven mysql-client openjdk-8-jdk

# Download and install glassfish
RUN wget http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip
RUN unzip glassfish-4.1.zip

# Download the mysql connector and place it in the proper directory
RUN wget http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.39.tar.gz

RUN tar -xvf mysql-connector-java-5.1.39.tar.gz

RUN cp ./mysql-connector-java-5.1.39/mysql-connector-java-5.1.39-bin.jar glassfish4/glassfish/domains/domain1/lib

RUN pip install sh

# Download RSS Code
RUN git clone https://github.com/FIWARE-TMForum/business-ecosystem-rss.git

WORKDIR business-ecosystem-rss

RUN git checkout develop

# Create WAR file
RUN mvn install -DskipTests

RUN mkdir /etc/default/rss/

# Set properties files
COPY database.properties /etc/default/rss/database.properties

COPY oauth.properties /etc/default/rss/oauth.properties

COPY ./entrypoint.py /
COPY ./entrypoint.sh /

EXPOSE 8080 4848 8181
ENTRYPOINT ["/entrypoint.sh"]
