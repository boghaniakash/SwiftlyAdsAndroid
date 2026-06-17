# Publishing SwiftlyAds to Maven Central

The `:swiftlyads` module is configured to publish to **Maven Central** via the
[Sonatype Central Portal](https://central.sonatype.com) using the
[vanniktech `maven-publish`](https://github.com/vanniktech/gradle-maven-publish-plugin) plugin.

- **Coordinates:** `io.github.akashboghani:swiftlyads:0.1.0`
- **Group/version/POM** are defined in `swiftlyads/build.gradle.kts` (`mavenPublishing { … }`).
  Bump the `version = "…"` there for each release. Use a `-SNAPSHOT` suffix for snapshot builds.

> Credentials and signing keys are **secrets** — never commit them. Put them in
> `~/.gradle/gradle.properties` (per-user, outside the repo) or pass them as environment variables.

---

## One-time setup

### 1. Sonatype Central Portal account + namespace

1. Create an account at https://central.sonatype.com.
2. Register the namespace **`io.github.akashboghani`**. For an `io.github.*` namespace the portal
   verifies ownership by asking you to create a GitHub repo with a generated name — follow the
   on-screen instructions. (This requires the GitHub account `akashboghani`.)
3. Generate a **user token**: Central Portal → *Account* → *Generate User Token*. This gives you a
   token *username* and *password* (not your login credentials).

### 2. GPG signing key

Maven Central requires every artifact to be GPG-signed.

```bash
# Generate a key (use a real name/email; remember the passphrase)
gpg --gen-key

# Find the key id (the long hex string)
gpg --list-secret-keys --keyid-format=long

# Publish the PUBLIC key to a keyserver so Central can verify the signatures
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# Export the SECRET key in ASCII-armored form (used as the in-memory signing key)
gpg --armor --export-secret-keys <KEY_ID>
```

### 3. Provide credentials to Gradle

Add to `~/.gradle/gradle.properties`:

```properties
# Sonatype Central Portal user token
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>

# In-memory GPG signing key (the full ASCII-armored secret key, newlines escaped as \n)
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingInMemoryKeyPassword=<your-gpg-passphrase>
```

…or as environment variables (handy for CI):

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=<token-username>
export ORG_GRADLE_PROJECT_mavenCentralPassword=<token-password>
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys <KEY_ID>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=<your-gpg-passphrase>
```

---

## Releasing

```bash
# 1. (optional) sanity-check the artifact locally first — no credentials/signing key needed
./gradlew :swiftlyads:publishToMavenLocal
#   produces ~/.m2/repository/io/github/akashboghani/swiftlyads/0.1.0/

# 2. Build, sign, and upload a staged deployment to the Central Portal
./gradlew :swiftlyads:publishToMavenCentral

# 3. Go to https://central.sonatype.com → Deployments, review the validation, and click "Publish".
```

`automaticRelease` is set to `false` in `mavenPublishing { … }`, so step 3 is a manual confirmation.
Once you trust the pipeline, set it to `true` and `publishToMavenCentral` will release automatically.

After the deployment is published it typically takes ~15–30 minutes to appear on Maven Central and
a bit longer to be searchable.

---

## Consuming the published library

```kotlin
// your settings.gradle.kts / build.gradle.kts repositories already include mavenCentral()
dependencies {
    implementation("io.github.akashboghani:swiftlyads:0.1.0")
}
```
