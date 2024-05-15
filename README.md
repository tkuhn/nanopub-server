Nanopub Server
==============

This is an implementation of a nanopublication server. Such servers form a
server network, which can be used to publish
[nanopublications](http://nanopub.org) that have [trusty
URIs](http://arxiv.org/abs/1401.5775). (Such a server only returns entire
nanopubs. No queries supported; no triple store involved.)

Nanopublications are tiny snippets of data with provenance information attached.
They can be organized in a very flexible manner into large datasets (which can
also be described as nanopublications). More details can be found in the
publications listed below.

To access the server network, the
[nanopub-java](https://github.com/Nanopublication/nanopub-java) library can be
used.


Publications
------------

The following publications explain the approach:

- Kuhn et al. Publishing without Publishers: a Decentralized Approach to
  Dissemination, Retrieval, and Archiving of Data. In Proceedings of the 14th
  International Semantic Web Conference (ISWC) 2015.
  http://arxiv.org/abs/1411.2749
- Kuhn et al. Decentralized provenance-aware publishing with nanopublications.
  PeerJ Preprints. https://peerj.com/preprints/1760/
- Kuhn. nanopub-java: A Java Library for Nanopublications. In Proceedings of the
  5th Workshop on Linked Science (LISC 2015). http://arxiv.org/abs/1508.04977

Current Network
---------------

Have a look at these nanopub monitors ([source
code](https://github.com/tkuhn/nanopub-monitor)) to see the current nanopub
server network:

- Nanopub Monitor: 
  - https://monitor.knowledgepixels.com/
  - https://monitor.np.trustyuri.net/

From that monitor page, you can find the URLs of the current servers in the
network, for example this one:

- Example of nanopub servers:
  - https://np.knowledgepixels.com/
  - https://server.np.trustyuri.net/

Deployment
----------

### Easy Deployment with Docker

Requirements:

- Docker 1.10 or higher
- Docker-compose 1.6 or higher

After cloning the repository, execute:

    $ docker-compose up

Config can be changed by passing environment variables prefixed with `NPS_`.
E.g. setting `mongodb.host` is done via variable `NPS_MONGODB_HOST`.


### Manual Compilation and Deployment

Requirements:

- Java 1.7 or higher
- Access to a MongoDB instance
- Disk space of up to around 15kB per nanopublication (10-11kB is average so
  far, but this value might change; the current size of the network can be seen
  here: http://npmonitor.inn.ac/)

In addition, we assume here that Git and Maven are installed (tested with
Maven version 3; version 2.0.9 might work too).

Now you can fetch the code for the nanopub server:

    $ git clone https://github.com/tkuhn/nanopub-server.git

To configure the server, make a copy of the configuration file with the prefix
`local.`, which overrides the main configuration file:

    $ cd nanopub-server/src/main/resources/ch/tkuhn/nanopub/server/
    $ cp conf.properties local.conf.properties

Edit the file `local.conf.properties` to configure your instance of the server,
and then compile it (run in the top `nanopub-server` directory):

    $ mvn package

(Depending on the state of this repository, you might also have to install
the latest snapshot version of https://github.com/Nanopublication/nanopub-java
with `git clone ...` and `mvn install`.)

Then you can run the nanopub server using Maven's Jetty plugin:

    $ mvn jetty:run

Now you should be able to locally access the server from your browser:

    http://0.0.0.0:8080/nanopub-server/

Alternatively, you can give the file `target/nanopub-server.war` to a web
application server such as Apache Tomcat. Then you can setup your web
server to map a public URL to the nanopub server, for example:

    http://example.org/np/ > http://0.0.0.0:8080/nanopub-server/

Add the public URL to the line `public-url=` of the configuration file and
recompile and restart the server. Then, it will connect to the server network.


Usage
-----

Usage is pretty straightforward when you look at the web interface that a
nanopub server provides. To retrieve a nanopub like

    http://example.org/mynanopubs/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M

the artifact code (last 45 characters) has to be given to the nanopub server
like this:

    http://example.org/np/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M

File extensions can be used to retrieve the nanopub in different formats:

    http://example.org/np/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M.trig
    http://example.org/np/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M.nq
    http://example.org/np/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M.xml

Depending on your browser settings, these URLs might open in an external editor
or you might have to download them. In order to view them directly in the
browser, `.txt` can be appended:

    http://example.org/np/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M.nq.txt


License
-------

This nanopub server is free software under the MIT License. See LICENSE.txt.
