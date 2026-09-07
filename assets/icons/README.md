App icons: provide PNGs for Android adaptive and legacy icons

Place your icon source files in this folder. Recommended filenames/sizes:

- `icon-512.png` (512x512) — high-res source
- `ic_launcher_foreground.png` (1024x1024) — foreground for adaptive icon (optional)
- `ic_launcher_background.png` (1024x1024) — background layer for adaptive icon (optional)

How to generate Android mipmap assets:

1. Use Android Asset Studio (https://romannurik.github.io/AndroidAssetStudio/icons-adaptive) to create adaptive icons and download the zip.
2. Unzip and copy the generated `mipmap-*/ic_launcher.png` and `mipmap-*/ic_launcher_round.png` folders into `android/app/src/main/res/` when you open the Android project.
3. Alternatively, provide a 512x512 PNG here and I can generate a simple set of resized icons for you if you want.

Integration steps (after adding icons):

1. Add web assets and Android platform: `npm run build && npx cap add android && npx cap copy android`
2. Open Android project: `npx cap open android`
3. In Android Studio, copy files from this folder into `app/src/main/res/mipmap-*/` and `mipmap-anydpi-v26` for adaptive icons.
4. Rebuild: `./gradlew assembleDebug` or use Android Studio's Build > Make Project.

If you want, upload the PNG(s) here and I will generate/rescale and place them into the Android res folders for a debug APK build.
