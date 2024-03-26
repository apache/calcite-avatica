---
layout: docs
title: Avatica HOWTO
permalink: /docs/howto.html
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

Here's some miscellaneous documentation about using Avatica.

* TOC
{:toc}

## Building from a source distribution

Prerequisites are Java (JDK 8 or later)
and Gradle (version 8.5) on your path.

(The source distribution
[does not include the Gradle wrapper](https://issues.apache.org/jira/browse/CALCITE-4575);
therefore you need to
[install Gradle manually](https://gradle.org/releases/).)

Unpack the source distribution `.tar.gz` file,
`cd` to the root directory of the unpacked source,
then build using Gradle:

{% highlight bash %}
$ tar xvfz apache-calcite-avatica-1.24.0-src.tar.gz
$ cd apache-calcite-avatica-1.24.0-src
$ gradle build
{% endhighlight %}

[Running tests](#running-tests) describes how to run more or fewer
tests (but you should use the `gradle` command rather than
`./gradlew`).

## Building from Git

Prerequisites are Git,
and Java (JDK 8 or later) on your path.

Create a local copy of the GitHub repository,
`cd` to its root directory,
then build using Gradle:

{% highlight bash %}
$ git clone git://github.com/apache/calcite-avatica.git avatica
$ cd avatica
$ ./gradlew build
{% endhighlight %}

[Running tests](#running-tests) describes how to run more or fewer
tests.

## Running tests

The test suite will run by default when you build, unless you specify
`-x test`

{% highlight bash %}
$ ./gradlew assemble # build the artifacts
$ ./gradlew build -x test # build the artifacts, verify code style, skip tests
$ ./gradlew check # verify code style, execute tests
$ ./gradlew test # execute tests
$ ./gradlew checkstyleMain checkstyleTest # verify code style
{% endhighlight %}

You can use `./gradlew assemble` to build the artifacts and skip all tests and verifications.

### To run tests in docker:

Prerequisites are [Docker](https://docs.docker.com/install/) and
[Docker Compose](https://docs.docker.com/compose/install/).

{% highlight bash %}
docker-compose run test
{% endhighlight %}

## Contributing

See the [developers guide]({{ site.baseurl }}/develop/#contributing).

## Getting started

See the [developers guide]({{ site.baseurl }}/develop/#getting-started).

# Advanced topics for developers

The following sections might be of interest if you are adding features
to particular parts of the code base. You don't need to understand
these topics if you are just building from source and running tests.

# Advanced topics for committers

The following sections are of interest to Calcite committers and in
particular release managers.

## Set up PGP signing keys (for Calcite committers)

Follow instructions [here](http://www.apache.org/dev/release-signing) to
create a key pair. (On Mac OS X, I did `brew install gpg` and
`gpg --gen-key`.)

Add your public key to the
[`KEYS`](https://dist.apache.org/repos/dist/release/calcite/KEYS)
file by following instructions in the `KEYS` file.
(The `KEYS` file is not present in the git repo or in a release tar
ball because that would be
[redundant](https://issues.apache.org/jira/browse/CALCITE-1746).)

## Run a GPG agent

By default, Gradle plugins which require you to unlock a GPG secret key
will prompt you in the terminal. To prevent you from having to enter
this password numerous times, it is highly recommended to install and
run `gpg-agent`.

This can be started automatically via an `~/.xsession` on Linux or some
scripting in your shell's configuration script of choice (e.g. `~/.bashrc` or `~/.zshrc`)

{% highlight bash %}
GPG_AGENT=$(which gpg-agent)
GPG_TTY=`tty`
export GPG_TTY
if [[ -f "$GPG_AGENT" ]]; then
  envfile="${HOME}/.gnupg/gpg-agent.env"

  if test -f "$envfile" && kill -0 $(grep GPG_AGENT_INFO "$envfile" | cut -d: -f 2) 2>/dev/null; then
      source "$envfile"
  else
      eval "$(gpg-agent --daemon --log-file=~/.gpg/gpg.log --write-env-file "$envfile")"
  fi
  export GPG_AGENT_INFO  # the env file does not contain the export statement
fi
{% endhighlight %}

Also, ensure that `default-cache-ttl 6000` is set in `~/.gnupg/gpg-agent.conf`
to guarantee that your credentials will be cached for the duration of the build.

## Set up Nexus repository credentials (for Calcite committers)

Gradle provides multiple ways to [configure project properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
For instance, you could update `$HOME/.gradle/gradle.properties`.

Note: the build script would print the missing properties, so you can try running it and let it complain on the missing ones.

The following options are used:

{% highlight properties %}
asfCommitterId=
asfNexusUsername=
asfNexusPassword=
asfSvnUsername=
asfSvnPassword=
{% endhighlight %}

When
[asflike-release-environment](https://github.com/vlsi/asflike-release-environment)
is used, the credentials are taken from `asfTest...`
(e.g. `asfTestNexusUsername=test`)

Note: if you want to uses `gpg-agent`, you need to pass `useGpgCmd` property, and specify the key id
via `signing.gnupg.keyName`.

## Making a snapshot (for Calcite committers)

Before you start:

* Set up signing keys as described above.
* Make sure you are using JDK 8 (not 9 or 10).

{% highlight bash %}
# Make sure that there are no junk files in the sandbox
git clean -xn

./gradlew -Pasf publish
{% endhighlight %}

## Making a release candidate (for Calcite committers)

Before you start:

* Set up signing keys as described above.
* Make sure you are using JDK 8 (not 9 or 10).
* Check that `README`, `site/_docs/howto.md`, `site/_docs/docker_images.md` have the correct version number.
* Check that `site/_docs/howto.md` has the correct Gradle version.
* Check that `NOTICE` has the current copyright year.
* Check that `calcite.avatica.version` has the proper value in `/gradle.properties`.
* Add release notes to `site/_docs/history.md`. If release notes already exist for the version to be released, but
  are commented out, remove the comments (`{% raw %}{% comment %}{% endraw %}` and `{% raw %}{% endcomment %}{% endraw %}`). Include the commit history,
  names of people who contributed to the release, and say which versions of Java, Guava and operating systems the
  release is tested against.
* Generate a report of vulnerabilities that occur among dependencies,
  using `./gradlew dependencyCheckUpdate dependencyCheckAggregate`.
* Make sure that
  <a href="https://issues.apache.org/jira/issues/?jql=project%20%3D%20CALCITE%20AND%20status%20%3D%20Resolved%20and%20fixVersion%20is%20null">
  every "resolved" JIRA case</a> (including duplicates) has
  a fix version assigned (most likely the version we are
  just about to release)

The release candidate process does not add commits,
so there's no harm if it fails. It might leave `-rc` tag behind
which can be removed if required.

You can perform a dry-run release with a help of
[vlsi/asflike-release-environment](https://github.com/vlsi/asflike-release-environment).
That performs the same steps, however it pushes changes to the mock Nexus, Git, and SVN servers.

If any of the steps fail, fix the problem, and
start again from the top.

### Prepare a release candidate directly in your environment

Pick a release candidate index (starting from 0) and ensure it does
not interfere with previous candidates for the version.

{% highlight bash %}
# Make sure that there are no junk files in the sandbox
git clean -xn

# Dry run the release candidate (push to asf-like-environment)
./gradlew prepareVote -Prc=0

# Push release candidate to ASF servers
./gradlew prepareVote -Prc=0 -Pasf
{% endhighlight %}

### Prepare a release candidate in Docker

* You will need to have [Docker](https://docs.docker.com/install/) and [Docker Compose](https://docs.docker.com/compose/install/) installed.

* The script expects you to mount your `~/.gnupg` directory into the `/.gnupg` directory in the container. Once mounted into the container,
the script will make a copy of the contents and move it to a different location, so that it will not modify the contents of your original
`~/.gnupg` directory during the build.

* Start the [asflike-release-environment](https://github.com/vlsi/asflike-release-environment) to prepare a staging environment for a dry-run.

{% highlight bash %}
# On Linux (dry-run):
docker-compose run -v ~/.gnupg:/.gnupg dry-run

# On Windows (dry-run):
docker-compose run -v /c/Users/username/AppData/Roaming/gnupg:/.gnupg dry-run

# On Linux (push to ASF servers):
docker-compose run -v ~/.gnupg:/.gnupg publish-release-for-voting

# On Windows (push to ASF servers):
docker-compose run -v /c/Users/username/AppData/Roaming/gnupg:/.gnupg publish-release-for-voting
{% endhighlight %}

## Checking the artifacts

* In the `release/build/distributions` directory should be these 3 files, among others:
  * apache-calcite-avatica-X.Y.Z-src.tar.gz
  * apache-calcite-avatica-X.Y.Z-src.tar.gz.asc
  * apache-calcite-avatica-X.Y.Z-src.tar.gz.sha512
* Note that the file names start `apache-calcite-avatica-`.
* In the source distro `.tar.gz` (currently there is
  no binary distro), check that all files belong to a directory called
  `apache-calcite-avatica-X.Y.Z-src`.
* That directory must contain files `NOTICE`, `LICENSE`,
  `README`, `README.md`
  * Check that the version in `README` is correct
  * Check that `LICENSE` is identical to the file checked into git
* Make sure that the following files do not occur in the source
  distros: `KEYS`, `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`,
  `gradle-wrapper.properties`
* For each .jar (for example `core/build/libs/avatica-core-X.Y.Z.jar`
  and `server/build/libs/avatica-server-X.Y.Z-sources.jar`),
  verify that the `META-INF` directory contains the correct
  contents for `LICENSE` and `NOTICE` per the
  source/classes contained. Refer to the ASF licensing documentation on
  what is required.
* Check PGP, per [this](https://httpd.apache.org/dev/verification.html)

Verify the staged artifacts in the Nexus repository:

* Go to [https://repository.apache.org/](https://repository.apache.org/) and login
* Under `Build Promotion`, click `Staging Repositories`
* In the `Staging Repositories` tab there should be a line with profile `org.apache.calcite`
* Navigate through the artifact tree and make sure the .jar, .pom, .asc files are present

## Cleaning up after a failed release attempt (for Calcite committers)

If something is not correct, you can fix it, commit it, and prepare the next candidate.
The release candidate tags might be kept for a while.

## Validate a release

{% highlight bash %}
# Check that the signing key (e.g. 2AD3FAE3) is pushed
gpg --recv-keys key

# Check keys
curl -O https://dist.apache.org/repos/dist/release/calcite/KEYS

# Sign/check sha512 hashes
# (Assumes your O/S has a 'shasum' command.)
function checkHash() {
  cd "$1"
  for i in *.{pom,gz}; do
    if [ ! -f $i ]; then
      continue
    fi
    if [ -f $i.sha512 ]; then
      if [ "$(cat $i.sha512)" = "$(shasum -a 512 $i)" ]; then
        echo $i.sha512 present and correct
      else
        echo $i.sha512 does not match
      fi
    else
      shasum -a 512 $i > $i.sha512
      echo $i.sha512 created
    fi
  done
}
checkHash apache-calcite-avatica-X.Y.Z-rcN
{% endhighlight %}

## Get approval for a release via Apache voting process (for Calcite committers)

Release vote on dev list.
Note: the draft mail is printed as the final step of `prepareVote` task,
and you can find the draft in `/build/prepareVote/mail.txt`

{% highlight text %}
To: dev@calcite.apache.org
Subject: [VOTE] Release apache-calcite-avatica-X.Y.Z (release candidate N)

Hi all,

I have created a build for Apache Calcite Avatica X.Y.Z, release candidate N.

Thanks to everyone who has contributed to this release.
<Further details about release.> You can read the release notes here:
https://github.com/apache/calcite-avatica/blob/XXXX/site/_docs/history.md

The commit to be voted upon:
https://gitbox.apache.org/repos/asf/calcite-avatica/commit/NNNNNN

Its hash is XXXX.

The artifacts to be voted on are located here:
https://dist.apache.org/repos/dist/dev/calcite/apache-calcite-avatica-X.Y.Z-rcN/

The hashes of the artifacts are as follows:
src.tar.gz.sha512 XXXX

A staged Maven repository is available for review at:
https://repository.apache.org/content/repositories/orgapachecalcite-NNNN

Release artifacts are signed with the following key:
https://people.apache.org/keys/committer/jhyde.asc

Please vote on releasing this package as Apache Calcite Avatica X.Y.Z.

The vote is open for the next 72 hours and passes if a majority of
at least three +1 PMC votes are cast.

[ ] +1 Release this package as Apache Calcite Avatica X.Y.Z
[ ]  0 I don't feel strongly about it, but I'm okay with the release
[ ] -1 Do not release this package because...


Here is my vote:

+1 (binding)

Julian
{% endhighlight %}

After vote finishes, send out the result:

{% highlight text %}
Subject: [RESULT] [VOTE] Release apache-calcite-avatica-X.Y.Z (release candidate N)
To: dev@calcite.apache.org

Thanks to everyone who has tested the release candidate and given
their comments and votes.

The tally is as follows.

N binding +1s:
<names>

N non-binding +1s:
<names>

No 0s or -1s.

Therefore I am delighted to announce that the proposal to release
Apache Calcite Avatica X.Y.Z has passed.

Thanks everyone. We’ll now roll the release out to the mirrors.

There was some feedback during voting. I shall open a separate
thread to discuss.


Julian
{% endhighlight %}

Use the [Apache URL shortener](http://s.apache.org) to generate
shortened URLs for the vote proposal and result emails. Examples:
[s.apache.org/calcite-1.2-vote](http://s.apache.org/calcite-1.2-vote) and
[s.apache.org/calcite-1.2-result](http://s.apache.org/calcite-1.2-result).


## Publishing a release (for Calcite committers)

After a successful release vote, we need to push the release
out to mirrors, and other tasks.

Choose a release date.
This is based on the time when you expect to announce the release.
This is usually a day after the vote closes.
Remember that UTC date changes at 4pm Pacific time.

In JIRA, search for
[all issues resolved in this release](https://issues.apache.org/jira/issues/?jql=project%20%3D%20CALCITE%20and%20fixVersion%20%3D%201.5.0%20and%20status%20%3D%20Resolved%20and%20resolution%20%3D%20Fixed),
and do a bulk update changing their status to "Closed",
with a change comment
"Resolved in release X.Y.Z (YYYY-MM-DD)"
(fill in release number and date appropriately).
Uncheck "Send mail for this update".

Tip: Push the git tag only after the staged nexus artifacts are promoted in the repository. This is because pushing the
tag triggers Docker Hub to start building the docker images immediately and the build will pull in the promoted artifacts.
If the artifacts are not yet available, the build on Docker Hub will fail. It's best to continue with the following steps
after you have confirmed that the nexus artifacts were promoted properly.

### Publishing directly in your environment

{% highlight bash %}
# Dry run publishing the release (push to asf-like-environment)
./gradlew publishDist -Prc=0

# Publish the release to ASF servers
./gradlew publishDist -Prc=0 -Pasf
{% endhighlight %}

If there are more than 2 releases in SVN (see https://dist.apache.org/repos/dist/release/calcite),
clear out the oldest ones:

{% highlight bash %}
svn rm https://dist.apache.org/repos/dist/release/calcite/apache-calcite-avatica-X.Y.Z
{% endhighlight %}

The old releases will remain available in the
[release archive](http://archive.apache.org/dist/calcite/).

### Publishing a release using docker

This assumes that a rc release was tagged and pushed to the git repository.

{% highlight bash %}
docker-compose run promote-release
{% endhighlight %}

## Add release notes and announce the release

Add a release note by copying
[site/_posts/2016-11-01-release-1.9.0.md]({{ site.sourceRoot }}/site/_posts/2016-11-01-release-1.9.0.md),
update the version number in `gradle.properties`,
generate the javadoc and copy to `site/target/avatica/javadocAggregate`,
[publish the site](#publish-the-web-site),
and check that it appears in the contents in [news](http://localhost:4000/news/).

After 24 hours, announce the release by sending an email to
[announce@apache.org](https://mail-archives.apache.org/mod_mbox/www-announce/).
You can use
[the 1.8.0 announcement](https://mail-archives.apache.org/mod_mbox/www-announce/201606.mbox/%3C57559CC7.1000402@apache.org%3E)
as a template. Be sure to include a brief description of the project.

## Publishing the web site (for Calcite committers)
{: #publish-the-web-site}

See instructions in
[site/README.md]({{ site.sourceRoot }}/site/README.md).
