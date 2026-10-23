# Mfuns Watch

Mfuns喵御宅非官方手表客户端，感谢使用。

## 目前支持内功能

- 账号密码登录
- 首页推荐 / 热榜 / 全站动态
- 搜索
- 分区浏览
- 文章详情 / 视频播放
- 评论区浏览与发送
- 点赞 / 收藏 / 投币
- 签到
- 收藏夹 / 浏览历史
- 私信会话与发送
- 用户主页

## 构建

环境要求：JDK 17+、Android SDK（compileSdk 36）。

```bash


./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/`。



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
    ├── theme/Theme.kt       主题
    ├── components/Common.kt 通用组件
    └── screens/              各页面
```

## 开源许可

[MIT](LICENSE)。

## 免责声明

本项目为非官方第三方客户端，与 Mfuns 官方无关联。接口来自社区及公开抓包整理，可能随官方调整而失效。
