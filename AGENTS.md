# 🧸 小孩陪伴 APP — 项目档案

> 用于 AI 助手跨设备继续开发的完整上下文
> 最后更新: 2026-06-16

---

## 📋 项目概览

**项目名称:** Pixel Buddy (像素伙伴) — 儿童陪伴 AI 应用

**仓库地址:** `https://github.com/zwhuang89725-rgb/judy-ai.git`

**项目目标:** 为 2-6 岁幼儿提供 AI 陪伴的 Android 应用，支持聊天、实时语音、讲故事、小游戏四大功能。

**开发语言:** Kotlin + Jetpack Compose (Android 原生)

**当前阶段:** UI 原型设计完成，Android 项目骨架搭建中，核心功能部分实现

---

## 📁 项目结构

```
D:\Judy\
├── index.html                    # 风格选择页（3套UI预览入口）
├── preview-candy-land.html       # 🍭 糖果乐园 — 软萌糖果色
├── preview-pixel-buddy.html      # 👾 像素伙伴 — 复古游戏风
├── preview-starry-fairytale.html # 🌙 星空童话 — 睡前故事风
├── AGENTS.md                     # 本文档 — 项目档案
├── .gitignore
│
└── pixel-buddy/                  # Android 原生应用
    ├── build.gradle.kts           # 顶层构建文件
    ├── settings.gradle.kts        # 阿里云镜像 + 项目设置
    ├── gradle.properties
    ├── gradlew / gradlew.bat
    └── app/
        ├── build.gradle.kts       # 依赖配置
        ├── proguard-rules.pro
        └── src/main/
            ├── AndroidManifest.xml
            ├── assets/
            │   └── stories.json   # 内置故事数据
            ├── res/               # 资源文件（图标、主题、颜色）
            └── java/com/pixelbuddy/app/
                ├── PixelBuddyApp.kt          # Application（Hilt入口）
                ├── MainActivity.kt           # 主 Activity + 导航
                ├── di/
                │   └── AppModule.kt          # Hilt 依赖注入
                ├── domain/
                │   ├── model/
                │   │   └── Models.kt         # 所有数据模型
                │   └── usecase/
                │       ├── VoiceChatUseCase.kt
                │       └── AlwaysOnVoiceUseCase.kt
                ├── data/
                │   ├── local/
                │   │   └── AppDatabase.kt    # Room 数据库
                │   ├── remote/
                │   │   ├── AIService.kt       # AI 接口抽象
                │   │   ├── AIServiceImpl.kt   # 实现
                │   │   └── ApiInterfaces.kt   # Retrofit 接口
                │   ├── audio/
                │   │   ├── AudioPlayer.kt
                │   │   ├── AudioRecorder.kt
                │   │   ├── StreamingAudioRecorder.kt
                │   │   └── VoiceActivityDetector.kt
                │   └── repository/
                │       ├── ChatRepository.kt
                │       ├── StoryRepository.kt
                │       └── AvatarRepository.kt
                └── presentation/
                    ├── theme/PixelBuddyTheme.kt
                    ├── navigation/Screen.kt
                    ├── onboarding/
                    ├── chat/
                    ├── voice/
                    ├── story/
                    ├── game/
                    ├── settings/
                    └── components/PixelAvatar.kt
```

---

## 🏗️ 技术架构

| 层级 | 技术 | 说明 |
|------|------|------|
| UI | Jetpack Compose + Material3 | 像素风主题（暗色背景 + 霓虹绿 #00FF88） |
| DI | Hilt (Dagger) | AppModule 提供所有依赖 |
| 本地存储 | Room + DataStore | 聊天记录 + 配置偏好 |
| 网络 | Retrofit + OkHttp | 调用 AI API |
| 音频 | Media3 ExoPlayer | 播放/录音 |
| 导航 | Navigation Compose | 5 Tab 底部导航 |

### AI 模型支持

支持国内主流大模型，用户在设置页可自由切换：

**聊天 (Chat):** DeepSeek / 通义千问 / 智谱GLM-4 / Moonshot / 百川 / 硅基流动等
**语音识别 (STT):** 阿里云 Paraformer / 讯飞 / 火山引擎 / Whisper
**语音合成 (TTS):** 小米 MiMo / 火山引擎 / 阿里云 CosyVoice / 讯飞 / FishAudio

默认预设: DeepSeek Chat API (`https://api.deepseek.com/v1`)

---

## 🎨 UI 设计 — 三套风格

请直接在浏览器打开 `index.html` 查看效果。

### 1️⃣ 糖果乐园 (Candy Land) — `preview-candy-land.html`
- **风格:** 软萌糖果色 · 大圆角 · 白底浅色
- **配色:** 粉红 `#FFB5C2` + 薄荷绿 `#A8E6CF`
- **角色:** 🐻 糖果小熊
- **适合:** 女宝宝 / 低龄幼儿

