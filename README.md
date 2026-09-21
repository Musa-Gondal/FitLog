# FitLog: workout, calorie and body tracker (Android)

Native Android app (Kotlin + Jetpack Compose + Room). Works fully offline and keeps all data on your phone.

## Features

### New in 1.1
**Coaching engine** (all maths in `data/Coach.kt`, so every number can be traced to a formula)
- **Trend weight.** Daily weigh-ins are smoothed (10 % exponential moving average, adjusted for gaps), so water and food swings don't mislead you.
- **Adaptive maintenance calories.** Over the last 28 days: average logged intake − (trend weight change × 7700 kcal/kg). Partial-log days are ignored, implausible results are limited, and the estimate is blended with the formula by a confidence score. You can apply it at the weekly check-in.
- **Goal rate.** Set a target of 0.25–1 % of bodyweight per week to lose, or 0.1–0.5 % to gain. The daily calorie change is calculated from that rate. Calories never drop below your BMR or 1200/1500 kcal. Protein is 2.1 g/kg when cutting and 1.8 g/kg otherwise; above BMI 27 it is based on your weight at BMI 25.
- **Weekly check-in.** This week vs last week for calories, protein days, workouts, hard sets, trend change vs plan, steps, water and PRs, plus a suggested new target.
- **Insights** tell you if you're losing too fast or not at all, how many kcal to adjust, when protein is short (with food fixes), when you're over calories, when a muscle is under- or over-trained, when a weigh-in is missing, and how your hydration is going.
- **Progressive overload.** Double progression per exercise: when every set reaches the top of the rep range, the coach suggests the next weight (+2.5 kg upper body, +5 kg lower body, +2 kg dumbbells, or 5/10 lb). It handles RPE 10 sets, detects strength (≤5-rep) training, suggests a deload after repeated misses, and flags a **plateau** after 3 sessions without progress.
- **Hard sets per muscle** each week, compared with the 10–20 set guideline (secondary muscles count half).

**Faster workout logging**
- Suggested weight and reps appear **greyed-out in every set**. Tick ✓ to log a set exactly as suggested. "Done" on the keyboard also completes the set.
- Tap **Previous** to copy last time's numbers. New sets copy the one before. "Fill coach targets" fills all sets at once.
- Set types (warm-up, drop set, failure) and **RPE**: tap the set number. A one-tap **warm-up ramp** (bar → 50 → 70 → 85 %) is available. Warm-ups are left out of volume, PRs and progression.
- **Supersets**: the rest timer only starts after the last exercise in the group.
- Per-exercise **notes**, **form cues and muscle info** for every exercise, and a **plate calculator** that draws the plates for each side.
- Rest timer with a progress bar and an "up next" label. It also shows as a **live countdown on the lock screen and in notifications**, and alerts you if the app is in the background.
- Keep-screen-on while training, a session progress bar, and a workout-complete summary with PRs.

**Reminders** (smart: each one skips itself if you've already done it)
- Breakfast, lunch and dinner logging
- Water every 1–3 h within a time window, with a **+250 ml button in the notification**
- Morning weigh-in
- Workout days at a chosen time
- Weekly check-in

**Motivation**
- 30 **achievement badges** in bronze, silver and gold (workout counts, streaks, protein/calorie/water runs, PRs, 100 kg club, bodyweight bench, 1.5× squat, 2× deadlift, total tonnage, weight milestones, Perfect Day…). Each shows its progress, and you get a celebration when you unlock one.
- **Calendar heatmap** (18 weeks) in Overall, Training or Nutrition mode. Tap a day to see which goals were hit.
- Current and best streak.

**UI**
- Redesigned Today screen with a hero calorie ring, animated macro rings, quick actions, the top coach insights, tappable water drops and a streak chip.
- Undo when you delete a food entry, haptic feedback, animations, and badges on the nav bar for a running workout or a due check-in.
- Progress now has five tabs: Coach · Body (trend chart with range filter) · Nutrition (7/14/30 days) · Strength (next-session advice) · Streaks.

### Core features
- Food log by meal with 150+ built-in foods, including Pakistani staples. Search, recent foods, custom foods, quick add and copy from the previous day.
- Workouts from routines (starter Push/Pull/Legs and Full-body included), nearly 80 exercises plus custom ones, and history with a repeat option.
- Weight, body measurements, PRs and estimated 1RM charts.
- Backup export/import to JSON and kg/lb units. Updating from 1.0 keeps all your data (the database migrates automatically).

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
  MainActivity.kt          navigation, bottom bar, global dialogs
  data/                    Room entities & DAOs (+ v1→v2 migration), profile/targets, food & exercise libraries, backup
  data/Coach.kt            coaching engine: trend, adaptive TDEE, overload, weekly report, insights
  data/Achievements.kt     badges, streaks, per-day facts for the heatmap
  notify/                  reminders (AlarmManager), rest-timer notification, boot receiver
  ui/AppViewModel.kt       all app state, active workout, rest timer
  ui/screens/              Today, Food, Workout, Progress, Settings, Profile
  ui/components/           cards, ring, charts, dialogs
```
Built with Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12, Room 2.6.1, minSdk 26 (Android 8+), targetSdk 35.

## Notes
- Built-in nutrition values are typical averages. Homemade food changes a lot with oil and portion size, so save your regular dishes as **custom foods** for better accuracy.
- The signing passwords in `app/build.gradle.kts` are fine for a personal app you install yourself. Change them before you ever publish to the Play Store.
