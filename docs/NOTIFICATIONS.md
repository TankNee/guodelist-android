# 通知、保活与 MiPush 接入调研

## 1. 现状

### Windows 通知没有声音

桌面端使用 `@tauri-apps/plugin-notification`。在 Windows 上它最终走
`notify-rust` + `winrt-notification`：

- 没有设置 `sound` 时，插件传入 `sound_name=None`；
- `notify-rust` 对 `None` 生成 `<audio silent="true"/>`；
- 所以 Toast 出来了，但是**静音**。

修复：发送通知时显式请求默认提示音。

```ts
sendNotification({ title, body, sound: "Default" });
```

0.9.7 已加入该参数，并增加了 `desktopNotification()` 的单元测试。
如果仍然没有声音，依次检查：

1. Windows 设置 → 系统 → 通知 → Guodelist → 允许通知、播放声音；
2. 专注助手/勿扰模式；
3. 音量合成器里 Guodelist 是否被静音；
4. 系统提示音方案是否被改成“无”。

### Android 为什么会被杀

当前 Android 用 `WorkManager` 的 one-time work 保存提醒时间。它有几个现实限制：

- WorkManager 是**延迟不精确**的，Doze/OEM 调度会推迟；
- 应用被 HyperOS 清理、用户“强行停止”后，WorkManager 队列不会执行；
- 单纯提高进程优先级、1px 前台服务、双进程互拉等“保活”手段在 Android 12+
  不可靠，也会增加耗电和审核风险。

结论：**本地提醒不能保证 100% 准时**；要可靠，必须让系统推送通道在应用进程不运行时
也能展示通知。小米设备上这就是 MiPush。

## 2. 已做的改进

### Windows

- 显式 `sound: "Default"`；
- 测试通知和真实提醒共用同一个发送函数。

### Android 0.4.15

- 新增 `BootReceiver`：开机、应用更新后从本地任务重建提醒队列；
- 通知 Channel 显式设置系统默认提示音与 `USAGE_NOTIFICATION_EVENT`；
- 保留 WorkManager 作为基础调度；
- 设置页已有“电池设置”入口，可进入系统电池优化页面。

这些改动能减少“重启后不再提醒”，但**不能解决强行停止/系统清理**。

## 3. 推荐的 Android 保活组合

按优先级：

| 手段 | 作用 | 限制 |
|---|---|---|
| WorkManager | 应用进程被普通回收后仍可由系统恢复 | 不精确，强行停止后失效 |
| `AlarmManager.setExactAndAllowWhileIdle` | 精确到分钟 | Android 12+ 需要精确闹钟权限；仍受 OEM 限制 |
| `BOOT_COMPLETED` 重建队列 | 重启后恢复 | 不能绕过强行停止 |
| 电池优化白名单 | 降低被冻结概率 | 需要用户手动授权 |
| HyperOS 自启动/后台无限制 | 降低被清理概率 | 需要用户手动设置，不能代码强制 |
| MiPush 系统通道 | 进程不在也能收到服务端提醒 | 依赖小米推送、网络、通知权限；强行停止仍可能收不到 |

不建议：

- 常驻前台服务只为了保活（耗电、通知栏常驻、Android 14 限制）；
- 双进程/JobScheduler 互拉；
- 隐藏图标、引导用户关闭电池优化但不说清楚用途。

## 4. MiPush 接入步骤

官方文档：

- [Android 客户端 SDK（AAR）](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1692)
- [服务端 HTTP API](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1559)
- [推送服务开通](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1691)

### 4.1 小米开放平台准备

1. 注册/登录小米开发者账号；
2. 创建应用，包名填 `cn.neeto.guodelist`；
3. 填写 Release 签名 SHA-1/SHA-256（必须是发布签名，不是 debug）；
4. 开通“推送服务”，拿到：
   - `AppID`
   - `AppKey`
   - `AppSecret`（**只能放服务端**）
5. 在推送后台配置 Android 包名、应用签名、通知渠道和消息分类；
6. 如果应用要上架小米应用商店，按商店要求提交隐私政策和推送 SDK 声明。

### 4.2 Android 客户端

1. 从 `https://admin.xmpush.xiaomi.com/.../downpage` 下载 AAR；
2. 放到 `app/libs/MiPush_SDK_Client_x.x.x.aar`；
3. 在 `app/build.gradle.kts` 加 `flatDir` 和 AAR 依赖；
4. 新建 `PushMessageReceiver` 子类，处理：
   - `onReceiveRegisterResult`：拿 `regId`，上报服务端；
   - `onNotificationMessageClicked`：带 `taskId` 打开任务详情；
   - `onReceivePassThroughMessage`：可选，处理透传；
