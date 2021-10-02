//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to you under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// The script generates a random subset of valid jdk, os, timezone, and other axes.
// You can preview the results by running "node matrix.js"
// See https://github.com/vlsi/github-actions-random-matrix
let {MatrixBuilder} = require('./matrix_builder');
const matrix = new MatrixBuilder();
matrix.addAxis({
  name: 'jdk',
  title: x => x.version + ', ' + x.group,
  values: [
    // Zulu
    {group: 'Zulu', version: '8', distribution: 'zulu', jit: 'hotspot'},
    {group: 'Zulu', version: '11', distribution: 'zulu', jit: 'hotspot'},
    {group: 'Zulu', version: '15', distribution: 'zulu', jit: 'hotspot'},

    // Adopt
    {group: 'Adopt Hotspot', version: '8', distribution: 'adopt-hotspot', jit: 'hotspot'},
    {group: 'Adopt Hotspot', version: '11', distribution: 'adopt-hotspot', jit: 'hotspot'},
    {group: 'Adopt Hotspot', version: '15', distribution: 'adopt-hotspot', jit: 'hotspot'},

    // Adopt OpenJ9
    // TODO: Replace these hard coded versions with something that dynamically picks the most recent
    {group: 'Adopt OpenJ9', version: '8', distribution: 'adopt-openj9', jit: 'openj9'},
    {group: 'Adopt OpenJ9', version: '11', distribution: 'adopt-openj9', jit: 'openj9'},
    {group: 'Adopt OpenJ9', version: '15', distribution: 'adopt-openj9', jit: 'openj9'},

    // Amazon Corretto
    {
      group: 'Corretto',
      version: '8',
      jit: 'hotspot',
      distribution: 'jdkfile',
      url: 'https://corretto.aws/downloads/latest/amazon-corretto-8-x64-linux-jdk.tar.gz'
    },
    {
      group: 'Corretto',
      version: '11',
      jit: 'hotspot',
      distribution: 'jdkfile',
      url: 'https://corretto.aws/downloads/latest/amazon-corretto-11-x64-linux-jdk.tar.gz'
    },
    // Microsoft
    {
      group: 'Microsoft',
      version: '11',
      jit: 'hotspot',
      distribution: 'jdkfile',
      url: 'https://aka.ms/download-jdk/microsoft-jdk-11.0.12.7.1-linux-x64.tar.gz'
    },
    // Liberica
    // Remove when https://github.com/actions/setup-java/pull/225 is merged
    {
      group: 'Liberica',
      version: '8',
      jit: 'hotspot',
      distribution: 'jdkfile',
      url: 'https://download.bell-sw.com/java/8u302+8/bellsoft-jdk8u302+8-linux-amd64.tar.gz'
    },
    {
      group: 'Liberica',
      version: '11',
      jit: 'hotspot',
      distribution: 'jdkfile',
      url: 'https://download.bell-sw.com/java/11.0.12+7/bellsoft-jdk11.0.12+7-linux-amd64.tar.gz'
    },
  ]
});
matrix.addAxis({
  name: 'tz',
  values: [
    'America/New_York',
    'Pacific/Chatham',
    'UTC'
  ]
});
matrix.addAxis({
  name: 'os',
  title: x => x.replace('-latest', ''),
  values: [
    'ubuntu-latest',
    'windows-latest',
    'macos-latest'
  ]
});
matrix.addAxis({
  name: 'hash',
  values: [
    {value: 'regular', title: '', weight: 42},
    {value: 'same', title: 'same hashcode', weight: 1}
  ]
});
matrix.addAxis({
  name: 'locale',
  title: x => x.language + '_' + x.country,
  values: [
    {language: 'de', country: 'DE'},
    {language: 'fr', country: 'FR'},
    {language: 'ru', country: 'RU'},
    {language: 'tr', country: 'TR'},
  ]
});
matrix.addAxis({
  name: 'guava',
  title: x => x === '' ? '' : 'guava-' + x,
  values: [
    '14.0.1',
    '23.0',
    '',
  ]
});

matrix.setNamePattern(['jdk', 'hash', 'os', 'tz', 'locale', 'guava']);

// TODO: figure out how "same hashcode" could be configured in OpenJ9
matrix.exclude({hash: {value: 'same'}, jdk: {jit: 'openj9'}});
matrix.exclude({jdk: {distribution: 'jdkfile'}, os: ['windows-latest', 'macos-latest']});
// Ensure at least one job with "same" hashcode exists
matrix.generateRow({hash: {value: 'same'}});
// Ensure at least one windows and at least one linux job is present (macos is almost the same as linux)
matrix.generateRow({os: 'windows-latest'});
matrix.generateRow({os: 'ubuntu-latest'});
// Ensure there will be at least one job with Java 8
matrix.generateRow({jdk: {version: 8}});
// Ensure there will be at least one job with Java 11
matrix.generateRow({jdk: {version: 11}});
// Ensure there will be at least one job with default Guava version
matrix.generateRow({guava: ''});
const include = matrix.generateRows(process.env.MATRIX_JOBS || 5);
if (include.length === 0) {
  throw new Error('Matrix list is empty');
}
include.sort((a, b) => a.name.localeCompare(b.name, undefined, {numeric: true}));
include.forEach(v => {
  let gradleArgs = [];
  if (v.guava !== '') {
      gradleArgs.push(`-Pguava.version=${v.guava}`);
  }
  v.extraGradleArgs = gradleArgs.join(' ');
  let jvmArgs = [];
  if (v.hash.value === 'same') {
    jvmArgs.push('-XX:+UnlockExperimentalVMOptions', '-XX:hashCode=2');
  }
  // Gradle does not work in tr_TR locale, so pass locale to test only: https://github.com/gradle/gradle/issues/17361
  jvmArgs.push(`-Duser.country=${v.locale.country}`);
  jvmArgs.push(`-Duser.language=${v.locale.language}`);
  if (v.jdk.jit === 'hotspot' && Math.random() > 0.5) {
    // The following options randomize instruction selection in JIT compiler
    // so it might reveal missing synchronization in TestNG code
    v.name += ', stress JIT';
    jvmArgs.push('-XX:+UnlockDiagnosticVMOptions');
    if (v.jdk.version >= 8) {
      // Randomize instruction scheduling in GCM
      // share/opto/c2_globals.hpp
      jvmArgs.push('-XX:+StressGCM');
      // Randomize instruction scheduling in LCM
      // share/opto/c2_globals.hpp
      jvmArgs.push('-XX:+StressLCM');
    }
    if (v.jdk.version >= 16) {
      // Randomize worklist traversal in IGVN
      // share/opto/c2_globals.hpp
      jvmArgs.push('-XX:+StressIGVN');
    }
    if (v.jdk.version >= 17) {
      // Randomize worklist traversal in CCP
      // share/opto/c2_globals.hpp
      jvmArgs.push('-XX:+StressCCP');
    }
  }
  v.testExtraJvmArgs = jvmArgs.join(' ');
  delete v.hash;
});

console.log(include);
console.log('::set-output name=matrix::' + JSON.stringify({include}));
