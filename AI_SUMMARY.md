# 通知转发助手 - AI开发总结文档

## 项目背景

本项目由AI助手（GLM-5）辅助开发完成。以下是开发过程中的完整总结。

## 项目目标

开发一个Android应用，解决小米运动健康无法选择双开应用通知的问题：
- 小米运动健康的通知设置中无法选择双开应用
- 双开的微信、QQ等应用的通知无法推送到小米手环
- 需要一个中间应用来转发双开应用的通知

## 技术方案

### 核心原理
1. 使用 `NotificationListenerService` 监听系统所有通知
2. 通过反射获取通知的 `userId` 来识别双开应用
3. 用户选择需要转发的应用后，将通知重新发送
4. 小米运动健康监听到转发通知后推送到手环

### 双开应用识别
MIUI的应用双开通过创建独立的用户空间实现：
- 主用户空间：userId = 0
- 双开用户空间：userId = 999 或其他非0值

应用唯一标识：`包名_userId`

## 项目结构

```
app/src/main/java/com/notifyforwarder/
├── App.kt                    # Application类，通知渠道配置
├── manager/
│   └── AppManager.kt         # 应用列表管理，双开应用扫描
├── service/
│   └── NotificationListener.kt  # 核心通知监听服务
├── ui/
│   ├── MainActivity.kt       # 主界面
│   └── AppManagerActivity.kt # 应用管理界面
├── receiver/
│   └── BootReceiver.kt       # 开机启动
└── model/
    └── AppInfo.kt            # 应用信息数据类
```

## 开发过程中的关键问题

### 1. 应用列表为空
**原因**：Android 11+ 包可见性限制
**解决**：添加 `QUERY_ALL_PACKAGES` 权限和 `<queries>` 配置

### 2. ForegroundService类型错误
**原因**：Android 14+ 要求指定 foregroundServiceType
**解决**：使用 `specialUse` 类型并添加 property 配置

### 3. MIUI只能接收自己应用的通知
**原因**：AndroidManifest中添加了 `default_filter_types` meta-data
**解决**：移除该配置

### 4. 双开应用识别
**原因**：UserHandle.getIdentifier() 是隐藏API
**解决**：使用反射调用

### 5. 服务状态不显示
**原因**：前台服务未正确启动
**解决**：在 onListenerConnected() 中调用 startForeground()

### 6. 通知重复显示
**尝试方案**：
- 静默通知（小米运动健康无法捕获）
- 延迟取消（时间难以把握）
- 通知分组（效果有限）

**当前方案**：保留转发通知，用户可手动划掉

## UI设计

采用Google Material Design 3扁平化风格：
- 主色：#1A73E8 (Google蓝)
- 运行状态：绿色文字
- 停止状态：红色文字
- 扁平列表，细分隔线

## 权限要求

- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `QUERY_ALL_PACKAGES`

## MIUI适配

- 自启动权限引导
- 电池优化豁免
- 后台运行权限

## 测试验证

使用自建测试应用 NotifyTest 进行测试：
1. 安装并授予权限
2. 选择转发应用
3. 发送测试通知
4. 验证手环收到通知

## 已知限制

1. 转发通知会在通知栏显示
2. 需要用户手动在小米运动健康中开启本应用的通知权限
3. 需要MIUI自启动权限保持后台运行

## 开发工具

- Android SDK 34
- Gradle 8.4
- Kotlin
- Material Design 3

## 版本历史

- v1.0.0 (2026-03-22): 初始版本发布

---

*本文档由AI助手生成，记录开发过程中的技术决策和问题解决方案。*
