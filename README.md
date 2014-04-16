Nanopub Server
==============

_work in progress_

This is a simple server to publish nanopublications. All it is supposed to do
is to provide stored nanopublications. No queries. No triple store.


Dependencies
------------

Maven has to be installed.

Installation of nanopub-java:

    $ git clone git@github.com:Nanopublication/nanopub-java.git
    $ cd nanopub-java
    $ mvn install

Installation of trustyuri-java:

    $ git clone git@github.com:trustyuri/trustyuri-java.git
    $ cd trustyuri-java
    $ mvn install


Compilation and Execution
-------------------------

Compile and package with Maven:

    $ mvn clean package

Running the server using Maven's Jetty plugin:

    $ mvn jetty:run

Then you should be able to locally access the web interface from your browser:

    http://0.0.0.0:8080/nanopub-server/

Alternatively, you can give the file `target/nanopub-server.war` to a web
application server such as Apache Tomcat.


License
-------

This validator for nanopublications is free software under the MIT License. See
LICENSE.txt.
