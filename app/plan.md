好的，我们来把整个项目拆解成可执行的步骤。我会默认你选择 **Android + Kotlin + Jetpack Compose** 实现，因为这是目前主流且与要求最契合的方案。下面是一份从零开始的完整执行规划，你可以照着一步步推进。

---

## 一、总体阶段与时间建议（共约 3 周）

| 阶段                    | 核心目标                                                     | 预计耗时 |
| ----------------------- | ------------------------------------------------------------ | -------- |
| 1. 环境搭建与架构初始化 | 创建 Compose 工程、引入 Room/协程/Navigation 等依赖，搭建分层骨架 | 1~2 天   |
| 2. 数据层实现           | 设计数据库表，编写 DAO，实现模拟网络数据解析（JSON -> Entity），异步操作 | 2~3 天   |
| 3. 文件 Tab 基础功能    | 文件列表、文件夹导航、文件图标、视频系统播放器、txt 阅读器（分页） | 3~4 天   |
| 4. 网盘 Tab             | 个人信息（剩余空间）、最近浏览列表、最近转存列表             | 1~2 天   |
| 5. 文件管理操作         | 上传（系统文件选择器 → 入库）、删除、移动、重命名            | 2~3 天   |
| 6. 分享与 DeepLink      | 生成分享短链、点击链接唤醒 App 并跳转到文件列表页            | 1~2 天   |
| 7. 集成与完善           | 边界处理、异步优化、测试用例编写、UI 打磨                    | 2~3 天   |
| 8. 交付物准备           | 录屏、APK、README、技术文档（类图/流程图/分页算法/DeepLink）、测试用例 | 2 天     |

---

## 二、技术选型与关键依赖

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **导航**：Compose Navigation（支持 DeepLink）
- **数据库**：Room（SQLite 封装）
- **异步**：Kotlin Coroutines + Flow
- **JSON 解析**：Kotlinx.serialization 或 Gson/Moshi
- **文件操作**：Android Storage Access Framework (SAF) / MediaStore
- **视频播放**：系统 Intent 调起外部播放器
- **DeepLink**：App Links（推荐）或自定义 scheme（如 `simplepan://`）
- **模拟网络**：定义 JSON 字符串/本地 JSON 文件，解析后存入 Room，用 `delay` 模拟网络延迟

> 如果需要拓展项（如广告），可选集优量汇或穿山甲 SDK，但非必须。

---

## 三、详细实施步骤

### 阶段1：环境搭建与架构初始化

1. **创建 Android 项目**  
   - 模板：Empty Compose Activity  
   - Minimum SDK：24 或 26  
   - 语言：Kotlin

2. **引入依赖** (`libs.versions.toml` 或直接 build.gradle)  
   ```kotlin
   // Compose BOM
   implementation(platform("androidx.compose:compose-bom:2024.02.00"))
   implementation("androidx.compose.ui:ui")
   implementation("androidx.compose.material3:material3")
   implementation("androidx.navigation:navigation-compose:2.7.7")
   
   // Room
   implementation("androidx.room:room-runtime:2.6.1")
   ksp("androidx.room:room-compiler:2.6.1")
   implementation("androidx.room:room-ktx:2.6.1")
   
   // Coroutines
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
   
   // Serialization
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
   ```

3. **搭建分层目录结构**  
   ```
   com.your.package
   ├── data
   │   ├── local
   │   │   ├── db/          // Room Database, DAO, Entities
   │   │   └── model/       // 数据库实体 & DTO
   │   └── repository       // 数据仓库实现
   ├── domain
   │   └── model            // 业务模型（可选，简单项目可直接用 Entity）
   ├── ui
   │   ├── navigation       // 导航图 & DeepLink 配置
   │   ├── screents
   │   │   ├── pan          // 网盘 Tab
   │   │   ├── files        // 文件 Tab
   │   │   └── reader       // txt 阅读器
   │   └── components       // 公共 Composable
   └── util                 // 工具类
   ```

4. **设置底部导航与页面框架**  
   - 两个 Tab：网盘、文件  
   - 使用 `Scaffold` + `NavigationBar` 实现  
   - 把 `NavHost` 放在 `Scaffold` 的 `content` 中

---

### 阶段2：数据层实现

#### 2.1 数据库设计

