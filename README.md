# Automattic-Tracks-Android
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

* Publish to Jitpack (for `Tracks` >`1.2.1`)

There's no need to do anything. Jitpack will handle build of requested verison itself.

## Usage

### Jitpack

In your gradle config:

1. Make sure there's `maven { url 'https://jitpack.io' }` defined in project's repositories

2. Add `Tracks` to dependencies:

```gradle
dependencies {
    // Replace LATEST_VERSION by the version you need.
    implementation 'com.github.Automattic:Automattic-Tracks-Android:LATEST_VERSION'
}
```

### Maven Local

In order to use this project locally
1. Define `LOCAL-SNAPSHOT` version in `Track`'s `build.gradle`

```gradle
publications {
        maven(MavenPublication) {
            (...)
            version 'LOCAL-SNAPSHOT'
        }
    }
```

2. Run `./gradlew publishToMavenLocal` and change version to `LOCAL-SNAPSHOT` on client:

```gradle
dependencies {
    implementation 'com.github.Automattic:Automattic-Tracks-Android:LOCAL-SNAPSHOT'
}
```

## License

Automattic-Tracks-Android is available under the GPL v2 license. See
the [LICENSE](LICENSE) file for more info.
