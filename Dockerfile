# Pull base image
FROM tomcat:8-jre8

# Remove default webapps:
RUN rm -fr /usr/local/tomcat/webapps/*

COPY target/nanopub-server /usr/local/tomcat/nanopub-server/target/nanopub-server
COPY scripts /usr/local/tomcat/nanopub-server/scripts
RUN ln -s /usr/local/tomcat/nanopub-server/target/nanopub-server /usr/local/tomcat/webapps/ROOT

# Port:
EXPOSE 8080

CMD ["/usr/local/tomcat/nanopub-server/scripts/start.sh"]