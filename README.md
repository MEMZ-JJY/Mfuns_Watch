# Mfuns Watch

Mfuns（mfuns.net）的非官方开源手表客户端，基于 Jetpack Compose for Wear OS 构建，**同时支持 Wear OS 手表与普通 Android 手表**（圆表盘、小屏均可）。

数据来源于 Mfuns 社区公开接口（`api.mfuns.net`），仅供学习交流。

## 功能

- 账号密码登录（社区 token，本地安全存储）
- 首页推荐 / 热榜 / 全站动态
- 搜索（关键词 + 类型筛选）
- 分区浏览
- 文章详情 / 视频播放（ExoPlayer，默认最高清晰度）
- 评论区浏览与发送
- 点赞 / 收藏 / 投币
- 每日签到
- 收藏夹 / 浏览历史
- 私信会话与发送 / 通知未读数
- 用户主页与关注

## 构建

环境要求：JDK 17+、Android SDK（compileSdk 36、build-tools 36.1.0）。

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（R8 混淆 + debug 签名，可直接安装）
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/`。

> Windows 下可用 `.tools/gradle-8.14.3/bin/gradle.bat` 直接构建（无需安装 Gradle）。

## 安装到手表

```bash
adb connect <手表IP>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

或把 APK 传到手表（微信文件传输 / 蓝牙 / ADB push）后点击安装。

## 手表模拟器

仓库内置 `run-emulator.bat`，双击即可启动已配置好的 Wear OS 模拟器（AVD 名 `MfunsWatch`，Wear OS 5.1，454×454 大圆表）。

如需从零搭建，需要 SDK 中安装 `cmdline-tools` 与 `system-images;android-35-ext15;android-wear;x86_64`，然后：

```bash
avdmanager create avd -n MfunsWatch \
  -k "system-images;android-35-ext15;android-wear;x86_64" \
  -d wearos_large_round
```

## 项目结构

```
app/src/main/java/net/mfuns/watch/
├── MfunsApp.kt              Application + 服务定位器
├── MainActivity.kt          入口
├── api/
│   ├── ApiClient.kt         OkHttp 封装与全部端点
│   └── Models.kt            模型、JSON 宽松解析、仓库门面
├── data/SessionStore.kt     会话持久化（DataStore）
└── ui/
    ├── Vms.kt               ViewModel 与 UiState
    ├── AppNav.kt            导航路由
    ├── Formatters.kt        数字/时间格式化
    ├── theme/Theme.kt       品牌粉色主题
    ├── components/Common.kt 通用组件
    └── screens/              各页面
```

## 开源许可

MIT License，详见 [LICENSE](LICENSE)。

## 免责声明

本项目为非官方第三方客户端，与 Mfuns 官方无关联。接口来自社区公开抓包整理，可能随官方调整而失效。
