# Simple-Drive — 简易网盘

基于 **Kotlin + Jetpack Compose (Material 3)** 的 Android 云存储管理 App。支持文件浏览、上传、多选、移动、重命名、删除、分享（DeepLink）、TXT 分页阅读、文件分类筛选等功能。

---

## 运行环境

| 项目 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1) 及以上 |
| Kotlin | 1.9+ |
| Gradle | 8.7+ |
| compileSdk | 36 |
| minSdk | 24 (Android 7.0) |
| targetSdk | 36 |

## 环境搭建

```bash
# 1. 克隆仓库
git clone https://github.com/eeee385/Simple-Drive.git
cd Simple-Drive

# 2. 用 Android Studio 打开项目根目录

# 3. Sync Gradle，等待依赖下载完成

# 4. 运行
./gradlew :app:installDebug
```

> **注意**：首次启动 App 时，若文件列表为空，点击 **「加载示例数据」** 按钮即可载入模拟文件数据（JSON → Room 数据库）。

## 项目启动

1. 用 Android Studio 打开项目根目录
2. 选择 Run Configuration 为 `app`
3. 连接 Android 设备或启动模拟器
4. 点击 Run（或 `Shift+F10`）

---

## 架构

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt          # 主入口，Tab 管理，DeepLink 处理
├── SimplePanApplication.kt  # Application，全局依赖管理
│
├── domain/                  # 领域层（业务逻辑）
│   ├── model/               #   UserInfo, FileCategory, FilterType
│   └── service/             #   FileService（排序、筛选）
│
├── data/                    # 数据层
│   ├── local/
│   │   ├── db/              #     Room 数据库 & DAO
│   │   ├── entity/          #     实体（FileEntity, ShareLinkEntity 等）
│   │   └── model/           #     DTO（FileDto）
│   └── repository/          #   数据仓库（File, User, Share）
│
├── ui/                      # 表现层
│   ├── theme/               #   色彩、主题（天空蓝 + 暖金）
│   ├── navigation/          #   路由定义 & NavHost
│   ├── components/          #   可复用组件（FileListItem, EmptyState, LoadingOverlay）
│   └── screens/
│       ├── pan/             #    网盘 Tab（首页、分享预览、最近浏览/转存）
│       ├── files/           #    文件 Tab（浏览、多选、移动）
│       └── reader/          #    TXT 阅读器
│
└── util/                    # 工具类
    ├── FileTypeHelper.kt    #   文件图标映射、大小格式化、类型识别
    ├── FileOpener.kt        #   系统播放器调用
    ├── TimeUtils.kt         #   相对时间格式化
    └── InitialDataLoader.kt #   首次启动数据初始化
```

### 技术栈

| 模块 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose + Material 3 |
| 本地数据库 | Room (SQLite) |
| 导航 | Navigation Compose |
| 异步 | Kotlin Coroutines + Flow |
| 序列化 | kotlinx.serialization |
| 构建 | Kotlin DSL (Gradle) |

---

## 核心功能

### 1. 文件管理

- **浏览**：按文件夹层级浏览文件，支持下拉刷新加载模拟数据
- **上传**：通过系统文件选择器上传文件到当前文件夹
- **新建文件夹**：在当前目录创建子文件夹
- **重命名**：修改文件或文件夹名称
- **移动**：底部弹出文件夹选择器，选择目标目录后移动
- **删除**：递归删除文件或文件夹（含确认弹窗）
- **多选操作**：长按进入多选模式，支持批量移动/删除/分享

### 2. 文件分类筛选

文件 Tab 根目录顶部提供分段筛选控件：全部 / 图片 / 视频 / 文档。文档分类包含 txt、pdf、ppt、代码文件等，使用排除法（非媒体、非二进制即为文档）自动扩展。

### 3. TXT 阅读器

- **编码检测**：自动识别 UTF-8 / GBK 编码
- **分页渲染**：基于 `StaticLayout` 实现精确分页，`HorizontalPager` 左右滑动翻页
- **进度追踪**：顶部进度条 + 当前页/总页数显示
- **字号适配**：18sp 正文字号，28sp 行高，动态适配屏幕尺寸

### 4. 分享功能

- 支持**多文件分享**：选择多个文件后生成一个分享链接（同一 shareId 关联多个文件）
- **DeepLink**：通过链接 `simplepan://share?sid=xxx` 直接唤起 App 并打开分享预览
- **剪贴板检测**：App 回到前台时自动检测剪贴板中的分享链接
- **分享预览**：展示文件树，支持展开文件夹查看，确认后转存到个人网盘

### 5. 最近记录

网盘 Tab 显示最近浏览和最近转存记录，点击可快速跳转到对应文件。

---

## 数据库设计

| 表名 | 说明 |
|------|------|
| `files` | 文件/文件夹（fileId, name, size, type, path, parentId, timestamp, contentUri） |
| `recent_browses` | 最近浏览记录（fileId, browseTime） |
| `recent_transfers` | 最近转存记录（fileId, transferTime） |
| `share_links` | 分享链接（复合主键 shareId + fileId, createdAt） |

### 数据流

```
assets/files.json  →  kotlinx.serialization 解码
  →  FileDto  →  flattenDtos() 扁平化  →  FileEntity
  →  Room DAO.insertFiles()  →  SQLite 存储
  →  FileRepository Flow 查询  →  ViewModel  →  Compose UI
```

模拟网络延迟：`syncFromMockData()` 内含 `delay(500)` 模拟网络请求。

---

## 页面路由

| 路由 | 说明 |
|------|------|
| `pan` | 网盘首页（用户卡片 + 最近记录） |
| `files` | 文件根目录 |
| `file_list/{parentId}` | 通过 DeepLink 进入子文件夹 |
| `reader/{fileId}` | TXT 阅读器 |
| `recent_list/{listType}` | 最近浏览 / 最近转存完整列表 |
| `folder_picker/{parentId}` | 移动文件夹选择器（底部弹窗） |
| `share_preview/{shareId}` | 分享预览页 |
| `empty` | 空占位路由（NavHost 始终挂载用） |

---

## 视觉设计

采用**天空蓝 + 暖金**主题：
- 主色 `#0284C7`、强调色 `#F59E0B`
- 自定义文件类型图标（12 种 Vector Drawable）
- iOS 风格圆弧弹窗和分段控件
- 底部操作栏圆弧药丸按钮

---

## 构建

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK（未签名）
./gradlew :app:assembleRelease

# Lint 检查
./gradlew :app:lint
```

---

## 许可证

MIT License
