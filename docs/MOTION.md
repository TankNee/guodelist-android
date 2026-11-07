# Motion system

The goal is the feel of [openGym](https://github.com/DuarteSantos8/openGym): movement that reads as
weight and friction, not decoration. Nothing bounces for the sake of bouncing, and nothing animates
on first paint except the content that actually changed.

## Tokens

All of them live in `ui/motion/GuoMotion.kt`.

| Token | Value | Used for |
|---|---|---|
| `TAP_MS` | 120 ms | press acknowledgements, colour fades |
| `QUICK_MS` | 180 ms | swipe backgrounds, small reveals |
| `STANDARD_MS` | 240 ms | content swaps, screen push-in |
| `SLOW_MS` | 320 ms | calendar month changes |
| `emphasized` | `cubic-bezier(.16, 1, .3, 1)` | sheets, screen pushes |
| `standard` | Material fast-out-slow-in | exits, cross-fades |
| `springGentle` | damping 0.86, stiffness 380 | rows, progress, tab tint |
| `springSnappy` | damping 0.72, stiffness 900 | press scale, FAB |
| `springBouncy` | damping 0.55, stiffness 520 | checkmark overshoot |

## Rules

1. **A finger gets a spring.** Scales, swipes and the checkmark are springs so an interrupted gesture
   keeps its velocity. Content that appears on its own gets a 180–320 ms bezier.
2. **Tap feedback stays under 140 ms.** `Modifier.pressScale` squashes to 0.965 for 120 ms and
   releases with stiffness 1000. You should feel the press, not wait for it.
3. **No list entrance animations.** Rows appear immediately; moving them uses
   `Modifier.animateItem()`. A screen with thirty tasks should never feel like it is booting.
4. **Completion is drawn.** The ring fills, then the check stroke scales from 0 to 1 in 150 ms, then
   the row leaves. The task disappears *after* the gesture has an answer.
5. **A swipe is a decision at 36% travel.** Right completes, left deletes (with Undo). Both fire a
   haptic before the state change.
6. **Screens move 7% of their width.** `slideInHorizontally { it / 14 }` plus a 220 ms fade, so the
   outgoing view does not slide across the whole screen.
7. **Sheets keep Material's spring** and a custom 38 × 4 dp handle; they open fully
   (`skipPartiallyExpanded = true`).
8. **Today's progress bar and the month title animate their value, not their layout.** Width and text
   cross-fade; nothing around them jumps.

## Reduced motion

`GuoMotion` durations are short enough to stay comfortable, but any new long-running animation should
check `Settings.Global.ANIMATOR_DURATION_SCALE` and collapse to a fade when animations are disabled.
The current build uses system-animation-scale-aware `animate*AsState` for the critical paths.
