# FitLog: workout, calorie and body tracker (Android)

Native Android app (Kotlin + Jetpack Compose + Room). Works fully offline and keeps all data on your phone.

## Features

**Today dashboard**
- Calorie ring (left or over), with protein, carbs and fat progress bars
- Water tracker (+glass / +500 ml), step entry, quick weigh-in
- Day streak (days with food or a workout logged) and workouts in the last 7 days

**Food and calories**
- Breakfast, lunch, dinner and snacks, with day-by-day navigation
- 150+ built-in foods including Pakistani staples: roti, paratha, biryani, karahi, nihari, daal, chai, lassi, samosa and more
- Search, category filters, recent foods, and your own **custom foods**
- Serving size in ½ steps, **Quick add** (calories/macros only), and copy a meal from the previous day
- Targets are calculated automatically (Mifflin-St Jeor BMR × activity level ± goal). You can also set a custom calorie target.

**Workouts**
- Start an empty workout or start from a **routine** (starter Push/Pull/Legs and Full-body routines included)
- Nearly 80 exercises grouped by muscle, plus custom exercises
- For each set you enter weight and reps. The "Previous" column shows last time's numbers, and new sets are pre-filled from them.
- **Rest timer** starts automatically when you tick a set. You can adjust it ±15 s, and the phone vibrates when rest is over.
- An in-progress workout is kept even if Android closes the app
- History with duration, sets and volume; repeat a past workout

**Progress**
- Body weight chart, 30-day change and BMI
- Measurements: waist, chest, arms, hips, thighs, neck, body fat %
- Calories and protein over the last 7 days vs target; 7- and 30-day averages
- Strength: estimated 1RM trend per exercise, personal records, and workouts per week

**Settings**
- kg / lb, water goal, step goal, rest-timer length
- **Backup export/import** (JSON file, e.g. to Google Drive) and an erase-all option

---

## Getting the APK onto your phone

### Option A: GitHub builds it for you (no Android Studio needed)
1. Create a free GitHub account and a new **empty** repository (private is fine).
2. Upload this whole folder to it. The easiest ways are the web page ("Add file → Upload files": drag in everything, **including the hidden `.github` folder**) or:
   ```bash
   cd FitLog
   git init && git add . && git commit -m "FitLog"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
3. Open the repo's **Actions** tab. The "Build APK" run takes about 5–8 minutes.
4. When it goes green, open **Releases** (right side of the repo page) on your phone and download `FitLog.apk`.
5. Open the file. Android will ask you to allow installs from your browser/file manager. Allow it, then install.

Every later push builds a new version, which installs **over** the old one and keeps your data. This works because the app is always signed with the same key (`app/keystore/fitlog.jks`). Don't delete that file.

### Option B: Android Studio
Open the folder in Android Studio (Ladybug or newer), let Gradle sync, then choose **Build → Build App Bundle(s)/APK(s) → Build APK(s)**. You can also plug in your phone with USB debugging on and press ▶ Run.

---

## Project layout
```
app/src/main/java/com/fitlog/app/
  MainActivity.kt          navigation + bottom bar
  data/                    Room entities & DAOs, profile/TDEE maths, food & exercise libraries, backup
  ui/AppViewModel.kt       all app state, active workout, rest timer
  ui/screens/              Today, Food, Workout, Progress, Settings, Profile
  ui/components/           cards, ring, charts, dialogs
```
Built with Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12, Room 2.6.1, minSdk 26 (Android 8+), targetSdk 35.

## Notes
- Built-in nutrition values are typical averages. Homemade food changes a lot with oil and portion size, so save your regular dishes as **custom foods** for better accuracy.
- The signing passwords in `app/build.gradle.kts` are fine for a personal app you install yourself. Change them before you ever publish to the Play Store.
