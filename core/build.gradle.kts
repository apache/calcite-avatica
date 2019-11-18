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

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    `java-library`
    id("com.github.vlsi.ide")
    id("com.google.protobuf")
}

// See https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_recognizing_dependencies
// TL;DR: public APIs are "api" (e.g. visible as superclass, or method argument, generic type, etc)
//   and "implementation" is for dependencies that do not participate in API
dependencies {
    api(project(":metrics"))
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.google.protobuf:protobuf-java")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("org.apache.httpcomponents:httpclient")
    implementation("org.apache.httpcomponents:httpcore")
    implementation("org.slf4j:slf4j-api")
    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.hamcrest:hamcrest-core")
}

sourceSets {
    main {
        proto {
            srcDir("src/main/protobuf")
        }
        java {
            // TODO: remove when protobuf-generated files are removed
            exclude("org/apache/calcite/avatica/proto/**")
        }
    }
}

val generatedProtobufDir = File(buildDir, "generated/source/proto/main/java")

tasks {
    named<Jar>("sourcesJar") {
        // TODO: remove when protobuf-generated files are removed
        from(generatedProtobufDir)
    }
}

val String.v: String get() = rootProject.extra["$this.version"] as String

protobuf {
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:${"protobuf".v}"
    }
    generateProtoTasks {
        for (task in ofSourceSet("main")) {
            ide {
                generatedJavaSources(task, generatedProtobufDir)
            }
        }
    }
}

val javaFilteredOutput = File(buildDir, "generated/java-filtered")

val filterJava by tasks.registering(Sync::class) {
    inputs.property("version", project.version)
    outputs.dir(javaFilteredOutput)

    from("$projectDir/src/main/java-filtered") {
        filteringCharset = "UTF-8"
        include("**/*.java")
        filter { x: String ->
            x.replace("${'$'}{avatica.release.version}", project.version.toString())
        }
    }
    into(javaFilteredOutput)
}

ide {
    generatedJavaSources(filterJava.get(), javaFilteredOutput)
}
