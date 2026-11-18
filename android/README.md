# All Exam India Android App

The Android app has a native home screen, native bottom navigation, native profile/settings, and a Light/Dark theme.

The Mock Test, PYQ Practice, and Current Affairs sections load their content from the live All Exam India website:

https://prajwaltarare7-oss.github.io/all-exam-india/

This keeps the website content as the source of truth. Updates published on the website appear in the app when the relevant section is refreshed. The website source and design are not modified by Android-only changes.

## Build

GitHub Actions builds a debug APK when files under `android/` change or when the workflow is manually started. The APK is uploaded as the artifact **all-exam-india-apk**.

The build uses Android SDK 35, Gradle 8.9, and Java 17. Internet access is required to load live exam content; WebView's normal cache may make previously visited resources available temporarily while offline.
