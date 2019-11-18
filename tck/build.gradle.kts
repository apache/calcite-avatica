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

import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.license.GatherLicenseTask
import com.github.vlsi.gradle.license.api.SimpleLicense
import com.github.vlsi.gradle.license.api.SpdxLicense
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
    shaded(platform(project(":bom")))
    shaded("com.beust:jcommander")
    shaded("com.google.guava:guava")
    shaded("junit:junit")
    shaded("org.hamcrest:hamcrest-core")
    shaded("org.hsqldb:hsqldb")
    // As long as we want to run compatibility checks again 1.6.0, we have to include these because
    // they weren't yet provided by the avatica client jar
    shaded("org.slf4j:slf4j-api")
    shaded("org.slf4j:slf4j-log4j12")
}

tasks {
    val getLicenses by registering(GatherLicenseTask::class) {
        similarityThreshold.set(42*2)
        configuration(shaded)
        extraLicenseDir.set(file("$rootDir/src/main/config/licenses"))
        overrideLicense("org.hsqldb:hsqldb") {
            // https://sourceforge.net/p/hsqldb/feature-requests/351/
            expectedLicense = SimpleLicense("HSQLDB License, a BSD open source license", uri("http://hsqldb.org/web/hsqlLicense.html"))
            effectiveLicense = SpdxLicense.BSD_3_Clause
        }
        overrideLicense("org.hamcrest:hamcrest-core") {
            // https://github.com/hamcrest/JavaHamcrest/issues/264
            expectedLicense = SimpleLicense("New BSD License", uri("http://www.opensource.org/licenses/bsd-license.php"))
            effectiveLicense = SpdxLicense.BSD_3_Clause
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
        archiveClassifier.set("")
        configurations = listOf(shaded)
        exclude("META-INF/maven/**")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        listOf(
            "com.beust",
            "com.fasterxml",
            "com.google.common",
            "javax.servlet",
            "junit",
            "net.hydromatic",
            "org.apache.log4j",
            "org.eclipse.jetty",
            "org.hamcrest",
            "org.hsqldb",
            "org.junit"
        ).forEach {
            relocate(it, "${project.group}.tck.shaded.$it")
        }
        CrLfSpec(LineEndings.LF).run {
            into("META-INF") {
                textAuto()
                dependencyLicenses(licenseFiles)
            }
        }
    }

    jar {
        enabled = false
        dependsOn(shadowJar)
    }
}