Entity：`FileEntity`

```kotlin
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val fileId: String,  // UUID
    val name: String,
    val size: Long,                  // bytes
    val path: String,                // 模拟的存储路径
    val type: String,                // "folder", "video", "txt", "other"
    val parentId: String?,           // 父文件夹ID，null 为根目录
    val timestamp: Long              // 修改/创建时间
)
```

额外需要两个表记录最近浏览和最近转存（也可以直接用 `FileEntity` + 类型标记，但要求顺序所以单独存更方便）：

```kotlin
@Entity(tableName = "recent_browse")
data class RecentBrowse(
    @PrimaryKey val id: Long,  // 自增，或者用 fileId 并覆盖时间
    val fileId: String,
    val browseTime: Long
)

@Entity(tableName = "recent_transfer")
data class RecentTransfer(
    @PrimaryKey val id: Long,
    val fileId: String,
    val transferTime: Long
)
```

DAO 需要提供：
- 根据 parentId 查询子文件列表
- 插入/更新文件
- 删除文件
- 查询最近浏览（按时间倒序，join FileEntity）
- 查询最近转存（同上）
- 批量插入文件（初始化数据）

#### 2.2 模拟网络数据解析

1. 在 `assets/` 下放一个 `files.json`，内容模拟网盘文件结构，例如：
   ```json
   [
     {
       "fileId": "1",
       "name": "文档",
       "type": "folder",
       "parentId": null,
       "children": [
         {"fileId":"2","name":"笔记.txt","type":"txt","size":1024,"path":"/文档/笔记.txt","parentId":"1","timestamp":1710000000}
       ]
     }
   ]
   ```
2. 编写解析器，递归将 JSON 转为 `FileEntity` 列表。
3. 在 Repository 中提供 `syncFiles()` 方法：  
   - 从 assets 读取 JSON 字符串  
   - 用 `delay(500)` 模拟网络请求  
   - 解析为实体列表  
   - 在协程 Dispatchers.IO 中插入数据库  
   - 通过 Flow 或 LiveData 暴露数据给 UI

#### 2.3 异步处理

所有数据库操作（增删改查）均封装在 Repository 中，使用 `withContext(Dispatchers.IO)`，UI 通过 `viewModelScope.launch` 调用，确保主线程不卡顿。

---

### 阶段3：文件 Tab 基础功能

1. **文件列表页** (`FileListScreen`)  
   - 顶部展示当前文件夹路径（可逐级返回根目录）
   - 使用 `LazyColumn` 展示文件列表
   - 每个条目显示图标（文件夹/视频/txt）、文件名、大小
   - 点击：
     - 文件夹 → 进入该文件夹列表（传递 parentId）
     - 视频文件 → 调用 `Intent.ACTION_VIEW` + 文件 URI（如果是模拟文件，可先让用户选择一个本地视频用于演示；若完全模拟，可展示“打开系统播放器”按钮并捕获事件）
     - txt 文件 → 导航至文本阅读器页面

2. **文件夹导航**  
   - 维护一个 `parentId` 的栈，或采用 Navigation 参数传递。
   - 返回上级时弹出栈顶。

3. **视频播放**  
   - 由于文件是模拟的，实际测试时可让用户先通过“上传”功能上传一个真实视频，然后点击打开。
   - 获取文件真实路径或 content URI，构造 `Intent` 调用系统播放器。

---

### 阶段4：网盘 Tab

1. **个人信息 & 剩余空间**  
   - 在网盘 Tab 顶部展示用户名、头像（可用占位）、剩余空间（如 10GB，本地统计所有文件总 size 然后与一个固定总容量做差）。
   - 数据可以从 SharedPreferences 或一个模拟的用户信息 JSON 中获取。

2. **最近浏览**  
   - 查询 `recent_browse` 表 join `files`，按时间倒序显示最近 N 条。
   - 每次用户打开一个文件（txt、视频）时，记录一条浏览记录（更新或插入）。

3. **最近转存**  
   - 查询 `recent_transfer`，逻辑同上。
   - 转存记录产生于“上传”或“移动/复制文件时标记为转存”（定义好触发条件，如从其他目录移动文件到当前目录）。

4. 两个列表均支持点击跳转到对应文件位置（文件夹则进入，txt 进入阅读器）。

---

