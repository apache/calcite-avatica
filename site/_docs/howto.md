---
layout: docs
title: HOWTO
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

Prerequisites are maven (3.2.1 or later)
and Java (JDK 8 or later) on your path.

Unpack the source distribution `.tar.gz` file,
`cd` to the root directory of the unpacked source,
then build using maven:

{% highlight bash %}
$ tar xvfz apache-calcite-avatica-1.12.0-src.tar.gz
$ cd apache-calcite-avatica-1.12.0-src
$ mvn install
{% endhighlight %}

[Running tests](#running-tests) describes how to run more or fewer
tests.

## Building from git

Prerequisites are git, maven (3.2.1 or later)
and Java (JDK 8 or later) on your path.

Create a local copy of the github repository,
`cd` to its root directory,
then build using maven:

{% highlight bash %}
$ git clone git://github.com/apache/calcite-avatica.git avatica
$ cd avatica
$ mvn install
{% endhighlight %}

[Running tests](#running-tests) describes how to run more or fewer
tests.

## Running tests

The test suite will run by default when you build, unless you specify
`-DskipTests`:

{% highlight bash %}
$ mvn clean verify -Dcheckstyle.skip
{% endhighlight %}

By default, invoking the `verify` Maven lifecycle phase will also cause checkstyle
rules to be run. It is expected that contributions pass the checkstyle rules; however,
it is common to ignore these while working on a feature/bug and fix them at the end.

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

By default, Maven plugins which require you to unlock a GPG secret key
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

## Set up Maven repository credentials (for Calcite committers)

Follow the instructions [here](http://www.apache.org/dev/publishing-maven-artifacts.html#dev-env) to add your credentials to your maven configuration.

## Making a snapshot (for Calcite committers)

Before you start:

* Set up signing keys as described above.
* Make sure you are using JDK 8 (not 9 or 10).

{% highlight bash %}
# Make sure that there are no junk files in the sandbox
git clean -xn

mvn -Papache-release clean install
{% endhighlight %}

When the dry-run has succeeded, change `install` to `deploy`.

## Making a release (for Calcite committers)

Before you start:

* Set up signing keys as described above.
* Make sure you are using JDK 8 (not 9 or 10).
* Check that `README`, `site/_docs/howto.md`, `site/_docs/docker_images.md`,
  and `docker/src/main/dockerhub/Dockerfile` have the correct version number.
* Check that `NOTICE` has the current copyright year.
* Set `version.major` and `version.minor` in `pom.xml`.
* Add release notes to `site/_docs/history.md`. Include the commit history,
  and say which versions of Java, Guava and operating systems the release is
  tested against.
* Generate a report of vulnerabilities that occur among dependencies,
  using `mvn verify -Ppedantic`.
* Make sure that
  <a href="https://issues.apache.org/jira/issues/?jql=project%20%3D%20CALCITE%20AND%20status%20%3D%20Resolved%20and%20fixVersion%20is%20null">
  every "resolved" JIRA case</a> (including duplicates) has
  a fix version assigned (most likely the version we are
  just about to release)

Create a release branch named after the release, e.g.
`branch-avatica-1.9`, and push it to Apache.

{% highlight bash %}
$ git checkout -b branch-avatica-X.Y
$ git push -u origin branch-avatica-X.Y
{% endhighlight %}

We will use the branch for the entire the release process. Meanwhile,
we do not allow commits to the master branch. After the release is
final, we can use `git merge --ff-only` to append the changes on the
release branch onto the master branch. (Apache does not allow reverts
to the master branch, which makes it difficult to clean up the kind of
messy commits that inevitably happen while you are trying to finalize
a release.)

Now, set up your environment and do a dry run. The dry run will not
commit any changes back to git and gives you the opportunity to verify
that the release process will complete as expected.

If any of the steps fail, clean up (see below), fix the problem, and
start again from the top.

{% highlight bash %}
# Make sure that there are no junk files in the sandbox
git clean -xn

# For the dry run, edit the docker/dockerhub/Dockerfile
patch -p1 <<EOF
diff --git a/docker/src/main/dockerhub/Dockerfile b/docker/src/main/dockerhub/Dockerfile
index 4617a4e..4ccd97f 100644
--- a/docker/src/main/dockerhub/Dockerfile
+++ b/docker/src/main/dockerhub/Dockerfile
@@ -23,3 +23,3 @@ RUN mkdir -p /home/avatica/classpath
# This line must be preserved. The Maven build will verify this version matches its version
-ARG AVATICA_VERSION="1.12.0"
+ARG AVATICA_VERSION="1.12.0-SNAPSHOT"
EOF

# Do a dry run of the release:prepare step, which sets version numbers.
# Typically we increment minor version: If X.Y.Z is 1.11.0, X2.Y2.Z2 is 1.12.0.
# Note X.Y.Z is the current version we're trying to release, and X2.Y2.Z2 is the next development version.
# For example, if I am currently building a release for 1.11.0, X.Y.Z would be 1.11.0 and X2.Y2.Z2 would be 1.12.0.
mvn -DdryRun=true -DreleaseVersion=X.Y.Z -DdevelopmentVersion=X2.Y2.Z2-SNAPSHOT -Dtag=avatica-X.Y.Z-rcN -Papache-release -Duser.name=${asf.username} release:prepare

# If you have multiple GPG keys, you can select the key used to sign the release by appending `-Darguments=-Dgpg.keyname=your_key_id`:
mvn -DdryRun=true -DreleaseVersion=X.Y.Z -DdevelopmentVersion=X2.Y2.Z2-SNAPSHOT -Dtag=avatica-X.Y.Z-rcN -Papache-release -Duser.name=${asf.username} release:prepare -Darguments=-Dgpg.keyname=your_key_id

{% endhighlight %}

Check the artifacts:

* In the `target` directory should be these 6 files, among others:
  * apache-calcite-avatica-X.Y.Z-src.tar.gz
  * apache-calcite-avatica-X.Y.Z-src.tar.gz.asc
  * apache-calcite-avatica-X.Y.Z-src.tar.gz.sha256
* Note that the file names start `apache-calcite-avatica-`.
* In the source distro `.tar.gz` (currently there is
  no binary distro), check that all files belong to a directory called
  `apache-calcite-avatica-X.Y.Z-src`.
* That directory must contain files `NOTICE`, `LICENSE`,
  `README`, `README.md`
  * Check that the version in `README` is correct
* Make sure that there is no `KEYS` file in the source distros
* For each .jar (for example `core/target/avatica-core-X.Y.Z.jar`
  and `server/target/avatica-server-X.Y.Z-sources.jar`),
  verify that the `META-INF` directory contains the correct
  contents for `DEPENDENCIES`, `LICENSE` and `NOTICE` per the
  source/classes contained. Refer to the ASF licensing documentation on
  what is required.
* Check PGP, per [this](https://httpd.apache.org/dev/verification.html)

If something is not correct, you can invoke the `release:clean` mojo to remove the
generated files from your workspace:

{% highlight bash %}
mvn release:clean
{% endhighlight %}

If successful, remove the `-DdryRun` flag and run the release for real.

{% highlight bash %}
# Prepare sets the version numbers, creates a tag, and pushes it to git.
# Typically we increment minor version: If X.Y.Z is 1.11.0, X2.Y2.Z2 is 1.12.0.
# Note X.Y.Z is the current version we're trying to release, and X2.Y2.Z2 is the next development version.
# For example, if I am currently building a release for 1.11.0, X.Y.Z would be 1.11.0 and X2.Y2.Z2 would be 1.12.0.
mvn -DreleaseVersion=X.Y.Z -DdevelopmentVersion=X2.Y2.Z2-SNAPSHOT -Dtag=avatica-X.Y.Z-rcN -Papache-release -Duser.name=${asf.username} release:prepare

# If you have multiple GPG keys, you can select the key used to sign the release by appending `-Darguments=-Dgpg.keyname=your_key_id`:
mvn -DreleaseVersion=X.Y.Z -DdevelopmentVersion=X2.Y2.Z2-SNAPSHOT -Dtag=avatica-X.Y.Z-rcN -Papache-release -Duser.name=${asf.username} release:prepare -Darguments=-Dgpg.keyname=your_key_id

# Perform checks out the tagged version, builds, and deploys to the staging repository
mvn -Papache-release -Duser.name=${asf.username} release:perform -Darguments="-DskipTests"
{% endhighlight %}

Verify the staged artifacts in the Nexus repository:

* Go to [https://repository.apache.org/](https://repository.apache.org/) and login
* Under `Build Promotion`, click `Staging Repositories`
* In the `Staging Repositories` tab there should be a line with profile `org.apache.calcite`
* Navigate through the artifact tree and make sure the .jar, .pom, .asc files are present
* Check the box on in the first column of the row,
  and press the 'Close' button to publish the repository at
  https://repository.apache.org/content/repositories/orgapachecalcite-1000
  (or a similar URL)

Upload the artifacts via subversion to a staging area,
https://dist.apache.org/repos/dist/dev/calcite/apache-calcite-avatica-X.Y.Z-rcN:

{% highlight bash %}
# Create a subversion workspace, if you haven't already
mkdir -p ~/dist/dev
pushd ~/dist/dev
svn co https://dist.apache.org/repos/dist/dev/calcite
popd

# Move the files into a directory
cd target
mkdir ~/dist/dev/calcite/apache-calcite-avatica-X.Y.Z-rcN
mv apache-calcite-avatica-* ~/dist/dev/calcite/apache-calcite-avatica-X.Y.Z-rcN

# Check in
cd ~/dist/dev/calcite
svn add apache-calcite-avatica-X.Y.Z-rcN
svn ci
{% endhighlight %}

## Cleaning up after a failed release attempt (for Calcite committers)

{% highlight bash %}
# Make sure that the tag you are about to generate does not already
# exist (due to a failed release attempt)
git tag

# If the tag exists, delete it locally and remotely
git tag -d avatica-X.Y.Z
git push origin :refs/tags/avatica-X.Y.Z

# Remove modified files
mvn release:clean

# Check whether there are modified files and if so, go back to the
# original git commit
git status
git reset --hard HEAD
{% endhighlight %}

## Validate a release

{% highlight bash %}
# Check that the signing key (e.g. 2AD3FAE3) is pushed
gpg --recv-keys key

# Check keys
curl -O https://dist.apache.org/repos/dist/release/calcite/KEYS

# Sign/check sha256 hashes
# (Assumes your O/S has a 'shasum' command.)
function checkHash() {
  cd "$1"
  for i in *.{pom,gz}; do
    if [ ! -f $i ]; then
      continue
    fi
    if [ -f $i.sha256 ]; then
      if [ "$(cat $i.sha256)" = "$(shasum -a 256 $i)" ]; then
        echo $i.sha256 present and correct
      else
        echo $i.sha256 does not match
      fi
    else
      shasum -a 256 $i > $i.sha256
      echo $i.sha256 created
    fi
  done
}
checkHash apache-calcite-avatica-X.Y.Z-rcN
{% endhighlight %}

## Get approval for a release via Apache voting process (for Calcite committers)

Release vote on dev list

{% highlight text %}
To: dev@calcite.apache.org
Subject: [VOTE] Release apache-calcite-avatica-X.Y.Z (release candidate N)

Hi all,

I have created a build for Apache Calcite Avatica X.Y.Z, release candidate N.

Thanks to everyone who has contributed to this release.
<Further details about release.> You can read the release notes here:
https://github.com/apache/calcite-avatica/blob/XXXX/site/_docs/history.md

The commit to be voted upon:
http://git-wip-us.apache.org/repos/asf/calcite-avatica/commit/NNNNNN

Its hash is XXXX.

The artifacts to be voted on are located here:
https://dist.apache.org/repos/dist/dev/calcite/apache-calcite-avatica-X.Y.Z-rcN/

The hashes of the artifacts are as follows:
src.tar.gz.sha256 XXXX

A staged Maven repository is available for review at:
https://repository.apache.org/content/repositories/orgapachecalcite-NNNN

Release artifacts are signed with the following key:
https://people.apache.org/keys/committer/jhyde.asc

Please vote on releasing this package as Apache Calcite Avatica X.Y.Z.

The vote is open for the next 72 hours and passes if a majority of
at least three +1 PMC votes are cast.

[ ] +1 Release this package as Apache Calcite X.Y.Z
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

Promote the staged nexus artifacts.

* Go to [https://repository.apache.org/](https://repository.apache.org/) and login
* Under "Build Promotion" click "Staging Repositories"
* In the line with "orgapachecalcite-xxxx", check the box
* Press "Release" button

Copy the Git tag:

{% highlight bash %}
git tag rel/avatica-X.Y.X avatica-X.Y.Z-rcN
git push origin rel/avatica-X.Y.Z
{% endhighlight %}

Check the artifacts into svn.

{% highlight bash %}
# Get the release candidate.
mkdir -p ~/dist/dev
cd ~/dist/dev
svn co https://dist.apache.org/repos/dist/dev/calcite

# Copy the artifacts. Note that the copy does not have '-rcN' suffix.
mkdir -p ~/dist/release
cd ~/dist/release
svn co https://dist.apache.org/repos/dist/release/calcite
cd calcite
cp -rp ../../dev/calcite/apache-calcite-avatica-X.Y.Z-rcN apache-calcite-avatica-X.Y.Z
svn add apache-calcite-avatica-X.Y.Z

# Check in.
svn ci
{% endhighlight %}

Svnpubsub will publish to the
[release repo](https://dist.apache.org/repos/dist/release/calcite) and propagate to the
[mirrors](http://www.apache.org/dyn/closer.cgi/calcite) within 24 hours.

If there are now more than 2 releases, clear out the oldest ones:

{% highlight bash %}
cd ~/dist/release/calcite
svn rm apache-calcite-avatica-X.Y.Z
svn ci
{% endhighlight %}

The old releases will remain available in the
[release archive](http://archive.apache.org/dist/calcite/).

Publish the [Docker images](docker.html).

Add a release note by copying
[site/_posts/2016-11-01-release-1.9.0.md]({{ site.sourceRoot }}/site/_posts/2016-11-01-release-1.9.0.md),
generate the javadoc and copy to `site/target/avatica/apidocs`
and `site/target/avatica/testapidocs`,
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
