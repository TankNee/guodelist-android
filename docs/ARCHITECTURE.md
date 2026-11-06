# Architecture

```
┌──────────────────────────── Compose UI ────────────────────────────┐
│ AppRoot (tabs, drawer, sheets)   CalendarScreen   SettingsScreen   │
│                  │                                                  │
│             AppViewModel  ── UiState(tasks, lists, settings, sync) │
└──────────────────┬───────────────────────────────┬────────────────┘
                   │                               │
           GuoStore (offline)                SyncManager
     JSON document + tombstones          /api/v1/sync, /auth/*
                   │                               │
                   └────────── TaskOps ────────────┘
                        pure merge / mutation
```

## Records are raw JSON on purpose

The desktop app owns the schema (`src/types.ts` in the Tauri repo). A phone that decoded those objects
into a narrow Kotlin data class would silently drop `attachments`, comments or any field added later
the first time it pushed. So the store keeps `JsonObject` and exposes typed accessors
(`TaskItem.title`, `.priority`, …). Edits rebuild the object with `with(...)`; unknown keys travel
through untouched. A test asserts this for `attachments`.

## Storage

`GuoStore` keeps one `guodelist.json` in `filesDir`, written atomically (`tmp` + rename) 220 ms after
the last mutation. Deletions add tombstones (`removed.tasks` / `removed.lists`), exactly like the
desktop store, so a merge cannot resurrect them.

The store exposes `localChanges`, which fires only for user edits. Remote merges update the state
without emitting, which is what keeps the sync loop from chasing its own tail.

## Sync

`SyncManager.syncNow()`:

1. snapshots every list and task plus tombstones;
2. `POST /api/v1/sync` with the bearer token;
3. `TaskOps.applyRemote()` merges the response: remote wins ties, tombstones win over older records,
   records unknown to the phone are kept;
4. `lastSync` is the server's `serverTime`.

Triggers: 1.8 s after a local edit, every 60 s while the process is alive, at app start (`bootstrap`
calls `/me` then syncs), and manually from Settings.

Auth is the same scrypt + HS256 JWT server the desktop uses. The token lives in a private
SharedPreferences file excluded from cloud backup (`res/xml/backup_rules.xml`).

## Language

`MainActivity.attachBaseContext()` wraps the context with a `Configuration` carrying `zh-CN` or `en`
when the user overrides the system language. Changing the choice in Settings writes the preference and
recreates the activity. All copy lives in `res/values/strings.xml` (English) and
`res/values-zh/strings.xml`; there are no hard-coded UI strings.

## Theme

`GuoColors` holds the product tokens (canvas, panel, line, ink, brand, danger…). Two palettes —
Classic and Starry — are orthogonal to light/dark, matching the desktop `data-pack`. `GuoTheme` also
flips the status-bar icon appearance when the in-app theme differs from the system.

## Why not Room?

The dataset is a single personal document of modest size; a JSON snapshot is what the desktop and the
server already exchange. It removes a code generator (KSP/kapt), keeps the build reproducible on a
plain JDK 17 + SDK 35 machine, and makes the "preserve unknown fields" guarantee trivial.
