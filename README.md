# Automattic-Tracks-Android [![Automattic](https://circleci.com/gh/Automattic/Automattic-Tracks-Android.svg?style=shield)](https://app.circleci.com/pipelines/github/Automattic/Automattic-Tracks-Android)
Client library for tracking user events for later analysis

## Introduction

Tracks for Android is a client library used to help track events inside of
an application. This project solely is responsible for collecting the events,
storing them locally, and on a schedule send them out to the Automattic
servers. Realistically this library is only useful for Automattic-based
projects but the idea is to share what we've made.

## Build

* Build:

```sh
$ ./gradlew assemble
```

* Publish to bintray:

```sh
$ ./gradlew assemble publishToMavenLocal bintrayUpload -PbintrayUser=XXX -PbintrayKey=XXX -PdryRun=false
```

Note: running the `publishToMavenLocal` task is very important, it will
create the .po file needed for any maven repository.

## Usage

In your gradle config:

```gradle
dependencies {
    // Replace LATEST_VERSION by the version you need.
    compile 'com.automattic:tracks:LATEST_VERSION'
}
```

## License

Automattic-Tracks-Android is available under the GPL v2 license. See
the [LICENSE](LICENSE) file for more info.
