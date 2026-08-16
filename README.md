# Android App — Setup Guide

This connects to the backend you already built and deployed (Phase 1).
Package name: `com.tijaabo.app` — rename it (Android Studio: right-click
package → Refactor → Rename) if you want something else before your real
release.

## What's built (Phase 2, part 1)

- **Splash screen** — resolves your backend URL, checks for forced updates, pings analytics
- **Bottom navigation** — Home, Video, Audio, News, Contact (labels pull live from your Settings tab)
- **Folder browsing** — drills into your category tree (Taariikhda Soomaliya → Banadir → Daynille, etc.) exactly as designed
- **Video/audio player** — HLS playback via ExoPlayer, autoplay-next for videos, no comments/likes, just views
- **News feed** — taps into attached video/audio, or shows full text
- **Contact Us** — submits straight into your admin Inbox
- **Security** — every API call sends your `X-App-Key` header automatically; minimal permissions (internet only)

## What's simplified for now (be aware)

- **Home tab currently mirrors the News feed** — since news posts can already carry attached video/audio/photos, this covers "mixed feed" for a first version. A true combined feed (standalone folder videos mixed in, not just ones linked from a news post) is a natural next addition once you're ready.
- **Offline caching (last-viewed content) is not yet implemented** — the app always loads fresh from your backend. We discussed this earlier (cache text/photos, not full videos); it's a good Part 2 addition.
- **No compiler available in my environment** — I wrote and carefully checked every file (XML validated, Kotlin brace-balance checked), but I could not actually build/run this app myself. **You'll get the first real compile the moment you open this in Android Studio or run it through GitHub Actions** — treat the first build as a real test, not a formality.

## Setup steps

### 1. Generate your signing keystore (once, ever — keep it safe forever)
```
keytool -genkeypair -v -keystore release.keystore -alias tijaabo -keyalg RSA -keysize 2048 -validity 10000
```
Keep `release.keystore` somewhere safe outside this repo. Losing it means you can never update this app under the same identity again.

### 2. Add GitHub Actions Secrets (repo Settings → Secrets and variables → Actions)
```
KEYSTORE_BASE64      = (base64 -i release.keystore | output, one line)
KEYSTORE_PASSWORD    = (the password you set above)
KEY_ALIAS             = tijaabo
KEY_PASSWORD          = (same or different password)
APP_ACCESS_KEY        = (the SAME value as APP_ACCESS_KEY in your Railway backend)
FALLBACK_API_URL      = https://resourceful-peace-production.up.railway.app/
```

### 3. Generate the Gradle wrapper (one-time, needs Android Studio or local Gradle)
This project needs `gradlew` + `gradle-wrapper.jar`, which are binary files I couldn't generate in my environment. Easiest fix:
- Open this folder in Android Studio — it will offer to generate the wrapper automatically, **or**
- Run locally: `gradle wrapper --gradle-version 8.7`

### 4. Push to GitHub
The `build-apk.yml` workflow runs automatically on push to `main`, or manually via the Actions tab ("Run workflow"). Download the built APK from the workflow's "Artifacts" section when it finishes.

### 5. Install and test
Sideload the APK onto your Android phone (Settings may ask to allow installs from unknown sources — expected, since you're not using Play Store). Test each tab: Video folders should show your existing "somalia" folder and "tijaabo" video; Contact Us message should appear in your admin Inbox.

## What's next (Part 2 of Phase 2)
- Offline caching for text/photos on reopen
- A true mixed Home feed
- App icon polish (current one is a simple placeholder)
- Testing on a real device and fixing whatever the first real build surfaces
