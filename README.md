# Automattic-Tracks-Android [![CircleCI](https://circleci.com/gh/Automattic/Automattic-Tracks-Android.svg?style=shield)](https://app.circleci.com/pipelines/github/Automattic/Automattic-Tracks-Android) [![Releases](https://img.shields.io/github/v/release/Automattic/Automattic-Tracks-Android)](https://github.com/Automattic/Automattic-Tracks-Android/releases)
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

## Usage

Follow instructions under `How to` section on [jitpack.com/Automattic/Automattic-Tracks-Android](https://jitpack.io/#Automattic/Automattic-Tracks-Android/)

## License

Automattic-Tracks-Android is available under the GPL v2 license. See
the [LICENSE](LICENSE) file for more info.
