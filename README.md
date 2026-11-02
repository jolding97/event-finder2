# EventRadar

An Android app where you list the things you're into (WEC, rally, bands, teams, comedy…) and it searches for UK events and puts them on a calendar. Each event shows the date, time, price, venue, a thumbnail image, and a link to the event page.

## What it does

- **Interests tab:** add interests with a category. You can optionally attach a free calendar feed, which is ideal for racing and rally series. There are quick-add chips for common picks. Tap an interest to add or change its feed. A new interest is searched straight away.
- **Calendar tab:** a month grid where days with events show the event's picture and coloured dots, one per interest. Tap a day to see its events. The **Upcoming** tab lists everything by date. Filter chips narrow it to one interest.
- **Event sheet:** a large image, date and time, venue, price and source. It has buttons to **open the event page**, **add it to your phone's calendar** (Google Calendar etc., no permission needed) and **share** it.
- **Refresh:** tap ↻, choose how far ahead to look (1, 3, 6 or 12 months; 3 is the default), and every interest is searched with a progress bar. Your choice becomes the default.
- **Background refresh:** every interest is searched again once a day. You can turn this off in Settings. Past events are removed automatically.

## Where events come from

Everything is free. There's no AI and no pay-as-you-go service.

| Source | Setup | Good for |
|---|---|---|
| [Ticketmaster Discovery API](https://developer.ticketmaster.com/) | Free key, pasted in Settings (5,000 calls/day) | Concerts, football, rugby, cricket, comedy, theatre |
| [Skiddle API](https://www.skiddle.com/api/join.php) | Free key, pasted in Settings | UK gigs, club nights, festivals |
| Calendar feeds (.ics / webcal) | No key. Paste a feed link on an interest | Racing and rally series (BTCC, BSB, British GT, rally championships), club fixtures, venue calendars |

**Racing calendars, built in:** the Interests tab has a *Racing calendars* list with all 56 series from [toomuchracing.com](https://toomuchracing.com/calendar/): F1, F2, F3, Formula E, IndyCar, MotoGP, WSBK, BSB, NASCAR, WEC, ELMS, IMSA, British GT, DTM, BTCC, Supercars, WRC, ERC, rallycross, drag racing, the Isle of Man TT and Goodwood. Tick a series and every round goes on your calendar. You can keep all rounds worldwide, which is good for TV, or only UK rounds. Each round links to a ticket search, and its thumbnail comes from the series' website.

**Your own calendar feeds** cover anything else. Most series, and fan calendar sites (e.g. sync2cal, toomuchracing's Google calendars), offer an "Add to calendar", "Subscribe" or "iCal" link. Copy it and paste it into the interest. With **Only keep UK rounds** on, international series such as WEC or WRC are trimmed to their UK rounds. Turn it off for feeds that are already UK-only, like a British rally championship. Feeds usually don't include prices, so those events show "Price TBC" and link to the event page.

Events missing an image get one from the event page's share image (`og:image`).

## Building the APK

### Option A: GitHub (no software to install)
1. Create a new GitHub repository and upload this folder's contents. You can drag and drop the files in the web UI, but make sure the hidden `.github` folder is included.
2. Open the **Actions** tab. The *Build APK* workflow runs on every push (or press *Run workflow*).
3. When it finishes, download the **EventRadar-apk** artifact, unzip it and copy `app-release.apk` to your phone.
4. Open the APK on the phone and allow "install unknown apps" when asked.

### Option B: Android Studio
1. Open the `EventRadar` folder in Android Studio (Ladybug or newer).
2. Let Gradle sync, then use **Run ▶** with your phone connected (USB debugging on), or **Build → Build APK(s)**.

The release build is signed with the debug key so it installs straight away. Set up your own signing key before publishing to the Play Store.

## Tips for good results
- Be specific: "Arctic Monkeys" beats "indie music"; "British Rally Championship" beats "rally".
- WEC currently has no UK round, so a WEC feed with "UK only" switched on will come back empty. Adding "Goodwood" or "Silverstone" picks up UK endurance and historic racing.
- Ticketmaster's keyword search is loose. A very generic interest like "rally" may pull in unrelated events, so prefer specific names, or use a feed instead.

## Project layout
```
app/src/main/java/com/eventradar/
  EventRadarApp.kt          app setup
  MainActivity.kt           scaffold + bottom navigation
  data/                     Room database, settings, repository (merging and de-duplication)
  sources/                  Ticketmaster, Skiddle, calendar feed (.ics) parser, thumbnail finder
  work/RefreshWorker.kt     daily background refresh
  ui/                       Compose screens (calendar, interests, settings, event sheet)
```
Stack: Kotlin, Jetpack Compose (Material 3), Room, WorkManager, OkHttp, kotlinx.serialization, Coil. Min Android 8.0.
