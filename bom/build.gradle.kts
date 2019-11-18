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
    `java-platform`
}

val String.v: String get() = rootProject.extra["$this.version"] as String

// Note: Gradle allows to declare dependency on "bom" as "api",
// and it makes the contraints to be transitively visible
// However Maven can't express that, so the approach is to use Gradle resolution
// and generate pom files with resolved versions
// See https://github.com/gradle/gradle/issues/9866

fun DependencyConstraintHandlerScope.apiv(
    notation: String,
    versionProp: String = notation.substringAfterLast(':')
) =
    "api"(notation + ":" + versionProp.v)

fun DependencyConstraintHandlerScope.runtimev(
    notation: String,
    versionProp: String = notation.substringAfterLast(':')
) =
    "runtime"(notation + ":" + versionProp.v)

dependencies {
    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    (constraints) {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        apiv("com.beust:jcommander")
        apiv("com.fasterxml.jackson.core:jackson-annotations", "jackson")
        apiv("com.fasterxml.jackson.core:jackson-core", "jackson")
        apiv("com.fasterxml.jackson.core:jackson-databind", "jackson")
        apiv("com.github.stephenc.jcip:jcip-annotations")
        apiv("com.google.guava:guava")
        apiv("com.google.protobuf:protobuf-java", "protobuf")
        apiv("com.h2database:h2")
        apiv("javax.servlet:javax.servlet-api", "servlet")
        apiv("junit:junit")
        apiv("net.hydromatic:scott-data-hsqldb")
        apiv("org.apache.httpcomponents:httpclient")
        apiv("org.apache.httpcomponents:httpcore")
        apiv("org.apache.kerby:kerb-client", "kerby")
        apiv("org.apache.kerby:kerb-core", "kerby")
        apiv("org.apache.kerby:kerb-simplekdc", "kerby")
        apiv("org.bouncycastle:bcpkix-jdk15on", "bouncycastle")
        apiv("org.bouncycastle:bcprov-jdk15on", "bouncycastle")
        apiv("org.eclipse.jetty:jetty-http", "jetty")
        apiv("org.eclipse.jetty:jetty-security", "jetty")
        apiv("org.eclipse.jetty:jetty-server", "jetty")
        apiv("org.eclipse.jetty:jetty-util", "jetty")
        apiv("org.hamcrest:hamcrest-core", "hamcrest")
        apiv("org.hsqldb:hsqldb")
        apiv("org.mockito:mockito-core", "mockito")
        apiv("org.ow2.asm:asm")
        apiv("org.ow2.asm:asm-all", "asm")
        apiv("org.ow2.asm:asm-analysis", "asm")
        apiv("org.ow2.asm:asm-commons", "asm")
        apiv("org.ow2.asm:asm-tree", "asm")
        apiv("org.ow2.asm:asm-util", "asm")
        apiv("org.slf4j:slf4j-api", "slf4j")
        apiv("org.slf4j:slf4j-log4j12", "slf4j")
    }
}
