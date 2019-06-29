/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":metrics"))
    api("org.slf4j:slf4j-api")
    implementation("javax.servlet:javax.servlet-api")
    implementation("com.google.guava:guava")

    api("org.eclipse.jetty:jetty-http")
    api("org.eclipse.jetty:jetty-security")
    api("org.eclipse.jetty:jetty-server")
    api("org.eclipse.jetty:jetty-util")

    // TODO: AvaticaSuite includes AvaticaUtilsTest and ConnectStringParserTest from :core
    //   Does it really make sense?
    testImplementation(project(":core", "testClasses"))

    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.hamcrest:hamcrest-core")

    testImplementation("net.hydromatic:scott-data-hsqldb")
    testImplementation("org.apache.kerby:kerb-client")
    testImplementation("org.apache.kerby:kerb-core")
    testImplementation("org.apache.kerby:kerb-simplekdc")
    testRuntimeOnly("org.hsqldb:hsqldb")
    testImplementation("com.github.stephenc.jcip:jcip-annotations")
    testRuntimeOnly("org.slf4j:slf4j-log4j12")
    testImplementation("org.bouncycastle:bcpkix-jdk15on")
    testImplementation("org.bouncycastle:bcprov-jdk15on")
}

tasks {
    // TODO: remove when org.apache.calcite.avatica.SpnegoTestUtil.TARGET_DIR is updated
    //   to use build/ or temp directory
    withType<Test>().configureEach {
        // Maven by default uses "target" directory name.
        // Gradle uses "build" by default.
        // Unfortunately, some tests try to put some files there
        systemProperty("target.dir", "build")
    }
}
