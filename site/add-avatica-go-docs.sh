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
git clone https://github.com/apache/calcite-avatica-go /tmp/calcite-avatica-go

CURRENT_DIR=$PWD

 # Reset the client reference to the last release (so that unreleased changes are not published)
cd /tmp/calcite-avatica-go

LATEST_TAG=$(git describe --exclude "*-rc*" --tags --abbrev=0)
git checkout tags/$LATEST_TAG site/_docs/go_client_reference.md

cd $CURRENT_DIR

cp /tmp/calcite-avatica-go/site/_docs/* _docs/
cp /tmp/calcite-avatica-go/site/_posts/* _posts/
cp /tmp/calcite-avatica-go/site/develop/* develop/
