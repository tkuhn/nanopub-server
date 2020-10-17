# Build stage
FROM maven:3.6.3-jdk-8-slim as build

# Copy source for build
COPY src /nanopub-server/src
COPY pom.xml /nanopub-server

# Build with maven
RUN mvn -f /nanopub-server/pom.xml clean package

# Pull base image
FROM tomcat:8-jre8

# Remove default webapps:
RUN rm -fr /usr/local/tomcat/webapps/*

# Copy target from build stage
COPY --from=build /nanopub-server/target/nanopub-server /usr/local/tomcat/nanopub-server/target/nanopub-server

COPY scripts /usr/local/tomcat/nanopub-server/scripts
RUN ln -s /usr/local/tomcat/nanopub-server/target/nanopub-server /usr/local/tomcat/webapps/ROOT

# Port:
EXPOSE 8080

CMD ["/usr/local/tomcat/nanopub-server/scripts/start.sh"]