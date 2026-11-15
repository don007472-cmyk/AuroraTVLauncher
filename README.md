# Aurora TV Launcher

A lightweight, Apple TV-inspired Android TV home-screen prototype for Android 8.0+ (API 26+), configured for your Android 11 / API 30 Mi TV.

## Included
- 1080p-friendly dark home screen with featured welcome panel and live clock
- App discovery through Android TV Leanback and standard launcher activities
- D-pad focus styling and remote-select app launching
- Long-press an app to add/remove it from **Your Apps**
- Long-press a favorite to move it left or right; order is saved locally
- No network permission, analytics, ads, background service, or app-removal behavior
- Does not replace or disable the existing Google TV launcher automatically

## Build APK on Windows 10

1. Install Android Studio (or Android command-line tools) and JDK 17.
2. Open this folder in Android Studio and allow Gradle sync.
3. Ensure Android SDK Platform 35 and Build Tools are installed (the app targets API 30 and compiles against 35).
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Or from a terminal in this folder, after Gradle is installed:

```bat
gradle assembleDebug
```

## First launch / setting as Home

Install only when you explicitly choose to. When Android asks which Home app to use, choose Aurora TV temporarily or set it as default if you want. The existing Google TV launcher remains installed. You can switch Home apps through Android Settings > Apps > Default apps > Home app (menu wording can vary by TV firmware).

## Notes

- This is a first functional prototype, not a pixel-perfect reproduction of Apple's proprietary interface or artwork.
- Android package visibility is limited to apps exposing a Leanback or standard launcher activity. Some TV apps may not appear if they expose neither.
- The featured panel is currently a static welcome panel; no streaming-service content is scraped or embedded.
- App favorites/order are stored in the launcher's private preferences. Clearing launcher data resets them.

## Build in GitHub Actions (no Android Studio needed)

1. Create a **private** repository on GitHub.
2. Upload the contents of this project folder to the repository (the `.github/workflows/build-apk.yml` file must be included).
3. Open the repository's **Actions** tab and select **Build Aurora TV APK**.
4. Choose **Run workflow** and confirm.
5. When the run finishes successfully, open that run and download the **AuroraTV-debug-APK** artifact.
6. Extract the downloaded artifact ZIP; it contains `app-debug.apk`.

The workflow builds a debug APK on GitHub's hosted runner. It does not access or modify your TV. Do not publish signing keys or passwords in the repository.
