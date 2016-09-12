FROM glassfish:latest

RUN apt-get update && apt-get install -y python2.7 python-pip git maven mysql-client openjdk-8-jdk

# Download the mysql connector and place it in the proper directory
RUN wget http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.39.tar.gz

RUN tar -xvf mysql-connector-java-5.1.39.tar.gz

RUN cp ./mysql-connector-java-5.1.39/mysql-connector-java-5.1.39-bin.jar glassfish/domains/domain1/lib

RUN pip install sh

RUN git clone https://github.com/FIWARE-TMForum/business-ecosystem-rss.git

WORKDIR business-ecosystem-rss

RUN git checkout develop

# Create WAR file

RUN mvn install -DskipTests

RUN mkdir /etc/default/rss/

RUN cp ./properties/database.properties /etc/default/rss/database.properties

RUN cp ./properties/oauth.properties /etc/default/rss/oauth.properties

RUN sed -i 's/jdbc:mysql:\/\/localhost:3306\/RSS/jdbc:mysql:\/\/db:3306\/RSS/g' /etc/default/rss/database.properties

RUN sed -i 's/database.username=root/database.username=root/g' /etc/default/rss/database.properties

RUN sed -i 's/database.password=root/database.password=my-secret-pw/g' /etc/default/rss/database.properties

RUN echo 'config.sellerRole=seller' >> /etc/default/rss/oauth.properties

RUN echo 'config.aggregatorRole=aggregator' >> /etc/default/rss/oauth.properties

COPY ./entrypoint.py /
COPY ./entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]