# WorkRun Android 2.2 — release preparation

Updated from the user-supplied WorkRun-Android-Source(1).zip (2.1).
Package com.workrun.app; min SDK 24; compile/target SDK 36; versionCode 4.
Check Play Console's highest used versionCode before uploading; increase 4 if necessary.
Existing green/gold launcher resources are preserved.

## Changes
- Standard Android Gradle project; Android Gradle Plugin 8.13.2 / Gradle 8.13 / JDK 17.
- AAB and APK build tasks replace the APK-only script.
- Android 15/16 screen-inset handling and Android 13+ predictive-back callback.
- User agent aligned to WorkRunAndroid/2.2.
- Signing key/password removed from the prepared source; ignored by git.
- Signed-release task fails when signing credentials are missing.
- Optional GitHub validation workflow builds an UNSIGNED AAB and DEBUG APK; it does not upload to Play or publish a release.

## Build on Windows
1. Install JDK 17, Android SDK platform 36 and build-tools 35.0.0 (Android Studio SDK Manager).
2. Install Gradle 8.13; add its bin directory to PATH. This package does not include a Gradle wrapper JAR.
3. Set ANDROID_HOME to the Android SDK path, or create local.properties with sdk.dir pointing there.
4. For a diagnostic build, run `gradle lintRelease assembleDebug bundleRelease` from this folder.
   The debug APK is for testing only; the unsigned bundle cannot be submitted as the signed Play upload.
5. After resolving the exposed-key issue, supply these environment variables without committing them:
   WORKRUN_KEYSTORE (absolute keystore path), WORKRUN_STORE_PASSWORD,
   WORKRUN_KEY_ALIAS, WORKRUN_KEY_PASSWORD.
6. Run BUILD-RELEASE.bat, or `gradle :app:signedRelease`.
7. Signed outputs: app/build/outputs/bundle/release/app-release.aab and
   app/build/outputs/apk/release/app-release.apk.
   Verify certificates with apksigner/jarsigner and validate the AAB using bundletool;
   install bundle-generated APKs and perform device tests before any approved upload.

## GitHub use
Put the contents of this project at the repository root, replacing the old source layout.
Delete obsolete root AndroidManifest.xml, src/, res/, build-apk.sh and signing/ from the working tree.
Keep a private backup of the existing signing identity before recovery; never put it in a public repository again.
The included .github/workflows/android.yml can be run with Actions > Android validation > Run workflow.
No workflow has been run by this preparation. Existing GitHub files were not modified.

## Exposed key recovery — still required
The original public repository contains a keystore and password in git history. Removing the files or
adding .gitignore does not revoke that key. Do not reuse it for a fresh app without assessing exposure.
If no release has been distributed and no key identity is established, create a new protected release/upload key.
If enrolled in Play App Signing, confirm whether this is the upload key; request upload-key reset if applicable.
If it is an established app-signing key, use the supported Play signing-key recovery/upgrade process;
do not replace it blindly because existing installations may lose update compatibility.
Repository history cleanup and account actions must be performed separately; no such actions were taken.

## Validation and remaining release gates
Performed: source migration checks, manifest/resource XML parsing, secret-file exclusion, expected
package/version/task checks and ZIP CRC check. No Android SDK, Gradle executable, signing verifier,
or emulator is installed in the execution environment, so compilation, lint, signing verification,
AAB validation and device testing have NOT passed or been run. No new APK/AAB is included.
JDK 17 is available. A network probe reached the Gradle distribution redirect, but does not establish
that the full Android toolchain or Maven dependencies are downloadable in this restricted environment.

Test Android 7 and current Android versions, rotation, keyboard, back gestures, WebView cookies/login,
network/TLS failure, Razorpay/UPI launch/return/cancellation, membership and coupon flows, and maintenance status.
Review privacy policy, account deletion, Data safety, reviewer account and required closed testing.
Paid membership with a property prize draw remains subject to the policy eligibility review described
in WorkRun-Play-Preparation.zip. Source changes do not resolve promotion eligibility or legal requirements.
No Play Console operation is authorized by this package.

Toolchain reference: https://developer.android.com/build/releases/agp-8-13-0-release-notes
