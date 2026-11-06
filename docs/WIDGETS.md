# HyperOS 小组件设计

参考：[Xiaomi HyperOS 小部件设计规范](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1664)

Guodelist 0.4.12 提供三个标准 Android AppWidget，按 HyperOS 的尺寸、圆角、安全区、字阶和深色模式设计。
它们出现在系统「小部件」的 Android 小部件入口；若要进入 HyperOS 小部件中心，还需要用小米开发者账号提交审核。

## 设计基线

| 项 | 取值 |
|---|---|
| 尺寸 | 2×2、4×2、4×4 |
| 圆角 | 24dp（1080p 约 55px） |
| 安全区 | 内容距边缘 ≥16dp（1080p 约 42px） |
| 字体 | 系统 MiSans；`sans-serif` / `sans-serif-medium` |
| 主数字 | 27–43sp，Demibold/Medium |
| 标题/说明 | 10–14sp，次级文字降透明度 |
| 背景 | 暖白 `#FFFEFC`，深色 `#171614` |
| 强调色 | 朱红 `#E1543A`，深色 `#F2694C` |
| 交互 | 点击任务进详情；点击快速卡片打开新建；无滑动、无输入 |

## 1. Guodelist·今日

自适应三种布局，按桌面单元格高度切换：

| 尺寸 | 内容 |
|---|---|
| 2×2 | 未完成数量、完成进度条、下一条任务 |
| 4×2 | 日期 + 数量 + 进度 + 3 条任务 |
| 4×4 | 日期 + 大数字 + 进度 + 6 条任务 + “还有 N 项” |

- 每条任务单独设置 `PendingIntent`，点击直接打开任务详情，而不是只回首页。
- 时间优先显示 `09:30  任务标题`；无时间只显示标题。
- 没有任务时显示「今天没有安排」。
- 深色模式跟随系统。

布局：`widget_today_small.xml`、`widget_today.xml`、`widget_today_large.xml`

## 2. Guodelist·接下来

4×2 卡片，展示今天及未来最近的 3 条未完成任务，按日期、时间、手动顺序排序。

- 日期只显示 `月-日`，符合 HyperOS 日期表达规范。
- 没有内容时显示「接下来没有安排」。
- 根点击打开应用，刷新按钮立即重读本地数据。

布局：`widget_upcoming.xml`

## 3. Guodelist·新建任务

2×2 快速捕获卡，朱红底、白色文字，显示今天剩余数量。

- 点击整张卡片直接打开应用的新建任务 Sheet（`MainActivity.EXTRA_QUICK_ADD`）。
- 不包含输入框，避免违反小部件“禁止文字输入”的规范。
- 深色模式下使用更亮的强调色和深色前景，保证对比度。

布局：`widget_quick_add.xml`

## 数据与刷新

- `WidgetData` 直接读取应用私有 `guodelist.json`，不启动同步、不访问网络。
- 尊重设置里的「显示清单」；`Upcoming` 与 `Today` 使用同一组筛选。
- 任务变化、应用回到前台、点击刷新、系统 30 分钟周期都会刷新。
- `TodayWidgetProvider.refresh()` 会同时刷新 Today、Upcoming、Quick add。
- 所有点击都是显式 `PendingIntent`，没有 `RemoteViewsService` 滚动集合。

## 文件

```text
widget/
  WidgetData.kt                 读取快照，生成任务行
  WidgetSupport.kt              任务/应用/刷新 PendingIntent，行绑定，进度
  TodayWidgetProvider.kt        2×2 / 4×2 / 4×4 自适应
  UpcomingWidgetProvider.kt     4×2 接下来
  QuickAddWidgetProvider.kt     2×2 新建任务
res/layout/
  widget_today_small.xml
  widget_today.xml
  widget_today_large.xml
  widget_upcoming.xml
  widget_quick_add.xml
res/xml/
  widget_today_info.xml
  widget_upcoming_info.xml
  widget_quick_info.xml
```

## 验收检查

- [ ] 2×2、4×2、4×4 缩放后文字不截断
- [ ] 深色模式对比度正常
- [ ] 字体放大/TalkBack 下主要信息可读
- [ ] 点击任务进入对应详情
- [ ] 点击快速卡片打开新建 Sheet
- [ ] 清单筛选生效
- [ ] 刷新后数字和进度立即更新
- [ ] 无任务/无数据时显示空状态
