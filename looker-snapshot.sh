#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# $1 is the artifact ID
# $2 is the version
# $3 is the JAR path
function snapshot_upload {
    mvn deploy:deploy-file \
        -DgroupId=org.apache.calcite.avatica \
        -DartifactId="$1" \
        -Dversion="$2" \
        -Dpackaging=jar \
        -Dfile="$3" \
        -DgeneratePom=true \
        -DrepositoryId=nexus \
        -Durl=https://nexusrepo.looker.com/repository/maven-snapshots/
}

./gradlew build && ./gradlew jar && ./gradlew generatePom && (
    VERSION="$(sed -n 's/^calcite\.avatica\.version=\([^ ]*\).*/\1/p' gradle.properties)-SNAPSHOT"
    snapshot_upload avatica-core "$VERSION" "./core/build/libs/avatica-core-$VERSION.jar"
    snapshot_upload avatica-server "$VERSION" "./server/build/libs/avatica-server-$VERSION.jar"
    snapshot_upload avatica-metrics "$VERSION" "./metrics/build/libs/avatica-metrics-$VERSION.jar"
    snapshot_upload avatica "$VERSION" "./shaded/core/build/libs/avatica-$VERSION-shadow.jar"
    echo
    echo "Done uploading version ${VERSION} to Looker Nexus Snapshots!"
)
