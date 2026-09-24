# Challenger

An Android app for personal challenges: set it up once, and it reminds you from
then on. You can tick a challenge off without opening the app at all.

English is the app's primary language; Russian ships as a translation.

## What it does

**Menu sections** — Sport (push-ups, pull-ups, barbell, abs, squats, running,
walking), Study (English, Spanish, IT) and your own challenges. Tapping a preset
opens the editor already filled in.

**A schedule per item**
- pattern: every day / every other day / chosen weekdays
- a duration in days, or no end date
- any number of reminder times during the day

**Priority** — must do, should do, nice to have. Must-do challenges come first on
the Today screen, are highlighted in red and use a high-importance notification
channel. Missing a nice-to-have does not break the streak.

**One tap to tick off** — from the notification (a "Done" button), from the home
screen widget, or in the app. Once ticked, the reminders for that day stop.

**Progress** — current streak, best streak, success rate, and a 12-week heat map
where you can still close a missed day.

**A companion** — she waits on the Today screen while the day is open and cheers
up with every tick, dancing once the whole day is closed. Artwork is pluggable:
see [docs/companion-art.md](docs/companion-art.md).

Everything is stored on the phone. No account, no internet.

## Stack

- Kotlin, Jetpack Compose, Material 3 (dynamic colour, light and dark)
- Room for local storage
- AlarmManager (`setExactAndAllowWhileIdle`) for precise reminders
- WorkManager for a daily rebuild of the schedule, as a safety net
- Glance for the Today home screen widget
- Lottie for companion animations
- minSdk 26, targetSdk 35

## Layout

```
data/model   Challenge and Completion entities, enums
data/db      Room: DAOs, converters, database
data/prefs   DataStore settings for the companion
data/repo    ChallengeRepository — the only place data changes
domain       Schedule (which day is active), Stats (streaks), Companion (mood)
notify       notifications, alarm scheduler, receivers, worker
widget       the Glance widget and its actions
ui           Today / Challenges / Editor / Stats / Settings screens
```

Two decisions hold the app together. The schedule is computed in
`domain/Schedule.kt` and nowhere else, and data changes only through
`ChallengeRepository`, which rebuilds the alarms and refreshes the widget after
every write. That is why the screens, the notification shade and the home screen
never drift apart.

## Build

```bash
./gradlew :app:assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # schedule and streak tests
```

Needs JDK 17+ and Android SDK 35. The SDK path goes in `local.properties`.

## On a real phone

1. Allow notifications and exact alarms — both are offered on the Settings screen.
2. Turn battery optimisation off for the app, or Android may delay reminders.
   Same screen.
3. Add the Today widget to your home screen.
