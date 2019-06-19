# Pull base image
FROM tomcat:8-jre8

# Remove default webapps:
RUN rm -fr /usr/local/tomcat/webapps/*

# Add nanopub-server app at root position:
COPY target/nanopub-server /usr/local/tomcat/webapps/ROOT

# Port:
EXPOSE 8080
