# Automattic-Tracks-Android
Client library for tracking user events for later analysis

## Introduction

Tracks for Android is a client library used to help track events inside of
an application. This project solely is responsible for collecting the events,
storing them locally, and on a schedule send them out to the Automattic
servers. Realistically this library is only useful for Automattic-based
projects but the idea is to share what we've made.

## Usage

```groovy
repositories {
    maven {
        url 'https://a8c-libs.s3.amazonaws.com/android'
        content {
            includeGroup 'com.automattic'
            includeGroup 'com.automattic.tracks'
        }
    }
}

dependencies {
    // For 'tracks' module
    implementation 'com.automattic:Automattic-Tracks-Android:{version}'

    // For 'experimentation' module
    implementation 'com.automattic.tracks:experimentation:{version}'
}
```

## Publishing a new version

In the following cases, the CI will publish a new version with the following format to our S3 Maven repo:

* For each commit in an open PR: `<PR-number>-<commit full SHA1>`
* Each time a PR is merged to `trunk`: `trunk-<commit full SHA1>`
* Each time a new tag is created: `{tag-name}`

## License

Automattic-Tracks-Android is available under the GPL v2 license. See
the [LICENSE](LICENSE) file for more info.
