Nanopub Server
==============

_(work in progress)_

This is a simple server to publish nanopublications that have a
[trusty URI](http://arxiv.org/abs/1401.5775). It only returns single nanopubs.
No queries. No triple store involved. MongoDB is used to store the nanopubs.


Compilation
-----------

Maven has to be installed, and the packages nanopub-java and trustyuri-java
have to be installed manually.

Installation of nanopub-java:

    $ git clone git@github.com:Nanopublication/nanopub-java.git
    $ cd nanopub-java
    $ mvn install

Installation of trustyuri-java:

    $ git clone git@github.com:trustyuri/trustyuri-java.git
    $ cd trustyuri-java
    $ mvn install

Then compile and package with Maven:

    $ mvn clean package


Deployment
----------

Running the server using Maven's Jetty plugin:

    $ mvn jetty:run

Then you should be able to locally access the server from your browser:

    http://0.0.0.0:8080/nanopub-server/

Alternatively, you can give the file `target/nanopub-server.war` to a web
application server such as Apache Tomcat. Then you can setup your web
server to map a public URL to the nanopub server, for example:

    http://example.org/np/ > http://0.0.0.0:8080/nanopub-server/


Usage
-----

To retrieve a nanopub like

    http://nanopub.org/mynanopub/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70

the artifact code (last 45 characters) has to be given to the nanopub server
like this:

    http://example.org/np/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70

File extensions can be used to retrieve the nanopub in different formats:

    http://example.org/np/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70.trig
    http://example.org/np/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70.nq
    http://example.org/np/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70.xml

Depending on your browser settings, these URLs might open in an external editor
or you might have to download them. In order to view them directly in the
browser, `.txt` can be appended:

    http://example.org/np/RA5AbXdpz5DcaYXCh9l3eI9ruBosiL5XDU3rxBbBaUO70.nq.txt

The plus `+` operator can be used to request lists of stored nanopublications.
The following request asks for all stored nanopubs:

    http://example.org/np/+

This returns a plain text file with one nanopub URI per line. There is an upper
limit on how many nanopubs will be shown (1000 by default). If there are more
nanopubs for your request, the last line will be `...`. You can make more
specific requests by specifying the first characters of the artifact code:

    http://example.org/np/RA5A+

This will return all nanopubs whose artifact code starts with `RA5A`.


License
-------

This nanopub server is free software under the MIT License. See LICENSE.txt.
