# Bitmark Mobile Android

The Bitmark app registers legal property rights on the public Bitmark blockchain for your digital assets.

[![Made by](https://img.shields.io/badge/Made%20by-Bitmark%20Inc-lightgrey.svg)](https://bitmark.com)
[![Build Status](https://travis-ci.org/bitmark-inc/bitmark-mobile-android.svg?branch=master)](https://travis-ci.org/bitmark-inc/bitmark-mobile-android)

## Getting Started

#### Prequisites

- Java 8
- Android 6.0 (API 23)

#### Preinstallation

Create `.properties` file for the configuration
- `sentry.properties` : uploading the Proguard mapping file to Sentry
```xml
defaults.project=bitmark-registry
defaults.org=bitmark-inc
auth.token=SentryAuthToken
```
- `key.properties` : API key configuration
```xml
api.key.bitmark=BitmarkSdkApiKey
api.key.intercom=IntercomApiKey
```
- `app/src/main/resources/sentry.properties` : Configuration for Sentry
```xml
dsn=SentryDSN
buffer.dir=sentry-events
buffer.size=100
async=true
async.queuesize=100
```
- `app/fabric.properties` : Configuration for Fabric distribution
```xml
apiSecret=FabricSecretKey
apiKey=FabricApiKey
```

Create `distribution` directory for distribution configuration
- release_note.txt : Release note for distribution
- testers.txt : list email of testers, separate by a comma

Add `release.keystore` and `release.properties` for releasing as production

## Documentation
This project is based on [Bitmark Android Architecture](https://github.com/bitmark-inc/bitmark-android-arch). 
For more detail, please give a read on that repo. 

## Installing

`./gradlew clean fillSecretKey assembleInhouseDebug`

Using `-PsplitApks` to build split APKs

## Deployment
The debug build is distributed via ***Fabric Beta***

`./gradew crashlyticsUploadDistributionInhouseDebug`

## License

```SPDX-License-Identifier: ISC```

Copyright © 2014-2019 Bitmark. All rights reserved.

Use of this source code is governed by an ISC license that can be found in the [LICENSE](LICENSE) file.
