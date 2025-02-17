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

# Apache Calcite Avatica site

This directory contains the code for the
[Avatica web site](https://calcite.apache.org/avatica),
a sub-directory of the
[Apache Calcite web site](https://calcite.apache.org).

You can build the site manually using your environment or use the docker compose file.

The site is automatically built and published following the process outlined in the [Calcite repository](https://github.com/apache/calcite/blob/main/site/README.md).

# Previewing the website locally using docker
## Setup your environment

1. Install [docker](https://docs.docker.com/install/)
2. Install [docker compose](https://docs.docker.com/compose/install/)

## Build site
1. `cd site`
2. `docker compose run build-site`

## Generate javadoc
1. `cd site`
2. `docker compose run generate-javadoc`

## Running development mode locally
You can preview your work while working on the site.

1. `cd site`
2. `docker compose run --service-ports dev`

The web server will be started on [http://localhost:4000/avatica/](http://localhost:4000/avatica/) (note the trailing slash)

As you make changes to the site, the site will automatically rebuild.

## Publishing the website
The website is automatically published using GitHub Actions, so you do not need to do anything but just merge your
changes to the `main` branch.