### 2️⃣ 像素伙伴 (Pixel Buddy) — `preview-pixel-buddy.html`
- **风格:** 复古游戏风 · 霓虹色 · 像素边框
- **配色:** 霓虹绿 `#00FF88` + 深蓝 `#1A1A2E`
- **字体:** VT323 / Press Start 2P (Google Fonts)
- **角色:** 👾 Pixel Buddy
- **特色:** 扫描线效果、像素表情动画、clip-path 切角

### 3️⃣ 星空童话 (Starry Fairytale) — `preview-starry-fairytale.html`
- **风格:** 睡前故事风 · 月光金 · 暗色护眼
- **配色:** 月光金 `#F9D56E` + 星空紫 `#C3AED6` + 深海蓝 `#1B2A4A`
- **角色:** ⭐ 星星伙伴
- **特色:** 星星闪烁动画、旋转渐变光环、脉冲呼吸灯

> ⚠️ **当前状态:** 三套只是 HTML 原型预览，Android 项目只实现了「像素伙伴」一套主题。最终 APP 需要把选中的 UI 风格移植到 Compose 中。

---

## 🔧 构建 & 运行

```bash
# 克隆仓库
git clone https://github.com/zwhuang89725-rgb/judy-ai.git
cd judy-ai/pixel-buddy

# 用 Android Studio 打开 pixel-buddy/ 目录
# 等待 Gradle 同步完成
# 连接 Android 设备或模拟器
# 点击 Run
```

**SDK 版本:** compileSdk=36, minSdk=26, targetSdk=36  
**Kotlin:** 1.9.22  
**Gradle:** 8.5

---

## ✅ 已完成的功能

- [x] 项目骨架搭建 (Hilt + Room + Retrofit + Navigation)
- [x] 3 套 UI 风格 HTML 预览原型
- [x] 像素伙伴 Android 主题 (PixelBuddyTheme.kt)
- [x] 底部 5 Tab 导航 (Chat / Voice / Story / Game / Settings)
- [x] Onboarding 引导页
- [x] 聊天界面 + ViewModel (ChatScreen.kt)
- [x] 语音页面 UI (VoiceScreen.kt)
- [x] 故事页面 UI + 内置故事数据 (StoryScreen.kt, stories.json)
- [x] 设置页面 + AI 模型配置 (SettingsScreen.kt)
- [x] 头像自定义设置 (AvatarSettingsSection.kt)
- [x] 音频录制/播放基础组件
- [x] 小游戏界面框架 (EchoSpeak / SoundGuess / ColorShape)
- [x] AI 服务接口抽象 (AIService + AIServiceImpl)
- [x] Chat 流式/非流式 API 调用
- [x] STT / TTS API 接口定义
- [x] 国内 AI 模型预设列表

---

## 🚧 待开发 / 需要完善

### 高优先级
- [ ] **AI 聊天功能联调:** ChatViewModel 中调用 AIService 发送消息并显示回复（需用户填写 API Key）
- [ ] **实时语音对话 (AlwaysOnVoiceUseCase):** STT → AI → TTS 全链路打通
- [ ] **小游戏逻辑实现:** EchoSpeakGame / SoundGuessGame / ColorShapeGame 的交互逻辑
- [ ] **API Key 安全存储:** 当前明文存储在 Room 中，建议用 EncryptedSharedPreferences

### 中优先级
- [ ] **故事播放功能:** 从 stories.json 加载故事，TTS 播报 + 翻页
- [ ] **头像系统:** 像素表情动画切换、自定义照片裁剪
- [ ] **家长控制:** 使用时长限制、内容过滤
- [ ] **多语言支持:** 英文界面
- [ ] **离线模式:** 部分故事/游戏可离线使用

### 低优先级 / 未来规划
- [ ] **UI 主题切换:** 在设置页让用户切换 Candy Land / Starry Fairytale 三套主题
- [ ] **动画 & 音效:** 更丰富的 Lottie 动画、触摸反馈音效
- [ ] **儿童安全:** 内容审核中间层、敏感词过滤
- [ ] **Google Play 上架准备:** 隐私政策、家长网关

---

## 🔑 使用指引（给下一台电脑的 AI）

当你打开这个项目继续开发时，请先：

1. **阅读本文件 (AGENTS.md)** — 了解项目全貌
2. **在浏览器打开 `index.html`** — 查看三套 UI 风格的视觉设计
3. **打开 `pixel-buddy/` 项目** — Android Studio 或命令行查看代码
4. **查看关键文件了解架构:**
   - `MainActivity.kt` — 入口和导航
   - `Models.kt` — 所有数据模型
   - `AIService.kt` — AI 服务接口
   - `AppModule.kt` — 依赖注入
   - `PixelBuddyTheme.kt` — 主题配色
5. **运行 `git pull`** 确保获取最新代码
6. **从 TODO 列表选一个待开发项**开始工作

---

## ⚙️ 环境要求

| 工具 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1.1) 或更新 |
| JDK | 17 |
| Gradle | 8.5 |
| Kotlin | 1.9.22 |
| Android SDK | 36 (Android 16) |
| 最低 API | 26 (Android 8.0) |