### 阶段5：文件管理操作

1. **上传文件**  
   - 使用 `ActivityResultContracts.GetContent()` 或 `OpenDocument` 打开系统文件选择器。
   - 获取选中文档的 URI，复制文件到应用内部缓存目录（或直接记录 URI，但需权限持久化）。
   - 将文件信息（名称、大小、类型、路径）构造为 `FileEntity`，插入 Room，同时加入最近转存记录。
   - 要求“使用数据库”，因此上传信息必须入库。

2. **删除文件**  
   - 长按文件弹出菜单，选择删除。
   - 从数据库中删除对应行，如果为文件夹则递归删除子文件。
   - UI 自动更新。

3. **移动文件**  
   - 提供一个“移动到”对话框，展示所有文件夹列表供选择。
   - 更新文件的 `parentId`。

4. **重命名**  
   - 弹出修改对话框，更新 `name` 字段。

---

### 阶段6：分享与 DeepLink

#### 6.1 分享链接生成

- 在文件列表给某个文件（或文件夹）提供“分享”按钮。
- 生成一段分享文本，包含一个 DeepLink URL，例如 `https://yourdomain.com/share?folder_id=xxx`。
- 要求链接**不包含直接的文件明文信息**，因此不能把文件名等放在 URL 里。可以用 `folder_id` 或加密后的 token。简化实现：用 Base64 编码的 JSON 或一个随机 shareId，本地数据库新增一个 `share_links` 表，存储 shareId 与 `folder_id` 的映射关系。
- 用户点击复制链接，调用系统剪贴板。

#### 6.2 接收链接并打开 App

- 在 `AndroidManifest.xml` 中配置 intent-filter 支持 App Links (建议用自定义 scheme 快速实现，例如 `simplepan://share?sid=xxx`)。
- 当用户点击链接打开 App 时，`MainActivity` 的 `onNewIntent` 或 Navigation 的 deepLink 解析参数。
- 提取 `sid`，去本地数据库查 `share_links` 得到 `folder_id`，然后导航到该文件夹的文件列表页。

#### 6.3 测试方法

- 用 `adb shell am start -a android.intent.action.VIEW -d "simplepan://share?sid=abc123"` 模拟链接点击。
- 验证能否正确跳转。

---

### 阶段7：TXT 文档阅读器（重点）

这是核心功能，需要专门设计。

1. **页面设计**  
   - 全屏阅读界面，半透明顶部工具栏（返回、文件名）。
   - 中间区域显示当前页文本。
   - 左右滑动使用 HorizontalPager 或 Swipe 手势实现翻页。

2. **分页算法**  
   - 加载 txt 文件内容为字符串。
   - 使用 `TextPaint` 和屏幕宽度/高度，减去边距后计算每页能显示的最大字符行数和每行字符数。
   - 核心步骤：
     - 获取屏幕可用宽度 `viewWidth`、高度 `viewHeight`，定义文字大小 `textSize`、行间距。
     - 创建 `StaticLayout`（非 Compose 的 `TextMeasurer` 也可）来测量文本。
     - 遍历文本，每次塞入能容纳的字符，形成一个页面（`List<CharSequence>` 或保存起始结束索引）。
     - 维护当前页码 `currentPage`。
   - Compose 中可利用 `rememberTextMeasurer()` 和 `TextLayoutResult`，配合容器大小计算断点，实现纯 Compose 分页。
   - 具体方法：使用 `TextMeasurer.measure()` 逐行/逐块测试填充，直到高度超出页面可用高度，记录截断位置。
   - 可以保存一个 `List<IntRange>` 每个元素表示一页在原文本中的起止索引。

3. **左右切换**  
   - 使用 `HorizontalPager` 配合 `currentPage` 状态。
   - 或手动处理滑动手势（pointerInput）实现翻页动画，推荐 `HorizontalPager` 更省事。

4. **广告插入（拓展项可选）**  
   - 在翻页时，每 N 页插入一次广告占位，显示广告 View（用 AndroidView 嵌入 Compose 原生广告 View）。  
   - 非强制，可先不做。

---

### 阶段8：集成测试与 UI 打磨