5. 在 `Application.onCreate()` 的主进程里初始化：

```kotlin
MiPushClient.setRegion(Region.China) // 大陆版；海外版按机房选择
MiPushClient.registerPush(context, APP_ID, APP_KEY)
```

6. 登录后设置用户维度：

```kotlin
MiPushClient.setUserAccount(context, accountId, null)
MiPushClient.setAlias(context, "user:$accountId", null)
```

7. 退出登录时 `unsetUserAccount` / `unsetAlias`，避免把通知推给下一个账号；
8. 注册结果上报服务端：

```http
POST /api/v1/push/devices
Authorization: Bearer <user-token>

{ "provider": "mipush", "regId": "...", "region": "China", "appVersion": "0.4.15" }
```

> 注意：`regId` 会变化，登录、重装、恢复备份后都要重新上报；服务端按
> `(userId, provider, regId)` 去重并更新 `lastSeenAt`。

### 4.3 服务端

服务端新增 `devices` 表和定时任务：

```text
devices(id, userId, provider, regId, region, appVersion, lastSeenAt, enabled)
```

定时任务每分钟：

1. 找出 `dueDate + dueTime - reminderMinutes <= now` 且未完成、未推送的任务；
2. 按 `userId` 找启用的小米设备；
3. 调 MiPush HTTP API；
4. 写 `push_log(taskId, userId, msgId, status, sentAt)` 做幂等，避免重复推送。

单条推送示例：

```http
POST https://api.xmpush.xiaomi.com/v3/message/regid
Authorization: key=<APP_SECRET>
Content-Type: application/x-www-form-urlencoded

registration_id=<regId>
restricted_package_name=cn.neeto.guodelist
title=任务提醒
description=下午去医院体检 · 今天 15:00
payload={"taskId":"tsk_123"}
notify_id=12345
extra.channel_id=reminders
extra.notify_effect=1
extra.sound_uri=android.resource://cn.neeto.guodelist/raw/notification
```

关键参数：

- `Authorization: key=<APP_SECRET>`，**不能放客户端**；
- `notify_id` 用任务 id 的稳定整数，重复发送会覆盖而不是刷屏；
- `extra.channel_id` 必须与客户端创建的 Channel 一致；
- `extra.notify_effect=1` 打开 Launcher，或在客户端自定义点击行为；
- `extra.sound_uri` 可指定 `res/raw` 的提示音；
- 定时消息可用 `time_to_send`（仅 7 天内），更推荐服务端定时任务；
- 失效 `regId` 通过回执/错误码清理。

### 4.4 消息分类与 HyperOS

MIUI/HyperOS 对通知分类和推送通道有额外要求：

- 在小米推送后台申请合适的消息分类（任务提醒/IM 等）；
- 客户端创建同名 Channel，并允许声音/震动；
- 用户关闭通知权限、关闭 Channel、开启勿扰，系统仍不会展示；
- 应用被“强行停止”后，MiPush 也可能收不到，直到用户再次打开应用；
- 非小米设备没有小米系统通道，需要华为/OPPO/vivo 等厂商推送或回退本地提醒。

## 5. 建议的落地顺序

1. **0.9.7 / 0.4.15**：Windows 声音、Android 开机重建、通知 Channel 声音（本次）；
2. **下一步**：申请小米推送，接入客户端 SDK 和 `regId` 上报；
3. 服务端加 `devices`、`push_log` 和每分钟提醒任务；
4. 先在小米真机验证：正常、后台、锁屏、重启、通知权限关闭、强行停止；
5. 再考虑厂商推送聚合（个推/极光/厂商通道）覆盖非小米设备。

## 6. 验收清单

- [ ] Windows 测试通知有声音
- [ ] Windows 真实提醒有声音
- [ ] Android 重启后仍能收到提醒
- [ ] Android 后台 30 分钟后仍能收到提醒
- [ ] Android 锁屏能收到
- [ ] 通知点击进入对应任务
- [ ] 关闭通知权限时有明确引导
- [ ] 同一任务不会重复推送
- [ ] 退出登录后不再收到该账号推送
- [ ] MiPush 失效 regId 会被清理
