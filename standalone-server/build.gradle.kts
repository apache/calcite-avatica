/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.license.GatherLicenseTask
import com.github.vlsi.gradle.license.api.SimpleLicense
import com.github.vlsi.gradle.license.api.SpdxLicense
import com.github.vlsi.gradle.license.api.SpdxLicenseException
import com.github.vlsi.gradle.license.api.and
import com.github.vlsi.gradle.license.api.with
import com.github.vlsi.gradle.release.Apache2LicenseRenderer
import com.github.vlsi.gradle.release.ArtifactType
import com.github.vlsi.gradle.release.dsl.dependencyLicenses
import com.github.vlsi.gradle.release.dsl.licensesCopySpec

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.license-gather")
    id("com.github.vlsi.stage-vote-release")
}

val shaded by configurations.creating

configurations {
    compileOnly {
        extendsFrom(shaded)
    }
}

dependencies {
    shaded(project(":core"))
    shaded(project(":server"))
    shaded("com.beust:jcommander")

    shaded("org.slf4j:slf4j-api")
    shaded("org.slf4j:slf4j-log4j12")
}

tasks {
    jar {
        enabled = false
        dependsOn(shadowJar)
    }

    val getLicenses by registering(GatherLicenseTask::class) {
        configuration(shaded)
        extraLicenseDir.set(file("$rootDir/src/main/config/licenses"))
        expectLicense("com.google.protobuf:protobuf-java", SpdxLicense.BSD_3_Clause)
        expectLicense("org.slf4j:slf4j-api:1.7.25", SpdxLicense.MIT)
        overrideLicense("javax.servlet:javax.servlet-api:4.0.1") {
            expectedLicense = SimpleLicense(
                "CDDL + GPLv2 with classpath exception",
                uri("https://oss.oracle.com/licenses/CDDL+GPL-1.1")
            )
            effectiveLicense =
                SpdxLicense.CDDL_1_1 and (SpdxLicense.GPL_2_0_or_later with SpdxLicenseException.Classpath_exception_2_0)
        }
    }

    val license by registering(Apache2LicenseRenderer::class) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Generates LICENSE file for the uberjar"
        artifactType.set(ArtifactType.BINARY)
        metadata.from(getLicenses)
    }

    val licenseFiles = licensesCopySpec(license)

    shadowJar {
        manifest {
            attributes["Main-Class"] = "org.apache.calcite.avatica.standalone.StandaloneServer"
        }
        archiveClassifier.set("shadow")
        configurations = listOf(shaded)
        exclude("META-INF/maven/**")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        listOf(
            "com.beust",
            "com.fasterxml",
            "com.google.common",
            "com.google.protobuf",
            "javax.servlet",
            "org.apache.http",
            "org.apache.commons"
        ).forEach {
            relocate(it, "${project.group}.standalone.shaded.$it")
        }
        CrLfSpec(LineEndings.LF).run {
            into("META-INF") {
                textAuto()
                dependencyLicenses(licenseFiles)
            }
        }
    }
}

artifacts {
    extraMavenPublications(tasks.shadowJar) {
        classifier = ""
    }
}