- 确认所有异步操作通过 `collectAsStateWithLifecycle()` 安全收集。
- 数据库操作无内存泄漏，使用 `ViewModel` + `Repository` 模式。
- 处理空状态（没有文件时显示提示）。
- 添加加载动画（模拟网络请求时用 CircularProgressIndicator）。

---

## 四、交付物准备指南

### 1. 工程源码（GitHub 仓库）
- 保持清楚的提交历史（如 feat: 添加文件列表、feat: 实现分页阅读器等）
- `README.md` 必须包含：
  - 项目简介
  - 开发环境（Android Studio 版本、JDK、Gradle 版本）
  - 如何构建运行（`./gradlew assembleDebug`）
  - 架构说明
  - DeepLink 测试方法
  - 使用到的开源库列表

### 2. 演示录屏 (3-5分钟)
建议路径：
- 启动 → 看到网盘 Tab（个人信息，剩余空间）→ 最近浏览/转存为空
- 切换到文件 Tab → 初始文件列表（模拟数据）→ 进入文件夹 → 打开一个 txt 文件 → 左右翻页（展示分页效果）→ 返回
- 点击视频文件 → 调用系统播放器
- 上传一个新文件 → 查看它出现在文件列表和最近转存
- 长按删除/移动/重命名文件
- 分享某文件夹 → 复制链接 → 退出 App，通过链接打开 App → 直接定位到该文件夹
- 回到网盘 Tab 查看最近浏览/转存列表已经更新

### 3. 技术文档
重点写：
- **TXT 分页算法**：流程图 + 计算逻辑（屏幕尺寸适配、断行方式）
- **DeepLink 实现**：uri scheme、映射表、Activity 处理流程、安全考虑
- **难点攻克**：如静态文本分页在 Compose 中的实现细节、文件夹递归删除的事务性、模拟网络与数据库的同步等
- **UML 类图**（推荐 PlantUML 或 draw.io）：展示 Entity、DAO、Database、Repository、ViewModel 的关系
- **核心流程图**：文件上传、分享、打开阅读器的主要流程

### 4. 测试用例集（最小功能验证）
写一个清晰的 checklist，供评委操作：
1. 安装 APK 后启动，底部两个 Tab 可切换
2. 网盘页能看到剩余空间和最近列表（初始为空或模拟几条）
3. 文件页显示根目录文件和文件夹
4. 点击文件夹可进入，并能返回
5. 点击 txt 文件进入阅读器，内容显示正常，能左右滑动翻页，页码/进度有变化
6. 点击视频文件能唤起系统播放器
7. 上传一张图片或一个文档，文件列表中出现它
8. 长按文件可删除、重命名、移动
9. 分享文件夹生成链接，通过链接唤起 App 并跳转到该文件夹
10. 网盘页的最近浏览/转存列表与操作对应更新

---

## 五、AI 辅助建议

你可以用 AI 工具生成样板代码（如 Room 初始化、Compose 列表、分页算法框架），但必须做到：
- 理解每一段代码，能解释为什么这么写。
- 分页算法部分建议先自己尝试设计，再用 AI 辅助优化边缘情况。
- DeepLink 配置中的 intent-filter 若复制网上代码，务必验证兼容性。

---

## 六、常见坑点提醒

1. **Room 与 Compose 的 Flow 结合**：需要在 ViewModel 中使用 `LiveData/Flow` 并用 `collectAsStateWithLifecycle()` 收集，防止生命周期问题。
2. **文件选择器获取的 URI 临时权限**：上传后应立即拷贝到应用内部存储或使用 `takePersistableUriPermission()`，否则重启后无法访问。
3. **分页计算**：注意中英文混排、标点换行规则；`StaticLayout` 的 `breakStrategy` 参数设置为 `BREAK_STRATEGY_HIGH_QUALITY` 可提高体验。
4. **DeepLink 验证**：自定义 scheme 在 Chrome 上可能被拦截，测试时可以用 `adb` 或放置一个网页链接点击。App Links 需要域名验证更复杂，项目中用自定义 scheme 即可满足要求。
5. **模拟网络**：一定要用 `delay` 并显示 loading 状态，否则看起来不像网络操作。

---

按照这个规划，你就能有条不紊地完成项目，并且在每个阶段都有清晰的产出。如果在具体编码中遇到任何技术难点，欢迎继续提问，我可以帮你细化算法或提供代码示例。祝你项目顺利！