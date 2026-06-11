# Hegemony Assistant Mobile

Standalone Android MVP for the Hegemony assistant.

The app runs the current demonstrative rules slice fully on the device:
state, legal move generation, previews, command application, bot turns, and
save/load all use the local TypeScript engine bundled into the APK. No backend
URL or network server is required.

## Run as mobile web

```bash
cd mobile
npm install
npm run dev
```

Open `http://localhost:5174`.

## Android workflow

```bash
cd mobile
npm run build
npm run android:sync
cd android
./gradlew assembleDebug
```

The debug APK is written to:

```text
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Offline smoke test

```bash
cd mobile
npm run smoke:offline
```

The smoke test covers reset, setup, legal moves, visual worker-assignment
payloads, preview, command submit, bot autoplay back to a human turn, save/load,
and final reset without starting the Java backend.
