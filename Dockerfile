# Use latest Maven as base docker image
FROM maven

# define mainworkdir
WORKDIR /opt/application

# add all app files to workdir
ADD . /opt/application

# install & package
RUN mvn install
RUN mvn package

# expose port 8080
EXPOSE 8080

# define run command
CMD ["bash", "scripts/start.sh"]
