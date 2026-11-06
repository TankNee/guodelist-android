# 应用图标切换

Guodelist 0.4.12 支持在 **设置 → 应用图标** 切换桌面图标：

| 选项 | 形态 | 资源 |
|---|---|---|
| 默认 | 跟随系统自适应图标遮罩 | `@mipmap/ic_launcher` |
| 圆形 | 朱红圆形底 + 奶油色便签与勾 | `ic_launcher_circle_icon` |
| 方形 | 朱红圆角方形底 + 奶油色便签与勾 | `ic_launcher_square_icon` |
| 异形 | 倾斜 12° 的圆角方块 + 便签与勾 | `ic_launcher_irregular_icon` |

## 实现

Android 不允许在运行时直接替换 `application` 的图标，所以使用四个 `activity-alias`：

```text
.MainActivity          target，不再声明 LAUNCHER
.IconAdaptive          @mipmap/ic_launcher
.IconRound             @drawable/ic_launcher_circle_icon
.IconSquare            @drawable/ic_launcher_square_icon
.IconIrregular         @drawable/ic_launcher_irregular_icon
```

每个 alias 都带 `MAIN / LAUNCHER` intent filter，但同一时刻只启用一个。

`core/icon/LauncherIcon.kt` 负责：

1. 先启用目标 alias；
2. 再禁用其余 alias；
3. 使用 `DONT_KILL_APP`，切换时不杀进程；
4. 启动时按 `SettingsStore.iconStyle` 再校正一次，避免系统更新/备份恢复后状态错乱。

选择持久化在 `guodelist_settings` 的 `icon_style` 键。

## 注意

- 某些第三方桌面会缓存图标，切换后回桌面等待 1–2 秒，或重启桌面。
- 从旧版本升级后，默认仍使用自适应图标；用户主动选择才会切换到新形状。
- 图标是矢量 drawable，透明区域以外的形状由 launcher 决定是否再套一层白色底板；不同 ROM 表现略有差异。
- 新增图标时，需要同时创建 vector、alias、Settings 选项和 `LauncherIcon.aliases` 映射。
