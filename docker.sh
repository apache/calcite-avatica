#!/bin/bash

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

set -e

GITBOX_URL=https://gitbox.apache.org/repos/asf/calcite-avatica.git
RELEASE_REPO=https://dist.apache.org/repos/dist/release/calcite/
DEV_REPO=https://dist.apache.org/repos/dist/dev/calcite/
PRODUCT=apache-calcite-avatica

function terminate() {
    printf "\n\nUser terminated build. Exiting...\n"
    exit 1
}

trap terminate SIGINT

KEYS=()

GPG_COMMAND="gpg"

get_gpg_keys(){
    GPG_KEYS=$($GPG_COMMAND --list-keys --with-colons --keyid-format LONG)

    KEY_NUM=1

    KEY_DETAILS=""

    while read -r line; do

        IFS=':' read -ra PART <<< "$line"

        if [[ ${PART[0]} == "pub" ]]; then

            if [ -n "$KEY_DETAILS" ]; then
                KEYS[$KEY_NUM]=$KEY_DETAILS
                KEY_DETAILS=""
                ((KEY_NUM++))

            fi

            KEY_DETAILS=${PART[4]}
        fi

        if [[ ${PART[0]} == "uid" ]]; then
            KEY_DETAILS="$KEY_DETAILS - ${PART[9]}"
        fi

    done <<< "$GPG_KEYS"

    if [[ -n "$KEY_DETAILS" ]]; then
        KEYS[$KEY_NUM]=$KEY_DETAILS
    fi
}

mount_gpg_keys(){
    mkdir -p /.gnupg

    if [[ -z "$(ls -A /.gnupg)" ]]; then
        echo "Please mount the contents of your .gnupg folder into /.gnupg. Exiting..."
        exit 1
    fi

    mkdir -p /root/.gnupg

    cp -r /.gnupg/ /root/

    chmod -R 700 /root/.gnupg/

    rm -rf /root/.gnupg/*.lock
}

SELECTED_GPG_KEY=""

select_gpg_key(){

    get_gpg_keys

    export GPG_TTY=/dev/console

    touch /root/.gnupg/gpg-agent.conf
    echo 'default-cache-ttl 10000' >> /root/.gnupg/gpg-agent.conf
    echo 'max-cache-ttl 10000' >> /root/.gnupg/gpg-agent.conf

    echo "Starting GPG agent..."
    gpg-agent --daemon

    while $INVALID_KEY_SELECTED; do

        if [[ "${#KEYS[@]}" -le 0 ]]; then
            echo "You do not have any GPG keys available. Exiting..."
            exit 1
        fi

        echo "You have the following GPG keys:"

        for i in "${!KEYS[@]}"; do
                echo "$i) ${KEYS[$i]}"
        done

        read -p "Select your GPG key for signing: " KEY_INDEX

        SELECTED_GPG_KEY=$(sed 's/ -.*//' <<< ${KEYS[$KEY_INDEX]})

        if [[ -z $SELECTED_GPG_KEY ]]; then
            echo "Selected key is invalid, please try again."
            continue
        fi

        echo "Authenticating your GPG key..."

        echo "test" | $GPG_COMMAND --local-user $SELECTED_GPG_KEY --output /dev/null --sign -

        if [[ $? != 0 ]]; then
            echo "Invalid GPG passphrase or GPG error. Please try again."
            continue
        fi

        echo "You have selected the following GPG key to sign the release:"
        echo "${KEYS[$KEY_INDEX]}"

        INVALID_CONFIRMATION=true

        while $INVALID_CONFIRMATION; do
            read -p "Is this correct? (y/n) " CONFIRM

            if [[ ($CONFIRM == "Y") || ($CONFIRM == "y") ]]; then
                INVALID_KEY_SELECTED=false
                INVALID_CONFIRMATION=false
            elif [[ ($CONFIRM == "N") || ($CONFIRM == "n") ]]; then
                INVALID_CONFIRMATION=false
            fi
        done
    done
}

