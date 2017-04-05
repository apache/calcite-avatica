---
layout: docs
title: Docker Images
sidebar_title: Docker Images
permalink: /docs/docker.html
---

<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

## Docker Images for Avatica

[Docker](https://en.wikipedia.org/wiki/Docker_(software)) is a popular piece of
software that enables other software to run "anywhere". In the context of Avatica,
we can use Docker to enable a run-anywhere Avatica server. These Docker containers
can be used to easily create a server for the development of custom Avatica clients
or encapsulating database access for testing software that uses Avatica.

### Base "avatica-server" Docker Image

Starting with the Avatica 1.10.0 release, Avatica is providing a number of Docker
containers. Each of these images is based on a "parent" "avatica-server" Docker image.

This Docker image has no bindings to a specific database (it has not database-specific
JDBC driver included). It only contains a Java runtime and the Avatica Standalone Server
jar (which contains all the necessary dependencies of the Avatica server). This docker
image is not directly useful for end users; it is useful for those who want to use Avatica
with a database of their choosing.

This Docker image will be deployed to the [Apache Dockerhub account](https://hub.docker.com/r/apache/) after the release
of 1.10.0 and will be updated for future releases of Avatica.

### Database-specific Docker Images

To make the lives of end-users who want to use a specific database easier, some Docker
images are provided for some common databases. The current databases include:

* [HyperSQL](http://hsqldb.org) (2.3.1)
* [MySQL](https://www.mysql.com/) (5.1.41)
* [PostgreSQL](https://www.postgresql.org/) (42.0)

These images are not deployed as the licensing on each database driver is varied. Please
understand and accept the license of each before using in any software project.

Each of these images include a `build.sh` script which will build the docker image using
the latest `avatica-server` Docker image. The resulting Docker image will be named according
to the following format: `avatica-<database>-server`. For example, `avatica-hsqldb-server`,
`avatica-mysql-server`, and `avatica-postgresql-server`.

Additionally, [Docker Compose](https://github.com/docker/compose) configuration files for the above
databases (sans HyperSQL) are provided which configure the database's standard Docker image
and then connect Avatica to that Docker container. For example, the PostgreSQL docker-compose configuration
file will start an instance of PostgreSQL and an instance of the Avatica server, each in their own container,
exposing an Avatica server configured against a "real" PostgreSQL database.

All of the `Dockerfile` and `docker-compose.yml` files are conveniently provided in an archive for
each release, starting with 1.10.0.

```
avatica-docker-1.10.0-SNAPSHOT/
avatica-docker-1.10.0-SNAPSHOT/hypersql/
avatica-docker-1.10.0-SNAPSHOT/mysql/
avatica-docker-1.10.0-SNAPSHOT/postgresql/
avatica-docker-1.10.0-SNAPSHOT/Dockerfile
avatica-docker-1.10.0-SNAPSHOT/hypersql/build.sh
avatica-docker-1.10.0-SNAPSHOT/hypersql/Dockerfile
avatica-docker-1.10.0-SNAPSHOT/mysql/build.sh
avatica-docker-1.10.0-SNAPSHOT/mysql/docker-compose.yml
avatica-docker-1.10.0-SNAPSHOT/mysql/Dockerfile
avatica-docker-1.10.0-SNAPSHOT/postgresql/build.sh
avatica-docker-1.10.0-SNAPSHOT/postgresql/docker-compose.yml
avatica-docker-1.10.0-SNAPSHOT/postgresql/Dockerfile
```

#### Running

Each of the provided database-specific Docker images set an `ENTRYPOINT` which
encapsulate most of the Java command. The following options are available to specify:

```
Usage: <main class> [options]
  Options:
    -h, -help, --help
       Print the help message
       Default: false
    -p, --port
       Port the server should bind
       Default: 0
    -s, --serialization
       Serialization method to use
       Default: PROTOBUF
       Possible Values: [JSON, PROTOBUF]
  * -u, --url
       JDBC driver url for the server
```

For example, to connect to a MySQL server, the following could be used:

```
$ ./avatica-docker-*/mysql/build.sh
$ docker run --rm -it avatica-mysql-server \
    -u jdbc:mysql://<fqdn>:3306/my_database -p 8765
```

To debug these docker images, the `ENTRYPOINT` can be overriden to launch a shell

```
$ docker run --rm --entrypoint='' -it avatica-mysql-server /bin/sh
```
