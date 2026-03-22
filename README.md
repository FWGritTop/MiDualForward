# MiDualForward

<p align="center">
  <strong>Forward notifications from dual apps to your Xiaomi bracelet</strong>
</p>

<p align="center">
  <em>小米运动健康无法选择双开应用通知？MiDualForward 帮你解决！</em>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#installation">Installation</a> •
  <a href="#usage">Usage</a> •
  <a href="#how-it-works">How it Works</a>
</p>

---

## Features

- ✅ Monitor all app notifications
- ✅ Support MIUI dual app detection
- ✅ Select apps to forward
- ✅ Search functionality
- ✅ Auto-forward to Xiaomi bracelet
- ✅ Auto-start on boot
- ✅ Material Design 3 UI

## Background

Xiaomi Fitness has a custom notification feature, but it cannot send notifications for dual apps (like dual WeChat, QQ, etc.). This AI-generated app forwards notifications from selected apps, enabling Xiaomi Fitness to forward messages to your bracelet.

## Installation

### Option 1: Download APK

Download [NotifyForwarder-v1.0.0.apk](./NotifyForwarder-v1.0.0.apk) directly from this repository.

### Option 2: Build from Source

```bash
git clone https://github.com/FWGritTop/MiDualForward.git
cd MiDualForward
./gradlew assembleRelease
```

## Usage

### Step 1: Grant Permissions

1. Open the app
2. Tap "Grant Permission"
3. Enable notification listener permission for "MiDualForward"

### Step 2: Select Apps

1. Tap "App Manager"
2. Search for apps you want to forward
3. Enable the switch
4. Dual apps will show as "App Name (Dual)"

### Step 3: Configure Xiaomi Fitness

1. Open Xiaomi Fitness
2. Go to Device Settings → Notification Settings
3. Enable notification permission for "MiDualForward"

### Step 4: Done!

Selected app notifications will now be forwarded to your Xiaomi bracelet.

## How it Works

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Dual App   │────▶│  MiDualForward   │────▶│Xiaomi Fitness│
│  (WeChat)   │     │(NotificationListener)│    │             │
└─────────────┘     └──────────────────┘     └──────┬──────┘
                                                    │
                                                    ▼
                                              ┌─────────────┐
                                              │Xiaomi Bracelet│
                                              └─────────────┘
```

1. Use `NotificationListenerService` to monitor notifications
2. Detect dual apps via `userId` using reflection
3. Re-post notifications from selected apps
4. Xiaomi Fitness captures and forwards to bracelet

## Permissions

| Permission | Purpose |
|------------|---------|
| Notification Listener | Monitor system notifications |
| Notification | Post forwarded notifications |
| Boot Completed | Auto-start on boot |
| Background | Keep service running |

## Compatibility

- Android 7.0+
- MIUI 12+
- Xiaomi Fitness

## Known Issues

- Forwarded notifications appear in notification bar (can be dismissed manually)
- Requires MIUI auto-start permission for background operation

## License

MIT License

## Contributing

Issues and Pull Requests are welcome!

## Acknowledgments

- Material Design 3
- Android NotificationListenerService
- AI-assisted development (GLM-5)
