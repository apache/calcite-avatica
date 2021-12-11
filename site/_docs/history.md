---
layout: docs
title: History
permalink: "/docs/history.html"
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

For a full list of releases, see
<a href="https://github.com/apache/calcite-avatica/releases">github</a>.
Downloads are available on the
[downloads page]({{ site.baseurl }}/downloads/avatica.html).

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.20.0">1.20.0</a> / 2021-12-13
{: #v1-20-0}

Apache Calcite Avatica 1.20.0
<a href="https://issues.apache.org/jira/browse/CALCITE-4931">upgrades Log4j2 to version 2.15.0</a>
to address
<a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=2021-44228">CVE-2021-44228</a>,
and makes the SPNEGO protocol
<a href="https://issues.apache.org/jira/browse/CALCITE-4152">much more efficient</a>.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13, 14, 15;
using IBM Java 8;
Guava versions 14.0.1 to 31.0.1-jre;
other software versions as specified in `gradle.properties`.

Contributors to this release:
Jacques Nadeau,
Jincheng Sun,
Josh Elser,
Julian Hyde (release manager),
NobiGo,
Sergey Nuyanzin,
Stamatis Zampetakis.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-4931">CALCITE-4931</a>]
  Upgrade SLF4J binding to Log4j2 version 2.15.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4877">CALCITE-4877</a>]
  Make the exception information more explicit for instantiate plugin
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4152">CALCITE-4152</a>]
  Upgrade Avatica to use the configurable SPNEGO Jetty implementation
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4828">CALCITE-4828</a>]
  Standard exception console output
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4837">CALCITE-4837</a>]
  `FLOOR` and `CEIL` functions return wrong results for `DECADE`, `CENTURY`,
  `MILLENNIUM`

Build and tests

* Disable Travis job that uses Calcite master until
  [<a href="https://issues.apache.org/jira/browse/CALCITE-4877">CALCITE-4877</a>]
  is fixed

Web site and documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-4840">CALCITE-4840</a>]
  Make `README` easier to scan

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.19.0">1.19.0</a> / 2021-10-11
{: #v1-19-0}

Apache Calcite Avatica 1.19.0 adds support for `BIT` and `NULL` data
types, fixes issues with values of type `ARRAY`, and includes a few
dependency updates.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13, 14, 15;
using IBM Java 8;
Guava versions 14.0.1 to 31.0.1-jre;
other software versions as specified in `gradle.properties`.

Contributors to this release:
Alessandro Solimando,
Amann Malik,
chenyuzhi459,
Francis Chuang,
Istvan Toth,
Julian Hyde (release manager),
NobiGo,
Jack Scott,
Sergey Nuyanzin,
Stamatis Zampetakis,
Zeng Rui.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-4573">CALCITE-4573</a>]
  `NullPointerException` while fetching from a column of type `ARRAY`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4626">CALCITE-4626</a>]
  Upgrade protobuf version to 3.17.1 to remove `com.google.protobuf.UnsafeUtil`
  warnings
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4602">CALCITE-4602</a>]
  `ClassCastException` retrieving from `ARRAY` that has mixed `INTEGER` and
  `DECIMAL` elements
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4600">CALCITE-4600</a>]
  `ClassCastException` retrieving from an `ARRAY` that has `DATE`, `TIME` or
  `TIMESTAMP` elements
* Upgrade forbiddenapis 2.7 &rarr; 3.2, and Guava to 14.0.1 &rarr; 31.0.1-jre
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4757">CALCITE-4757</a>]
  Allow columns of type `NULL` in `ResultSet`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4767">CALCITE-4767</a>]
  Add `Quoting.BACK_TICK_BACKSLASH`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4536">CALCITE-4536</a>]
  Add support for `BIT` data type
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4752">CALCITE-4752</a>]
  `PreparedStatement#SetObject()` fails for `BigDecimal` values
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4646">CALCITE-4646</a>]
  Bump Jetty version to 9.4.42.v20210604
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4676">CALCITE-4676</a>]
  Avatica client leaks TCP connections

Build and tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-4790">CALCITE-4790</a>]
  Make Gradle pass the `user.timezone` property to the test JVM
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4815">CALCITE-4815</a>]
  Enforce shaded artifacts include `checker-qual` 3.10.0 or later
* Bump javadoc compilation gradle image to 6.8
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4755">CALCITE-4755</a>]
  Prepare for next development iteration

Website and documentation

* Site: Remove nowadays redundant minified javascript files

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.18.0">1.18.0</a> / 2021-05-18
{: #v1-18-0}

Apache Calcite Avatica 1.18.0 includes a few dependency upgrades, minor fixes and a breaking change.
Please see below to determine how the breaking change will affect your project.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13, 14, 15;
using IBM Java 8;
Guava versions 14.0 to 29.0;
other software versions as specified in `gradle.properties`.

***Breaking changes***
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4503">CALCITE-4503</a>] Order of fields in records should follow that of the SQL types

`Meta#deduce(List<ColumnMetaData> columns, Class resultClazz)` now only derives the order of the fields from the list of provided column metadata `columns` when generating a record from the given Java class `resultClazz`, instead of relying on the field order provided by `Object#getFields()`, which is a JVM-dependent feature.

Before, the field names where not checked against the field names of the provided class. Now, if `resultClazz` is not null, the provided field names are expected to match existing fields in that class. If a column metadata has name `column`, and no public field in `resultClazz` with that name exists, the following exception is thrown:
`java.lang.RuntimeException: java.lang.NoSuchFieldException: C`.

Features and bug fixes
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4138">CALCITE-4138</a>]
  Metadata operations via Avatica turn empty string args to null (Istvan Toth)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4095">CALCITE-4095</a>]
  Update Jetty to 9.4.31.v20200723 and use `SslContextFactory.Server` instead of `SslContextFactory` (Peter Somogyi)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4196">CALCITE-4196</a>]
  Consume all data from client before replying with HTTP/401
* Upgrade gradle from 6.3 to 6.7
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4379">CALCITE-4379</a>]
  Meta.Frame created with java float values in rows hits a ClassCastException in toProto() (Dmitri Bourlatchkov)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4181">CALCITE-4181</a>]
  Avatica throws exception when select field is a List<Object> (Kent Nguyen)
* Upgrade Gradle from 6.7 to 6.8.1
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4476">CALCITE-4476</a>]
  DateTimeUtils.timeStringToUnixDate may produce wrong time (Vladimir Ozerov)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3401">CALCITE-3401</a>]
  Assume empty keystore passwords by default (Istvan Toth, Alessandro Solimando)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3881">CALCITE-3881</a>]
  DateTimeUtils.addMonths yields incorrect results (Zhenghua Gao)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3163">CALCITE-3163</a>]
  Incorrect mapping of JDBC float/real array types to Java types (Ralph Gasser)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4503">CALCITE-4503</a>]
  Order of fields in records should follow that of the SQL types (Alessandro Solimando)
* Upgrade Gradle docker containers to 6.8
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4575">CALCITE-4575</a>]
  Remove Gradle wrapper from source distribution
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4576">CALCITE-4576</a>]
  Release process should not overwrite LICENSE file

Tests
* Remove files that change often from Travis cache and remove broken files automatically
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3163">CALCITE-3163</a>]
  Improve test coverage for float/real/double array types (Alessandro Solimando)
* Replace AssertTestUtils with custom Hamcrest matcher for accessors content

Website and Documentation
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3841">CALCITE-3841</a>]
  Change downloads page to use downloads.apache.org
* [<a href="https://issues.apache.org/jira/browse/CALCITE-4367">CALCITE-4367</a>]
  Correct Avatica protocol docs

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.17.0">1.17.0</a> / 2020-06-22
{: #v1-17-0}

Apache Calcite Avatica 1.17.0 is a small release incorporating a few dependency upgrades and minor fixes.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13, 14;
using IBM Java 8;
Guava versions 14.0 to 29.0;
other software versions as specified in `gradle.properties`.

Features and bug fixes
* Upgrade RAT to 0.5.3 and print violations in the console.
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3610">CALCITE-3610</a>]
  Fix dockerhub dockerfiles to point to the right JARs on nexus
* Move PGP signing to com.github.vlsi.stage-vote-release Gradle plugin.
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3623">CALCITE-3623</a>]
  Replace Spotless with AutoStyle.
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2704">CALCITE-2704</a>]
  Fix multilingual decoding issue where the server parses Chinese characters as gibberish.
* Upgrade Gradle to 6.3 and color test results in output.
* Upgrade protobuf to 0.8.12.
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3822">CALCITE-3822</a>]
  Source distribution must not contain fonts under SIL OFL 1.1 license (category B)
* Upgrade Gradle to 6.3 in docker-compose files.

Tests
* Upgrade Github Actions' Checkout action to v2.
* Add gradle wrapper validation job to Github Actions.

Website and Documentation
* Fix broken link to Javascript binding in the documentation.

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.16.0">1.16.0</a> / 2019-12-19
{: #v1-16-0}

Apache Calcite Avatica 1.16.0 replaces the maven with gradle. This release adds support for Kerberos authentication
using SPNEGO over HTTPS. In addition, there were also a few dependency updates and bug fixes. Github Actions was also
enabled in the repository for running tests.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13;
using IBM Java 8;
Guava versions 14.0 to 23.0;
other software versions as specified in `gradle.properties`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-3059">CALCITE-3059</a>]
  Fix release script to use correct release branch name when merging to master and to use the correct variable when generating the vote email
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3090">CALCITE-3090</a>]
  Remove the Central configuration
* Update owsap-dependency-check from 4.0.2 to 5.0.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3104">CALCITE-3104</a>]
  Bump httpcore from 4.4.10 to 4.4.11 (Fokko Driesprong)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3105">CALCITE-3105</a>]
  Bump Jackson from 2.9.8 to 2.9.9 (Fokko Driesprong)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3180">CALCITE-3180</a>]
  Bump httpclient from 4.5.6 to 4.5.9 (Fokko Driesprong)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3324">CALCITE-3324</a>]
  Add create method in MetaResultSet (Robert Yokota)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3384">CALCITE-3384</a>]
  Support Kerberos-authentication using SPNEGO over HTTPS (Istvan Toth)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3199">CALCITE-3199</a>]
  DateTimeUtils.unixDateCeil should not return the same value as unixDateFloor (Zhenghua Gao)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3412">CALCITE-3412</a>]
  FLOOR(timestamp TO WEEK) gives wrong result: Fix DateTimeUtils.julianDateFloor so that unixDateFloor etc. give the right result
* Implement Gradle-based build scripts
* Sign release artifacts only, skip signing for -SNAPSHOT
* Add source=1.8 to javadoc options, fix javadoc warnings
* Add -PskipJavadoc to skip publication of the javadocs (to speedup publishToMavenLocal)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3490">CALCITE-3490</a>]
  Upgrade Jackson to 2.10.0
* Bump release plugin 1.44.0 -> 1.45.0: do not require GPG key for publishDist
* Bump release plugins 1.45.0 -> 1.46.0: avoid failures on Gralde upgrade, fix gitignore handling
* Add -PenableMavenLocal and -PenableGradleMetadata build options
* Update build script: simplify properties, fix javadoc build for non UTF8 default encoding
* Update release plugins to 1.48.0 to workaround SVN 1.9 issue with mv+cp
* Sort dependencies, use api(javax.servlet), implementation(slf4j-api)
* @PackageMarker is no longer needed
* License header is managed with Spotless, there's no need to double-check it with Checkstyle
* Whitespace is managed with Spotless, so the check is removed from Checkstyle config
* Upgrade to Gradle 6.0.1 to prevent pushing .sha256 and .sha512 to Nexus
* Add gradle task 'aggregateJavadocIncludingTests' that builds javadoc for both main and test
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3493">CALCITE-3493</a>]
  Update docker script to use gradle
* Use Gitbox for pushing tags when making a release
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3573">CALCITE-3573</a>]
  Upgrade to Gradle 6.0 container to build releases and javadoc
* Configure Git tags to be pushed to calcite-avatica repository not calcite
* Stop building zip archives when building using gradle

Tests
* Use GitHub Actions for Windows CI
* Add Travis job with building Calcite master
* Show standard streams in Gradle tests
* Skip slow Calcite tests
* Add GitHub Actions macOS
* Fix AvaticaSpnegoTest for canonicalHostName("localhost") != "localhost": Avatica HTTP client always uses CANONICAL_HOSTNAME which confuses test code.
* Use Spotless and .editorconfig for import order normalization instead of Checkstyle
* Add option to skip signing: -PskipSigning
* Fetch Calcite from apache/calcite repository for integration testing
* GitHub Actions: actions/checkout@master -> v1.1.0 to avoid unexpected failures

Website and Documentation
* Add JavaScript client to client list
* Update avatica/docs/howto.md: SVN -> Git, fix wording
* Exclude "site/target" from Jekyll build: It prevents generation of unwanted site/target/avatica/target directory
* Configure Jekyll to use Etc/GMT+5 timezone for consistent page urls
* Fix links to javadoc
* Remove instructions to close Nexus repository when building a rc as this is now automated

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.15.0">1.15.0</a> / 2019-05-13
{: #v1-15-0}

Apache Calcite Avatica 1.15.0 is a small release that reverts CALCITE-2845 due to some incompatibilities with downstream
clients.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13;
using IBM Java 8;
Guava versions 14.0 to 23.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-3043">CALCITE-3043</a>]
  Add the ability to publish and promote releases using docker
* [<a href="https://issues.apache.org/jira/browse/CALCITE-3040">CALCITE-3040</a>]
  Revert CALCITE-2845 due to incompatibilities with downstream clients

Website and Documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-3033">CALCITE-3033</a>]
  Add navigation item for avatica-go HOWTO documentation

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.14.0">1.14.0</a> / 2019-04-29
{: #v1-14-0}

Apache Calcite Avatica 1.14.0 includes around 13 bugs fixes and enhancements. Jetty was upgraded to 9.4.15v20190215,
which fixes a vulnerability of moderate severity: [CVE-2018-12545](https://nvd.nist.gov/vuln/detail/CVE-2018-12545).

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12, 13;
using IBM Java 8;
Guava versions 14.0 to 23.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2789">CALCITE-2789</a>]
  Bump version dependencies Jan 2019
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2845">CALCITE-2845</a>]
  Avoid duplication of exception messages
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2950">CALCITE-2950</a>]
  Avatica DriverVersion.load leaks InputStream
* Improve exception message in AbstractService
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2776">CALCITE-2776</a>]
  Fix wrong value when accessing struct types with one attribute
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2269">CALCITE-2269</a>]
  Enable Error Prone checking
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2945">CALCITE-2945</a>]
  Use Boolean#equals in Boolean object compare
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2972">CALCITE-2972</a>]
  Upgrade jetty to 9.4.15.v20190215
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2987">CALCITE-2987</a>]
  Use maven image instead of maven:alpine when building release using docker
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2882">CALCITE-2882</a>]
  Connection properties are lost after timeout (Bake)
* Fix misspelled JDBC connection max duration property: expiryduration (Lanny)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2939">CALCITE-2939</a>]
  Fix NPE when executeBatch is array type (Bake)

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2728">CALCITE-2728</a>]
  Update appveyor.yml to enable Appveyor testing against JDK 11
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2961">CALCITE-2961</a>]
  Enable Travis to test against JDK 13

Website and Documentation

* Update to new git URL (switch to gitbox)
* Add links to git commits back to download pages
* Switch from maven:alpine to maven docker image for generating javadoc when publishing the site
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2922">CALCITE-2922</a>]
  Update link to Apache Jenkins Calcite-Avatica job

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.13.0">1.13.0</a> / 2018-12-04
{: #v1-13-0}

Apache Calcite Avatica 1.13.0 includes around 30 bugs fixes and enhancements. This release adds the ability to
prepare and make a release, run tests and execute `mvn clean` in a docker container.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11, 12;
using IBM Java 8;
Guava versions 14.0 to 23.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2386">CALCITE-2386</a>]
  Naively wire up struct support
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2390">CALCITE-2390</a>]
  Remove uses of `X509CertificateObject` which is deprecated in current version of bouncycastle
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2467">CALCITE-2467</a>]
  Update owasp-dependency-check maven plugin to 3.3.1, protobuf-java to 3.5.1, jackson to 2.9.6 and jetty to 9.4.11.v20180605
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2503">CALCITE-2503</a>]
  AvaticaCommonsHttpClientImpl client needs to set user-token on HttpClientContext before sending the request
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2570">CALCITE-2570</a>]
  Upgrade forbiddenapis to 2.6 for JDK 11 support
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1183">CALCITE-1183</a>]
  Upgrade kerby to 1.1.1 and re-enable AvaticaSpnegoTest
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2486">CALCITE-2486</a>]
  Upgrade Apache parent POM to version 21 and update other dependencies
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2583">CALCITE-2583</a>]
  Upgrade dropwizard metrics to 4.0.3
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1006">CALCITE-1006</a>]
  Enable spotbugs-maven-plugin
* Move spotbugs-filter.xml to src/main/config/spotbugs/
* Update usage of JCommander after upgrading to 1.72
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2587">CALCITE-2587</a>]
  Regenerate protobuf files for protobuf 3.6.1
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2594">CALCITE-2594</a>]
  Ensure forbiddenapis and maven-compiler use the correct JDK version
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2595">CALCITE-2595</a>]
  Add maven wrapper
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2385">CALCITE-2385</a>]
  Add flag to disable dockerfile checks when executing a dry-run build
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2676">CALCITE-2676</a>]
  Add release script and docker-compose.yml to support building releases using docker
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2680">CALCITE-2680</a>]
  Downgrade maven-scm-provider to 1.10.0 due to API incompatibility that prevents releases from building
* Update release script to use GPG agent
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2681">CALCITE-2681</a>]
  Add maven-scm-api as a dependency, so that Avatica can build
* Include -Dgpg.keyname when executing release:perform in the release script
* Prompt user for git username when using release script
* Fix release script to ensure git usernames are not truncated
* Remove requirement to set maven master password when using the release script
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2682">CALCITE-2682</a>]
  Add ability to run tests in docker
* Update travis-ci status badge to the correct one in README.md
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2385">CALCITE-2385</a>]
  Update travis configuration to disable dockerfile checks during testing
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2698">CALCITE-2698</a>]
  Use Docker Hub hooks to select Avatica version during image build and publish HSQLDB image

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2568">CALCITE-2568</a>]
  Ensure that IBM JDK TLS cipher list matches Oracle/OpenJDK for Travis CI
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2655">CALCITE-2655</a>]
  Enable Travis to test against JDK12
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2412">CALCITE-2412</a>]
  Add appveyor.yml to run Windows tests

Website and Documentation

* Fix broken links to Github release on the history page
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2381">CALCITE-2381</a>]
  Document how to authenticate against the Apache maven repository, select GPG keys and version numbers when building
  a release
* Fix Go client download links
* Fix download link to release history in news item template
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2550">CALCITE-2550</a>]
  Update download links for avatica-go to link to `apache-calcite-avatica-go-x.x.x-src.tar.gz` for release 3.2.0 and onwards
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2574">CALCITE-2574</a>]
  Update download pages to include instructions for verifying downloaded artifacts
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2577">CALCITE-2577</a>]
  Update URLs on download page to HTTPS
* Update links on Go client download page to reference `go_history.html` and not `history.html`

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.12.0">1.12.0</a> / 2018-06-24
{: #v1-12-0}

Apache Calcite Avatica 1.12.0 includes more than 20 bugs fixes and new features. ZIP archives will no longer be
produced from this release onwards.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10, 11;
using IBM Java 8;
Guava versions 14.0 to 23.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1520">CALCITE-1520</a>]
  Implement method `AvaticaConnection.isValid()`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2212">CALCITE-2212</a>]
  Enforce minimum JDK 8 via `maven-enforcer-plugin`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2268">CALCITE-2268</a>]
  Bump HSQLDB to 2.4.0 in Avatica Docker image
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2272">CALCITE-2272</a>]
  Bump dependencies: Apache Parent POM 19, JDK 10 Surefire and JDK 10 Javadoc
  Fix Javadoc generation
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2218">CALCITE-2218</a>]
  Fix `AvaticaResultSet.getRow()`
* Add Docker Hub image for HSQLDB
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2289">CALCITE-2289</a>]
  Enable html5 for Javadoc on JDK 9+
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2284">CALCITE-2284</a>]
  Allow Jetty Server to be customized before startup (Alex Araujo)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2333">CALCITE-2333</a>]
  Stop generating ZIP archives for release
* Bump HSQLDB dependency to 2.4.1
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2294">CALCITE-2294</a>]
  Allow customization for `AvaticaServerConfiguration` for plugging new
  authentication mechanisms (Karan Mehta)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1884">CALCITE-1884</a>]
  `DateTimeUtils` produces incorrect results for days before the Gregorian
  cutover (Haohui Mai and Sergey Nuyanzin)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2303">CALCITE-2303</a>]
  Support `MICROSECONDS`, `MILLISECONDS`, `EPOCH`, `ISODOW`, `ISOYEAR` and
  `DECADE` time units in `EXTRACT` function (Sergey Nuyanzin)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2350">CALCITE-2350</a>]
  Fix cannot shade Avatica with Guava 21.0 or higher
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2341">CALCITE-2341</a>]
  Fix Javadoc plugin incompatibility
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2352">CALCITE-2352</a>]
  Update checkstyle to 6.18 and update `suppressions.xml`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2219">CALCITE-2219</a>]
  Update `Connection`, `Statement`, `PreparedStatement` and `ResultSet` to throw
  an exception if resource is closed
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2361">CALCITE-2361</a>]
  Upgrade Bouncycastle to 1.59
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2299">CALCITE-2299</a>]
  Add support for `NANOSECOND` in `TimeUnit` and `TimeUnitRange`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2285">CALCITE-2285</a>]
  Support client cert keystore for Avatica client (Karan Mehta)

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2210">CALCITE-2210</a>]
  Remove `oraclejdk7`, add `oraclejdk9`, add `oraclejdk10`, and add `ibmjava` to
  `.travis.yml`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2255">CALCITE-2255</a>]
  Add JDK 11 `.travis.yml`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2022">CALCITE-2022</a>]
  Add dynamic drive calculation to correctly determine trust store location when
  testing on Windows (Sergey Nuyanzin)

Website and Documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1160">CALCITE-1160</a>]
  Redirect from Avatica community to Calcite community
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1937">CALCITE-1937</a>]
  Update Avatica website to support the inclusion of Avatica-Go's content and add
  option for using docker to develop and build the website
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1914">CALCITE-1914</a>]
  Add DOAP (Description of a Project) file for Avatica
* Fix broken link in `HOWTO`
* Add missing license header to avatica-go docs generation script

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.11.0">1.11.0</a> / 2018-03-09
{: #v1-11-0}

Apache Calcite Avatica 1.11.0 adds support for JDK 10 and drops
support for JDK 7. There are more than 20 bug fixes and new features.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 8, 9, 10;
Guava versions 14.0 to 23.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* Generate sha256 checksums for releases (previous releases used md5 and sha1)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2199">CALCITE-2199</a>]
  Allow methods overriding `AvaticaResultSet.getStatement()` to throw a
  `SQLException` (Benjamin Cogrel)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-508">CALCITE-508</a>]
  Ensure that `RuntimeException` is wrapped in `SQLException`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2013">CALCITE-2013</a>]
  Upgrade HSQLDB to 2.4
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2154">CALCITE-2154</a>]
  Upgrade jackson to 2.9.4
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2140">CALCITE-2140</a>]
  Basic implementation of `Statement.getMoreResults()`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2086">CALCITE-2086</a>]
  Increased max allowed HTTP header size to 64KB
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2073">CALCITE-2073</a>]
  Allow disabling of the `maven-protobuf-plugin`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2017">CALCITE-2017</a>]
  Support JAAS-based Kerberos login on IBM Java
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2003">CALCITE-2003</a>]
  Remove global synchronization on `openConnection`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1922">CALCITE-1922</a>]
  Allow kerberos v5 OID in SPNEGO authentication
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1915">CALCITE-1915</a>]
  Work around a Jetty bug where the SPNEGO challenge is not sent
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1902">CALCITE-1902</a>]
  In `AvaticaResultSet` methods, throw `SQLFeatureNotSupportedException` rather
  than `UnsupportedOperationException` (Sergio Sainz)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1487">CALCITE-1487</a>]
  Set the request as handled with authentication failures
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1904">CALCITE-1904</a>]
  Allow SSL hostname verification to be turned off
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1879">CALCITE-1879</a>]
  Log incoming protobuf requests at `TRACE`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1880">CALCITE-1880</a>]
  Regenerate protobuf files for 3.3.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1836">CALCITE-1836</a>]
  Upgrade to protobuf-java-3.3.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1195">CALCITE-1195</a>]
  Add a curl+jq example for interacting with Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1813">CALCITE-1813</a>]
  Use correct noop-driver artifactId

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-2145">CALCITE-2145</a>]
  `RemoteDriverTest.testBatchInsertWithDates` fails in certain time zones
  (Alessandro Solimando)
* Fix tests on Windows; disable SPNEGO test on Windows

Web site and documentation

* Update description of the `signature` field in `ResultSetResponse`
* Correct field name in `PrepareAndExecuteRequest` documentation (Lukáš Lalinský)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2083">CALCITE-2083</a>]
  Update documentation
* [<a href="https://issues.apache.org/jira/browse/CALCITE-2036">CALCITE-2036</a>]
  Fix "next" link in `history.html`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1878">CALCITE-1878</a>]
  Update the website for protobuf changes in 1.10.0

## <a href="https://github.com/apache/calcite-avatica/releases/tag/rel/avatica-1.10.0">1.10.0</a> / 2017-05-30
{: #v1-10-0}

Apache Calcite Avatica 1.10.0 is the first release since
[Avatica's git repository](https://gitbox.apache.org/repos/asf/calcite-avatica.git)
separated from
[Calcite's repository](https://gitbox.apache.org/repos/asf/calcite.git) in
[[CALCITE-1717](https://issues.apache.org/jira/browse/CALCITE-1717)].
Avatica now runs on JDK 9 (and continues to run on JDK 7 and 8),
and there is now a Docker image for an Avatica server.
You may now send and receive Array data via the JDBC API.
Several improvements to date/time support in DateTimeUtils.

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 7, 8, 9;
Guava versions 14.0 to 19.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1690">CALCITE-1690</a>]
  Timestamp literals cannot express precision above millisecond
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1539">CALCITE-1539</a>]
  Enable proxy access to Avatica server for third party on behalf of end users
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1756">CALCITE-1756</a>]
  Differentiate between implicitly null and explicitly null `TypedValue`s
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1050">CALCITE-1050</a>]
  Array support for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1746">CALCITE-1746</a>]
  Remove `KEYS` file from git and from release tarball
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1353">CALCITE-1353</a>]
  Convert `first_frame_max_size` to an `int32`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1744">CALCITE-1744</a>]
  Clean up the Avatica poms
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1741">CALCITE-1741</a>]
  Upgrade `maven-assembly-plugin` to version 3.0.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1364">CALCITE-1364</a>]
  Docker images for an Avatica server
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1717">CALCITE-1717</a>]
  Remove Calcite code and lift Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1700">CALCITE-1700</a>]
  De-couple the `HsqldbServer` into a generic JDBC server
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1699">CALCITE-1699</a>]
  Statement may be null
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1667">CALCITE-1667</a>]
  Forbid calls to JDK APIs that use the default locale, time zone or character
  set
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1664">CALCITE-1664</a>]
  `CAST('<string>' as TIMESTAMP)` wrongly adds part of sub-second fraction to the
  value
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1654">CALCITE-1654</a>]
  Avoid generating a string from the Request/Response when it will not be logged
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1609">CALCITE-1609</a>]
  In `DateTimeUtils`, implement `unixDateExtract` and `unixTimeExtract` for more
  time units
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1608">CALCITE-1608</a>]
  Move `addMonths` and `subtractMonths` methods from Calcite class `SqlFunctions`
  to Avatica class `DateTimeUtils`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1600">CALCITE-1600</a>]
  In `Meta.Frame.create()`, change type of `offset` parameter from `int` to `long`
  (Gian Merlino)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1602">CALCITE-1602</a>]
  Remove uses of deprecated APIs
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1599">CALCITE-1599</a>]
  Remove unused `createIterable` call in `LocalService` (Gian Merlino)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1576">CALCITE-1576</a>]
  Use the `protobuf-maven-plugin`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1567">CALCITE-1567</a>]
  JDK9 support
* Remove non-ASCII characters from Java source files
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1538">CALCITE-1538</a>]
  Support `truststore` and `truststore_password` JDBC options
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1485">CALCITE-1485</a>]
  Upgrade Avatica's Apache parent POM to version 18

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1752">CALCITE-1752</a>]
  Use `URLDecoder` instead of manually replacing "%20" in URLs
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1736">CALCITE-1736</a>]
  Address test failures when the path contains spaces
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1568">CALCITE-1568</a>]
  Upgrade `mockito` to 2.5.5

Web site and documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1743">CALCITE-1743</a>]
  Add instructions to release docs to move git tag from `rcN` to `rel/`

## <a href="https://github.com/apache/calcite-avatica/releases/tag/calcite-avatica-1.9.0">1.9.0</a> / 2016-11-01
{: #v1-9-0}

Apache Calcite Avatica 1.9.0 includes various improvements to make it
more robust and secure, while maintaining API and protocol
compatibility with previous versions. We now include non-shaded and
shaded artifacts, to make it easier to embed Avatica in your
application. There is improved support for the JDBC API, including
better type conversions and support for canceling statements. The
transport is upgraded to use protobuf-3.1.0 (previously 3.0 beta).

Compatibility: This release is tested
on Linux, macOS, Microsoft Windows;
using Oracle JDK 1.7, 1.8;
Guava versions 14.0 to 19.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1471">CALCITE-1471</a>]
  `HttpServerSpnegoWithJaasTest.testAuthenticatedClientsAllowed` fails on Windows
  (Aaron Mihalik)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1464">CALCITE-1464</a>]
  Upgrade Jetty version to 9.2.19v20160908
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1463">CALCITE-1463</a>]
  In `standalone-server` jar, relocate dependencies rather than excluding them
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1355">CALCITE-1355</a>]
  Upgrade to protobuf-java 3.1.0
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1462">CALCITE-1462</a>]
  Remove Avatica pom cruft
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1458">CALCITE-1458</a>]
  Add column values to the deprecated protobuf attribute
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1433">CALCITE-1433</a>]
  Add missing dependencies to `avatica-server`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1433">CALCITE-1433</a>]
  Fix missing avatica `test-jar` dependency
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1423">CALCITE-1423</a>]
  Add method `ByteString.indexOf(ByteString, int)`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1408">CALCITE-1408</a>]
  `ResultSet.getXxx` methods should throw `SQLDataException` if cannot convert to
  the requested type (Laurent Goujon)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1410">CALCITE-1410</a>]
  Fix JDBC metadata classes (Laurent Goujon)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1224">CALCITE-1224</a>]
  Publish non-shaded and shaded versions of Avatica client artifacts
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1407">CALCITE-1407</a>]
  `MetaImpl.fieldMetaData` wrongly uses 1-based column ordinals
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1361">CALCITE-1361</a>]
  Remove entry from `AvaticaConnection.flagMap` when session closed
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1399">CALCITE-1399</a>]
  Make the jcommander `SerializationConverter` public
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1394">CALCITE-1394</a>]
  Javadoc warnings due to `CoreMatchers.containsString` and `mockito-all`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1390">CALCITE-1390</a>]
  Avatica JDBC driver wrongly modifies `Properties` object
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1371">CALCITE-1371</a>]
  `PreparedStatement` does not process Date type correctly (Billy (Yiming) Liu)
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1301">CALCITE-1301</a>]
  Add `cancel` flag to `AvaticaStatement`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1315">CALCITE-1315</a>]
  Retry the request on `NoHttpResponseException`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1300">CALCITE-1300</a>]
  Retry on HTTP-503 in hc-based `AvaticaHttpClient`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1263">CALCITE-1263</a>]
  Case-insensitive match and null default value for `enum` properties
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1282">CALCITE-1282</a>]
  Adds an API method to set extra allowed Kerberos realms

Tests

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1226">CALCITE-1226</a>]
  Disable `AvaticaSpnegoTest` due to intermittent failures

Web site and documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1369">CALCITE-1369</a>]
  Add list of Avatica clients to the web site
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1229">CALCITE-1229</a>]
  Restore API and Test API links to site
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1287">CALCITE-1287</a>]
  TCK test for binary columns
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1285">CALCITE-1285</a>]
  Fix client URL template in example config file

## <a href="https://github.com/apache/calcite-avatica/releases/tag/calcite-avatica-1.8.0">1.8.0</a> / 2016-06-04
{: #v1-8-0}

Apache Calcite Avatica 1.8.0 continues the focus on compatibility with previous
versions while also adding support for authentication between Avatica client and server.
Performance, notably on the write-path, is also major area of improvement
in this release, increasing as much as two to three times over previous versions
with the addition of new API support. The documentation for both users and developers
continues to receive improvements.

A number of protocol issues are resolved relating to the proper serialization of
decimals, the wire-API semantics of signed integers that were marked as unsigned
integers, and the unintentional Base64-encoding of binary data using the Protocol
Buffers serialization in Avatica. These issues were fixed in such a way to be
backwards compatible, but older clients/servers may still compute incorrect data.

Users of Avatica 1.7.x should not notice any issues in upgrading existing applications
and are encouraged to upgrade as soon as possible.

Features and bug fixes

* [<a href='https://issues.apache.org/jira/browse/CALCITE-1159'>CALCITE-1159</a>]
  Support Kerberos-authenticated clients using SPNEGO
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1173'>CALCITE-1173</a>]
  Basic and Digest authentication
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1249'>CALCITE-1249</a>]
  L&N incorrect for source and non-shaded jars for avatica-standalone-server module
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1103'>CALCITE-1103</a>]
  Decimal data serialized as Double in Protocol Buffer API
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1205'>CALCITE-1205</a>]
  Inconsistency in protobuf TypedValue field names
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1207'>CALCITE-1207</a>]
  Allow numeric connection properties, and K, M, G suffixes
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1209'>CALCITE-1209</a>]
  Byte strings not being correctly decoded when sent to avatica using protocol buffers
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1213'>CALCITE-1213</a>]
  Changing AvaticaDatabaseMetaData from class to interface breaks compatibility
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1218'>CALCITE-1218</a>]
  Mishandling of uncaught exceptions results in no ErrorResponse sent to client
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1230'>CALCITE-1230</a>]
  Add SQLSTATE reference data as enum SqlState
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1243'>CALCITE-1243</a>]
  max_row_count in protobuf Requests should be signed int
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1247'>CALCITE-1247</a>]
  JdbcMeta#prepare doesn't set maxRowCount on the Statement
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1254'>CALCITE-1254</a>]
  Support PreparedStatement.executeLargeBatch
* [<a href='https://issues.apache.org/jira/browse/CALCITE-643'>CALCITE-643</a>]
  User authentication for avatica clients
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1128'>CALCITE-1128</a>]
  Support addBatch()/executeBatch() in remote driver
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1179'>CALCITE-1179</a>]
  Extend list of time units and time unit ranges
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1180'>CALCITE-1180</a>]
  Support clearBatch() in remote driver
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1185'>CALCITE-1185</a>]
  Send back ErrorResponse on failure to parse requests
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1192'>CALCITE-1192</a>]
  Document protobuf and json REP types with examples
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1214'>CALCITE-1214</a>]
  Support url-based kerberos login
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1236'>CALCITE-1236</a>]
  Log exceptions sent back to client in server log
* [<a href='https://issues.apache.org/jira/browse/CALCITE-836'>CALCITE-836</a>]
  Provide a way for the Avatica client to query the server versions
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1156'>CALCITE-1156</a>]
  Bump jetty version
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1184'>CALCITE-1184</a>]
  Update Kerby dependency to 1.0.0-RC2

Tests

* [<a href='https://issues.apache.org/jira/browse/CALCITE-1190'>CALCITE-1190</a>]
  Cross-Version Compatibility Test Harness
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1113'>CALCITE-1113</a>]
  Parameter precision and scale are not returned from Avatica REST API
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1186'>CALCITE-1186</a>]
  Parameter 'signed' metadata is always returned as false
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1189'>CALCITE-1189</a>]
  Unit test failure when JVM default charset is not UTF-8
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1061'>CALCITE-1061</a>]
  RemoteMetaTest#testRemoteStatementInsert's use of hsqldb isn't guarded
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1194'>CALCITE-1194</a>]
  Avatica metrics has non-test dependency on JUnit
* [<a href='https://issues.apache.org/jira/browse/CALCITE-835'>CALCITE-835</a>]
  Unicode character seems to be handled incorrectly in Avatica

Web site and documentation

* [<a href='https://issues.apache.org/jira/browse/CALCITE-1251'>CALCITE-1251</a>]
  Write release notes
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1201'>CALCITE-1201</a>]
  Bad character in JSON docs
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1267'>CALCITE-1267</a>]
  Point to release notes on website in README
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1163'>CALCITE-1163</a>]
  Avatica sub-site logo leads to Calcite site instead of Avatica's
* [<a href='https://issues.apache.org/jira/browse/CALCITE-1202'>CALCITE-1202</a>]
  Lock version of Jekyll used by website

## <a href="https://github.com/apache/calcite-avatica/releases/tag/calcite-avatica-1.7.1">1.7.1</a> / 2016-03-18
{: #v1-7-1}

This is the first release of Avatica as an independent project. (It
is still governed by Apache Calcite's PMC, and stored in the same git
repository as Calcite, but releases are no longer synchronized, and
Avatica does not depend on any Calcite modules.)

One notable technical change is that we have replaced JUL (`java.util.logging`)
with [SLF4J](http://slf4j.org/). SLF4J provides an API that Avatica can use
independent of the logging implementation. This is more
flexible for users: they can configure Avatica's logging within their
own chosen logging framework. This work was done in
[[CALCITE-669](https://issues.apache.org/jira/browse/CALCITE-669)].

If you have configured JUL in Calcite/Avatica previously, you'll
notice some differences, because JUL's `FINE`, `FINER` and `FINEST`
logging levels do not exist in SLF4J. To deal with this, we mapped
`FINE` to SLF4J's `DEBUG` level, and mapped `FINER` and `FINEST` to
SLF4J's `TRACE`.

The performance of Avatica was an important focus for this release as well.
Numerous improvements aimed at reducing the overall latency of Avatica RPCs
was reduced. Some general testing showed an overall reduction of latency
by approximately 15% over the previous release.

Compatibility: This release is tested on Linux, Mac OS X, Microsoft
Windows; using Oracle JDK 1.7, 1.8; Guava versions 12.0.1 to 19.0;
other software versions as specified in `pom.xml`.

Features and bug fixes

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1156">CALCITE-1156</a>]
  Upgrade Jetty from 9.2.7.v20150116 to 9.2.15.v20160210
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1141">CALCITE-1141</a>]
  Bump `version.minor` for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1132">CALCITE-1132</a>]
  Update `artifactId`, `groupId` and `name` for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1064">CALCITE-1064</a>]
  Address problematic `maven-remote-resources-plugin`
* In `TimeUnit` add `WEEK`, `QUARTER`, `MICROSECOND` values, and change type of
  `multiplier`
* Update `groupId` when Calcite POMs reference Avatica modules
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1078">CALCITE-1078</a>]
  Detach avatica from the core calcite Maven project
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1117">CALCITE-1117</a>]
  Default to a `commons-httpclient` implementation
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1118">CALCITE-1118</a>]
  Add a noop-JDBC driver for testing Avatica server
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1119">CALCITE-1119</a>]
  Additional metrics instrumentation for request processing
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1094">CALCITE-1094</a>]
  Replace `ByteArrayOutputStream` to avoid synchronized writes
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1092">CALCITE-1092</a>]
  Use singleton descriptor instances for protobuf field presence checks
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1093">CALCITE-1093</a>]
  Reduce impact of `ArrayList` performance
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1086">CALCITE-1086</a>]
  Avoid sending `Signature` on `Execute` for updates
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1031">CALCITE-1031</a>]
  In prepared statement, `CsvScannableTable.scan` is called twice
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1085">CALCITE-1085</a>]
  Use a `NoopContext` singleton in `NoopTimer`
* [<a href="https://issues.apache.org/jira/browse/CALCITE-642">CALCITE-642</a>]
  Add an avatica-metrics API
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1071">CALCITE-1071</a>]
  Improve hash functions
* [<a href="https://issues.apache.org/jira/browse/CALCITE-669">CALCITE-669</a>]
  Mass removal of Java Logging for SLF4J
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1067">CALCITE-1067</a>]
  Test failures due to clashing temporary table names
* [<a href="https://issues.apache.org/jira/browse/CALCITE-999">CALCITE-999</a>]
  Clean up maven POM files

Web site and documentation

* [<a href="https://issues.apache.org/jira/browse/CALCITE-1142">CALCITE-1142</a>]
  Create a `README` for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1144">CALCITE-1144</a>]
  Fix `LICENSE` for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1143">CALCITE-1143</a>]
  Remove unnecessary `NOTICE` for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1139">CALCITE-1139</a>]
  Update Calcite's `KEYS` and add a copy for Avatica
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1140">CALCITE-1140</a>]
  Release notes and website updates for Avatica 1.7
* Instructions for Avatica site
* New logo and color scheme for Avatica site
* [<a href="https://issues.apache.org/jira/browse/CALCITE-1079">CALCITE-1079</a>]
  Split out an Avatica website, made to slot into the Calcite site at `/avatica`

## Past releases

Prior to release 1.7.1, Avatica was released as part of Calcite. Maven
modules had groupId 'org.apache.calcite' and module names
'calcite-avatica', 'calcite-avatica-server' etc.

Please refer to the
[Calcite release page](https://calcite.apache.org/docs/history.html)
for information about previous Avatica releases.
