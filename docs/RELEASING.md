# Releasing the Android app

This repository is intentionally portable: no production hostname or signing key is committed.

## Build configuration

Set the API origin at build time:

```bash
./gradlew :app:assembleRelease -PGUODELIST_SERVER=https://api.your-server.example
```

Or put this in `~/.gradle/gradle.properties`:

```properties
GUODELIST_SERVER=https://api.your-server.example
```

The default is the reserved placeholder `https://api.example.com`. The same value is used for
sync and for the in-app update check. Users can still override the server in
Settings → Account & sync → Server settings.

## Signing

Create a git-ignored `keystore.properties` in the repository root:

```properties
storeFile=/absolute/path/to/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Do not commit the keystore, `keystore.properties`, or passwords. Back up the keystore: every future
in-app update must use the same signing identity.

```bash
./gradlew :app:assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

## Tests and coverage

```bash
./gradlew testDebugUnitTest
./gradlew jacocoTestCoverageVerification
./gradlew jacocoTestReport
```

## Publishing

Upload the APK to your own release host and update the release manifest used by the server. If you
publish on GitHub Releases, attach the APK there as well; the in-app updater is independent of the
source host.

Before publishing, check:

- [ ] No keystore/password/token is staged (`git status`, `git grep`).
- [ ] No production hostname is hardcoded.
- [ ] Version code and version name are bumped.
- [ ] The APK installs and updates over the previous signing identity.
