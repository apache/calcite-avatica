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

pluginManagement {
    plugins {
        fun PluginDependenciesSpec.idv(id: String) = id(id) version extra["$id.version"].toString()

        idv("com.github.johnrengelman.shadow")
        idv("com.github.spotbugs")
        idv("com.github.vlsi.crlf")
        idv("com.github.vlsi.ide")
        idv("com.github.vlsi.license-gather")
        idv("com.github.vlsi.stage-vote-release")
        idv("com.google.protobuf")
        idv("de.thetaphi.forbiddenapis")
        idv("org.jetbrains.gradle.plugin.idea-ext")
        idv("org.nosphere.apache.rat")
        idv("org.owasp.dependencycheck")
    }
}

// This is the name of a current project
// Note: it cannot be inferred from the directory name as developer might clone Avatica to avatica_tmp folder
rootProject.name = "calcite-avatica"

include(
        "bom",
        "core",
        "docker",
        "metrics",
        "metrics-dropwizardmetrics",
        "noop-driver",
        "server",
        "tck",
        "standalone-server",
        "shaded:avatica",
        "release"
        )

// https://discuss.gradle.org/t/multi-module-project-with-sub-modules-with-same-name/31928
// Gradle can't handle dependencies between projects with same name even at different full paths
project(":shaded:avatica").projectDir = file("shaded/core")

// See https://github.com/gradle/gradle/issues/1348#issuecomment-284758705 and
// https://github.com/gradle/gradle/issues/5321#issuecomment-387561204
// Gradle inherits Ant "default excludes", however we do want to archive those files
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitattributes")
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

fun property(name: String) =
    when (extra.has(name)) {
        true -> extra.get(name) as? String
        else -> null
    }

// This enables to use local clone of vlsi-release-plugins for debugging purposes
property("localReleasePlugins")?.ifBlank { "../vlsi-release-plugins" }?.let {
    println("Importing project '$it'")
    includeBuild(it)
}
