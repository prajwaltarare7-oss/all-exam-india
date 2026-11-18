# All Exam India Android APK

This Android app is a lightweight WebView wrapper for the live All Exam India website:

https://prajwaltarare7-oss.github.io/all-exam-india/

## Build

The GitHub Actions workflow builds a debug APK automatically when Android project files change or when manually started.

The APK is uploaded as the workflow artifact named **all-exam-india-apk**.

The APK build uses the Android SDK and Java 17 and Android SDK toolchains provided by GitHub Actions, and runs automatically whenever files under `android/` change.\n\nThis first APK requires an internet connection because it loads the live website. It does not change the website source or its design.
