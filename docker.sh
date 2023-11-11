#!/bin/bash
#
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
#
set -e

function terminate() {
    printf "\n\nUser terminated build. Exiting...\n"
    exit 1
}

trap terminate SIGINT

PRODUCT=apache-calcite-avatica
RELEASE_REPO=https://dist.apache.org/repos/dist/release/calcite/
DEV_REPO=https://dist.apache.org/repos/dist/dev/calcite/

KEYS=()

GPG_COMMAND="gpg"

install_gnupg2(){
  apt update
  apt install gnupg2 -y
}

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

RC_NUMBER=""
ASF_USERNAME=""
ASF_PASSWORD=""
NAME=""

get_dry_run_build_configuration(){

    while $DRY_RUN_NOT_CONFIRMED; do
        read -p "Enter the release candidate number (example: if you are releasing rc0, enter 0): " RC_NUMBER
        read -p "Enter your ASF username: " ASF_USERNAME
        read -p "Enter your name (this will be used for git commits): " NAME
        echo "Build configured as follows:"
        echo "RC: $RC_NUMBER"
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
        read -p "Enter the release candidate number (example: if you are releasing rc0, enter 0): " RC_NUMBER
        read -p "Enter your name (this will be used for git commits): " NAME
        echo "Build configured as follows:"
        echo "RC: $RC_NUMBER"
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

promote_release(){

    LATEST_TAG=$(git describe --tags `git rev-list --tags --max-count=1`)

    if [[ ! $LATEST_TAG =~ .+-rc[[:digit:]]+$ ]]; then
        echo "The latest tag ($LATEST_TAG) is not a RC release and should not be re-released."
        exit 1
    fi

    TAG_WITHOUT_PRODUCT=$(echo $LATEST_TAG | sed -e 's/avatica-//')
    TAG_WITHOUT_RC=$(echo $TAG_WITHOUT_PRODUCT | sed -e 's/-rc[0-9][0-9]*//')
    RC_NUMBER=$(echo $TAG_WITHOUT_PRODUCT | sed -e 's/^[0-9\.]*-rc//')

    if ! svn ls $DEV_REPO/$PRODUCT-$TAG_WITHOUT_PRODUCT; then
        echo "The release $PRODUCT-$TAG_WITHOUT_PRODUCT was not found in the dev repository. Was it uploaded for voting?"
        exit 1
    fi

    get_asf_credentials
    ./gradlew publishDist -Pasf -PasfSvnUsername=$ASF_USERNAME -PasfSvnPassword=$ASF_PASSWORD -PasfNexusUsername=$ASF_USERNAME -PasfNexusPassword=$ASF_PASSWORD -PasfGitSourceUsername=$ASF_USERNAME -PasfGitSourcePassword=$ASF_PASSWORD -Prc=$RC_NUMBER -Pasf.git.pushRepositoryProvider=GITBOX

    # If there is more than 1 release, delete all of them, except for the newest one
    # To do this, we do the following:
    # 1. Get the list of releases with verbose information from svn
    # 2. Sort by the first field (revision number) in descending order
    # 3. Select apache-calcite-avatica releases
    # 4. Exclude the release we're trying to promote, in case it was from a failed promotion.
    # 5. Trim all whitespace down to 1 empty space.
    # 6. Select field 7, which is each release's folder
    svn checkout $RELEASE_REPO /tmp/release
    cd /tmp/release

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
        install_gnupg2
        mount_gpg_keys
        select_gpg_key
        get_dry_run_build_configuration

        ./gradlew prepareVote -PasfTestSvnUsername=test -PasfTestSvnPassword=test -PasfTestNexusUsername=test -PasfTestNexusPassword=test -PasfTestGitSourceUsername=test -PasfTestGitSourcePassword=test -Prc=$RC_NUMBER -PuseGpgCmd -Psigning.gnupg.keyName=$SELECTED_GPG_KEY
        ;;

    publish-release-for-voting)
        install_gnupg2
        mount_gpg_keys
        select_gpg_key
        get_build_configuration
        get_asf_credentials

        ./gradlew prepareVote -Pasf -PasfCommitterId=$ASF_USERNAME -PasfSvnUsername=$ASF_USERNAME -PasfSvnPassword=$ASF_PASSWORD -PasfNexusUsername=$ASF_USERNAME -PasfNexusPassword=$ASF_PASSWORD -PasfGitSourceUsername=$ASF_USERNAME -PasfGitSourcePassword=$ASF_PASSWORD -Prc=$RC_NUMBER -PuseGpgCmd -Psigning.gnupg.keyName=$SELECTED_GPG_KEY -Pasf.git.pushRepositoryProvider=GITBOX
        ;;

    clean)
        ./gradlew clean
        ;;

    promote-release)
        promote_release
        ;;

    test)
        ./gradlew test
        ;;

    *)
       echo $"Usage: $0 {dry-run|publish-release-for-voting|clean|promote-release|test}"
       ;;

esac