RELEASE_VERSION=""
RC_NUMBER=""
DEV_VERSION=""
ASF_USERNAME=""
ASF_PASSWORD=""
NAME=""

get_dry_run_build_configuration(){

    while $DRY_RUN_NOT_CONFIRMED; do
        read -p "Enter the version number to be released (example: 1.12.0): " RELEASE_VERSION
        read -p "Enter the release candidate number (example: if you are releasing rc0, enter 0): " RC_NUMBER
        read -p "Enter the development version number (example: if your release version is 1.12.0, enter 1.13.0): " DEV_VERSION
        read -p "Enter your ASF username: " ASF_USERNAME
        read -p "Enter your name (this will be used for git commits): " NAME
        echo "Build configured as follows:"
        echo "Release: $RELEASE_VERSION-rc$RC_NUMBER"
        echo "Next development version: $DEV_VERSION-SNAPSHOT"
        echo "ASF Username: $ASF_USERNAME"
        echo "Name: $NAME"

        INVALID_CONFIRMATION=true

        while $INVALID_CONFIRMATION; do
            read -p "Is this correct? (y/n) " CONFIRM

            if [[ ($CONFIRM == "Y") || ($CONFIRM == "y") ]]; then
                DRY_RUN_NOT_CONFIRMED=false
                INVALID_CONFIRMATION=false
            elif [[ ($CONFIRM == "N") || ($CONFIRM == "n") ]]; then
                INVALID_CONFIRMATION=false
            fi
        done
    done
}

get_build_configuration(){

    while $BUILD_NOT_CONFIRMED; do
        read -p "Enter the version number to be released (example: 1.12.0): " RELEASE_VERSION
        read -p "Enter the release candidate number (example: if you are releasing rc0, enter 0): " RC_NUMBER
        read -p "Enter the development version number (example: if your release version is 1.12.0, enter 1.13.0): " DEV_VERSION
        read -p "Enter your name (this will be used for git commits): " NAME
        echo "Build configured as follows:"
        echo "Release: $RELEASE_VERSION-rc$RC_NUMBER"
        echo "Next development version: $DEV_VERSION-SNAPSHOT"
        echo "Name: $NAME"

        INVALID_CONFIRMATION=true

        while $INVALID_CONFIRMATION; do
            read -p "Is this correct? (y/n) " CONFIRM

            if [[ ($CONFIRM == "Y") || ($CONFIRM == "y") ]]; then
                BUILD_NOT_CONFIRMED=false
                INVALID_CONFIRMATION=false
            elif [[ ($CONFIRM == "N") || ($CONFIRM == "n") ]]; then
                INVALID_CONFIRMATION=false
            fi
        done
    done
}

get_asf_credentials(){
    while $ASF_CREDS_NOT_CONFIRMED; do
        read -p "Enter your ASF username: " ASF_USERNAME
        read -s -p "Enter your ASF password: " ASF_PASSWORD
        printf "\n"
        echo "Your ASF Username is:" $ASF_USERNAME

        INVALID_CONFIRMATION=true

        while $INVALID_CONFIRMATION; do
            read -p "Is this correct? (y/n) " CONFIRM

            if [[ ($CONFIRM == "Y") || ($CONFIRM == "y") ]]; then
                ASF_CREDS_NOT_CONFIRMED=false
                INVALID_CONFIRMATION=false
            elif [[ ($CONFIRM == "N") || ($CONFIRM == "n") ]]; then
                INVALID_CONFIRMATION=false
            fi
        done
    done
}

set_git_credentials(){
    echo https://$ASF_USERNAME:$ASF_PASSWORD@gitbox.apache.org >> /root/.git-credentials
    git config --global credential.helper 'store --file=/root/.git-credentials'

    git config --global user.name "$NAME"
}

