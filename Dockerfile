# Pull base image
FROM tomcat:8-jdk8

# Remove default webapps:
RUN apt-get update &&\
    apt-get install -y maven=3.6.0-1 &&\
    rm -fr /usr/local/tomcat/webapps/*

COPY . /usr/local/tomcat/nanopub-server/
WORKDIR /usr/local/tomcat/nanopub-server/

RUN mvn clean install && ln -s /usr/local/tomcat/nanopub-server/target/nanopub-server /usr/local/tomcat/webapps/ROOT

# Port:
EXPOSE 8080

CMD ["/usr/local/tomcat/nanopub-server/scripts/start.sh"]
