# Quest Mod File Storage

This repository contains a minimal Android app (Kotlin + Jetpack Compose) that provides simple file storage and management on shared external storage for Quest 3.

Features included in this initial build:
- Browse files and folders under a default root: /sdcard/Mods
- Create / delete / rename (simple) operations
- Copy (via utility), zip/unzip utilities
- MD5 checksum utility
- Simple HTTP PUT upload server (port 8080) for pushing files from PC: use curl with PUT
- Requests "All files access" (MANAGE_EXTERNAL_STORAGE) guidance on first run

Warning:
- This app is intended for sideloading to Quest devices for personal use. Granting All files access allows full access to shared storage — be careful.
- Installing or modifying game files may violate terms of service or break games.

Building and sideloading (quick):
1. Enable Developer Mode on your Quest 3 and connect it to your PC with adb.
2. Build the APK in Android Studio (or use Gradle):
   - Open this project in Android Studio and build a debug APK.
3. Sideload with adb:
   - adb install -r app/build/outputs/apk/debug/app-debug.apk

Using the HTTP upload server:
- Start the server in the app (Start Upload Server button).
- From your PC, upload a file using curl (replace HEADSET_IP):
  curl -X PUT --data-binary @localfile "http://HEADSET_IP:8080/upload?name=mods/myfile.zip"

Next steps:
- I will build a debug-signed APK, push the source to this repo (done), and publish a release with the APK attached.
- If you want the APK signed with your keystore, provide it and I will re-sign the APK and update the release.