set_maven_credentials(){
    mkdir -p /root/.m2
    rm -f /root/.m2/settings.xml
    rm -f /root/.m2/settings-security.xml

    ENCRYPTED_MAVEN_PASSWORD="$(mvn --encrypt-master-password $ASF_PASSWORD)"

    cat <<EOF >> /root/.m2/settings-security.xml
<settingsSecurity>
  <master>$ENCRYPTED_MAVEN_PASSWORD</master>
</settingsSecurity>
EOF

    ENCRYPTED_ASF_PASSWORD="$(mvn --encrypt-password $ASF_PASSWORD)"

    cat <<EOF >> /root/.m2/settings.xml
<settings>
  <servers>
    <server>
      <id>apache.snapshots.https</id>
      <username>${ASF_USERNAME}</username>
      <password>${ENCRYPTED_ASF_PASSWORD}</password>
    </server>
    <server>
      <id>apache.releases.https</id>
      <username>${ASF_USERNAME}</username>
      <password>${ENCRYPTED_ASF_PASSWORD}</password>
    </server>
  </servers>
</settings>
EOF
}

publish_release_for_voting(){

    LATEST_TAG=$(git describe --tags `git rev-list --tags --max-count=1`)

    if [[ ! $LATEST_TAG =~ .+-rc[[:digit:]]+$ ]]; then
        echo "The latest tag ($LATEST_TAG) is not a RC release and should not be published for voting."
        exit 1
    fi

    TAG_WITHOUT_PRODUCT=$(echo $LATEST_TAG | sed -e 's/avatica-//')
    TAG_WITHOUT_RC=$(echo $TAG_WITHOUT_PRODUCT | sed -e 's/-rc[0-9][0-9]*//')
    SOURCE_RELEASE=$PRODUCT-$TAG_WITHOUT_RC-src.tar.gz
    GPG_SIGNATURE=$PRODUCT-$TAG_WITHOUT_RC-src.tar.gz.asc
    SHA512=$PRODUCT-$TAG_WITHOUT_RC-src.tar.gz.sha512
    GPG_SHA512=$PRODUCT-$TAG_WITHOUT_RC-src.tar.gz.asc.sha512
    COMMIT=$(git rev-list -n 1 $LATEST_TAG)

    # Check to see a release is built
    MISSING_FILES=false

    if [ ! -f "target/$SOURCE_RELEASE" ]; then
        echo "Did not find source release ($SOURCE_RELEASE) in target folder."
        MISSING_FILES=true
    fi

    if [ ! -f "target/$GPG_SIGNATURE" ]; then
        echo "Did not find GPG signature ($GPG_SIGNATURE) in target folder."
        MISSING_FILES=true
    fi

    if [ ! -f "target/$SHA512" ]; then
        echo "Did not find SHA512 ($SHA512) in target folder."
        MISSING_FILES=true
    fi

    if [ ! -f "target/$GPG_SHA512" ]; then
        echo "Did not find GPG SHA512 ($GPG_SHA512) in target folder."
        MISSING_FILES=true
    fi

    if $MISSING_FILES == true; then
        exit 1
    fi

    while $NOT_CONFIRMED; do
        echo "Publish configured as follows:"
        echo "Release: $LATEST_TAG"

        INVALID_CONFIRMATION=true

        while $INVALID_CONFIRMATION; do
            read -p "Is this correct? (y/n) " CONFIRM

            if [[ ($CONFIRM == "Y") || ($CONFIRM == "y") ]]; then
                NOT_CONFIRMED=false
                INVALID_CONFIRMATION=false
            elif [[ ($CONFIRM == "N") || ($CONFIRM == "n") ]]; then
                INVALID_CONFIRMATION=false
            fi
        done
    done

    HASH=$(cat "target/$SHA512" | tr -d '\n')

    get_asf_credentials

    svn checkout $DEV_REPO /tmp/calcite --depth empty
    mkdir -p /tmp/calcite/$PRODUCT-$TAG_WITHOUT_PRODUCT
    cp -R target/$PRODUCT-* /tmp/calcite/$PRODUCT-$TAG_WITHOUT_PRODUCT

    cd /tmp/calcite
    svn add $PRODUCT-$TAG_WITHOUT_PRODUCT
    chmod -x $PRODUCT-$TAG_WITHOUT_PRODUCT/*

    svn commit -m "$PRODUCT-$TAG_WITHOUT_PRODUCT" --force-log --username $ASF_USERNAME --password $ASF_PASSWORD

    [[ $LATEST_TAG =~ -rc([[:digit:]]+)$ ]]
    RC_NUMBER=${BASH_REMATCH[1]}

    [[ $TAG_WITHOUT_RC =~ ([[:digit:]]+\.[[:digit:]]+)\.[[:digit:]]+$ ]]
    BRANCH_VERSION=${BASH_REMATCH[1]}

    read -p "Please enter your first name for the voting email: " FIRST_NAME
    read -p "Enter the ID at the end of the staged repository (for orgapachecalcite-1000, enter 1000): " STAGED_REPO_ID

    echo "The release $PRODUCT-$TAG_WITHOUT_PRODUCT has been uploaded to the development repository."
    printf "\n"
    printf "\n"
    echo "Email the following message to dev@calcite.apache.org. Please check the message before sending."
    printf "\n"
    echo "To: dev@calcite.apache.org"
    echo "Subject: [VOTE] Release $PRODUCT-$TAG_WITHOUT_RC (release candidate $RC_NUMBER)"
    echo "Message:
Hi all,

I have created a build for Apache Calcite Avatica $TAG_WITHOUT_RC, release candidate $RC_NUMBER.

Thanks to everyone who has contributed to this release.

You can read the release notes here:
https://github.com/apache/calcite-avatica/blob/branch-avatica-$BRANCH_VERSION/site/_docs/history.md

The commit to be voted upon:
https://gitbox.apache.org/repos/asf?p=calcite-avatica.git;a=commit;h=$COMMIT

Its hash is $COMMIT

The artifacts to be voted on are located here:
$DEV_REPO$PRODUCT-$TAG_WITHOUT_PRODUCT/

The hashes of the artifacts are as follows:
src.tar.gz.sha512 $HASH

A staged Maven repository is available for review at:
https://repository.apache.org/content/repositories/orgapachecalcite-$STAGED_REPO_ID

Release artifacts are signed with the following key:
https://people.apache.org/keys/committer/$ASF_USERNAME.asc

If you do not have a Java environment available, you can run the tests using docker. To do so, install docker and docker-compose, then run \"docker-compose run test\" from the root of the directory.

Please vote on releasing this package as Apache Calcite Avatica $TAG_WITHOUT_RC.

The vote is open for the next 72 hours and passes if a majority of at least three +1 PMC votes are cast.

[ ] +1 Release this package as Apache Calcite 1.14.0
[ ]  0 I don't feel strongly about it, but I'm okay with the release
[ ] -1 Do not release this package because...


Here is my vote:

+1 (binding)

$NAME
"
}

promote_release(){

    LATEST_TAG=$(git describe --tags `git rev-list --tags --max-count=1`)

    if [[ ! $LATEST_TAG =~ .+-rc[[:digit:]]+$ ]]; then
        echo "The latest tag ($LATEST_TAG) is not a RC release and should not be re-released."
        exit 1
    fi

    TAG_WITHOUT_PRODUCT=$(echo $LATEST_TAG | sed -e 's/avatica-//')
    TAG_WITHOUT_RC=$(echo $TAG_WITHOUT_PRODUCT | sed -e 's/-rc[0-9][0-9]*//')

    if ! svn ls $DEV_REPO/$PRODUCT-$TAG_WITHOUT_PRODUCT; then
        echo "The release $PRODUCT-$TAG_WITHOUT_PRODUCT was not found in the dev repository. Was it uploaded for voting?"
        exit 1
    fi

    get_asf_credentials

    set_git_credentials

    git tag rel/avatica-$TAG_WITHOUT_RC $LATEST_TAG

    git push $GITBOX_URL rel/avatica-$TAG_WITHOUT_RC

    svn checkout $RELEASE_REPO /tmp/release
    rm -rf /tmp/release/$PRODUCT-$TAG_WITHOUT_RC
    mkdir -p /tmp/release/$PRODUCT-$TAG_WITHOUT_RC

    svn checkout $DEV_REPO/$PRODUCT-$TAG_WITHOUT_PRODUCT /tmp/rc
    cp -rp /tmp/rc/* /tmp/release/$PRODUCT-$TAG_WITHOUT_RC

    cd /tmp/release

    svn add $PRODUCT-$TAG_WITHOUT_RC

    # If there is more than 1 release, delete all of them, except for the newest one
    # To do this, we do the following:
    # 1. Get the list of releases with verbose information from svn
    # 2. Sort by the first field (revision number) in descending order
    # 3. Select apache-calcite-avatica releases
    # 4. Exclude the release we're trying to promote, in case it was from a failed promotion.
    # 5. Trim all whitespace down to 1 empty space.
    # 6. Select field 7, which is each release's folder
    CURRENT_RELEASES=$(svn ls -v $RELEASE_REPO | sort -k1 -r | grep $PRODUCT-[[:digit:]] | grep -v $PRODUCT-$TAG_WITHOUT_RC | tr -s ' ' | cut -d ' ' -f 7)

    RELEASE_COUNT=0
    while read -r RELEASE; do
        if [[ $RELEASE_COUNT -ne 0 ]]; then
            svn rm $RELEASE
            echo "Removing release $RELEASE"
        fi

        RELEASE_COUNT=$((RELEASE_COUNT+1))
    done <<< "$CURRENT_RELEASES"

    svn commit -m "Release $PRODUCT-$TAG_WITHOUT_RC" --force-log --username $ASF_USERNAME --password $ASF_PASSWORD

    echo "Release $PRODUCT-$LATEST_TAG successfully promoted to $PRODUCT-$TAG_WITHOUT_RC"
}

case $1 in
    dry-run)
        mount_gpg_keys
        select_gpg_key
        get_dry_run_build_configuration

        mvn -Dmaven.artifact.threads=20 -DdryRun=true -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEV_VERSION-SNAPSHOT -Dtag="avatica-$RELEASE_VERSION-rc$RC_NUMBER" -Papache-release -Duser.name=$ASF_USERNAME release:prepare -Darguments=-Dgpg.keyname=$SELECTED_GPG_KEY
        ;;

    release)
        mount_gpg_keys
        select_gpg_key
        get_build_configuration
        get_asf_credentials
        set_git_credentials
        set_maven_credentials

        mvn -Dmaven.artifact.threads=20 -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEV_VERSION-SNAPSHOT -Dtag="avatica-$RELEASE_VERSION-rc$RC_NUMBER" -Papache-release -Duser.name=$ASF_USERNAME release:prepare -Darguments=-Dgpg.keyname=$SELECTED_GPG_KEY
        mvn -Dmaven.artifact.threads=20 -Papache-release -Duser.name=$ASF_USERNAME release:perform -Darguments="-DskipTests -Dgpg.keyname=$SELECTED_GPG_KEY"
        ;;

    clean)
        mvn release:clean
        ;;

    publish-release-for-voting)
        publish_release_for_voting
        ;;

    promote-release)
        promote_release
        ;;

    test)
        mvn clean verify -Dcheckstyle.skip
        ;;

    *)
       echo $"Usage: $0 {dry-run|release|clean|publish-release-for-voting|promote-release|test}"
       ;;

esac
