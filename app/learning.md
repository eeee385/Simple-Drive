# 简易网盘 — 技术总结与答辩准备

## 一、已使用的技术栈详解

### 1. Kotlin + Jetpack Compose（UI 框架）

**什么是 Compose？**
- Google 推出的声明式 UI 框架，用 Kotlin 函数描述界面，"状态驱动 UI"
- 当你修改一个 `MutableState` 变量，所有用到它的 UI 自动重绘（重组/Recomposition）

**项目中如何体现？**
```kotlin
// MainActivity.kt — 一个状态变量驱动整个 UI
var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
// 当 selectedIndex 变化，底部导航栏和内容区自动刷新
```

**关键概念：**
- `@Composable`：标记 UI 函数，只能在 Composable 函数中调用
- `remember`：在重组过程中保留变量值
- `rememberSaveable`：不仅保留变量值，旋转屏幕也能恢复（存 Bundle）
- `mutableStateOf`：创建一个可观察的状态，值变化时触发重组
- `mutableStateListOf`：同上，但用于列表，增删元素也能触发重组
- `LaunchedEffect(key)`：在 Composable 进入组合时启动协程，key 变化时重新执行
- `collectAsState()`：将 Flow 转为 Compose 可观察的 State

**可能被问到：**
- **Q: 声明式 UI 和传统 XML 布局有什么区别？**
  > 传统方式：findViewById → setText，手动更新每个控件。声明式：描述"这个 Text 显示 $count"，变量变化时框架自动更新。

- **Q: `remember` 和 `rememberSaveable` 的区别？**
  > `remember`：配置变更（旋转屏幕）后状态丢失。`rememberSaveable`：自动保存到 Bundle，旋转后恢复。项目中 `pendingMoveIds` 用了 `rememberSaveable`，因为导航跨页面时需要保留。

- **Q: 为什么 `mutableStateListOf` 很重要？**
  > 项目中 `FolderPickerScreen` 的 `nameStack` 从 `mutableListOf` 改为 `mutableStateListOf`，因为普通 List 的增删不会触发 UI 重组，路径面包屑不会更新。

---

### 2. Room（本地数据库）

**什么是 Room？**
- Android 官方 SQLite 封装库，通过注解自动生成数据库代码
- 三大组件：`@Entity`（表）、`@Dao`（操作接口）、`@Database`（数据库实例）

**项目中的体现：**
```
FileEntity (files表) ← FileDao (CRUD) ← AppDatabase (单例) ← FileRepository (封装)
```

**关键设计点：**

| 设计 | 代码位置 | 为什么这么写 |
|------|---------|------------|
| Flow 响应式查询 | `FileDao.getFilesByParentId` 返回 `Flow<List<FileEntity>>` | 数据库一变化，UI 自动刷新，不需要手动通知 |
| `flatMapLatest` 链式查询 | `FilesViewModel.files` | 当用户进入子文件夹（`parentId` 变化），自动切换到新查询，同时取消旧查询避免浪费 |
| `OnConflictStrategy.REPLACE` | 上传/同步时的 `@Insert` | 避免重复插入报错，自动覆盖已有记录 |
| 唯一索引 + upsert | `RecentBrowseEntity` 的 `indices = [Index(value = ["fileId"], unique = true)]` | 同一文件浏览两次只更新 `browseTime`，不产生重复行（INSERT OR REPLACE） |
| KSP 编译期代码生成 | `ksp(libs.androidx.room.compiler)` | 编译时根据 `@Dao` 接口自动生成实现类，`@Query` 的 SQL 在编译期校验，写错表名编译直接报错 |
| 单例模式 | `AppDatabase.getInstance()` 使用 `synchronized` + `@Volatile` | 双重检查锁定，保证全 App 只有一个数据库连接 |

**可能被问到：**
- **Q: 为什么 DAO 方法有些返回 `Flow`，有些是 `suspend`？**
  > `Flow`：用于需要"自动刷新"的场景（如文件列表）。`suspend`：用于一次性操作（如插入、删除），调用完成后不需要持续监听。

- **Q: KSP 是什么？和 KAPT 的区别？**
  > KSP（Kotlin Symbol Processing）是 Kotlin 专用的注解处理器，比 KAPT（基于 Java APT）快 2 倍。Room 用 KSP 在编译时生成 `_Impl` 类。

- **Q: Room 如何保证线程安全？**
  > Room 本身不保证线程安全。项目中所有数据库操作都在 `withContext(Dispatchers.IO)` 中执行，避免主线程操作数据库崩溃。

---

### 3. Navigation Compose（页面导航 + DeepLink）

**核心概念：**
- `NavHost`：管理所有页面的容器
- `NavController`：控制页面跳转
- `route`：每个页面的 URL 路径，支持参数如 `"reader/{fileId}"`
- `navArgument`：声明路由参数类型

**项目中的路由表：**
```
"pan"                        → PanScreen（网盘Tab）
"files"                      → FilesScreen（文件Tab）
"file_list/{parentId}"       → 进入文件夹
"reader/{fileId}"            → TXT阅读器（占位）
"recent_list/{listType}"     → 全量最近列表
"folder_picker/{parentId}"   → 移动目标选择器
```

**DeepLink 机制（share 功能）：**
```
用户点击链接: simplepan://share?sid=abc12345
    ↓
AndroidManifest intent-filter 匹配 simplepan://share
    ↓
App 启动 → NavHost 解析 sid
    ↓
查 share_links 表 → 得到 fileId → 导航到文件夹
```

**可能被问到：**
- **Q: `launchSingleTop = true` 和 `restoreState = true` 的作用？**
  > `launchSingleTop`：栈顶已有该页面时不创建新实例，避免重复页面。`restoreState`：恢复之前保存的页面状态。

- **Q: `BackHandler` 是什么？**
  > 拦截系统返回键。项目中在 `FilesScreen` 和 `FolderPickerScreen` 中用它：进入子文件夹后按返回键先回到上级目录，而不是退出整个页面。

- **Q: `savedStateHandle` 的用途？**
  > 在页面间传递数据。项目中用它在 FolderPicker 和 FilesScreen 之间传递"移动目标文件夹 ID"。

- **Q: 跨页面传递结果有哪些方式？**
  > 方式 1：`savedStateHandle`（当前使用）。方式 2：共享 ViewModel。方式 3：回调 lambda。项目中踩过坑——`remember` 变量导航后丢失，改用 `rememberSaveable` 才解决。

---

### 4. Kotlin Coroutines + Flow（异步处理）

**核心概念：**
- `viewModelScope.launch`：在 ViewModel 中启动协程，ViewModel 清除时自动取消（防内存泄漏）
- `withContext(Dispatchers.IO)`：把数据库/文件操作切到 IO 线程池
- `StateFlow`：有初始值的热流，UI 通过 `collectAsState()` 订阅
- `flatMapLatest`：上游 Flow 发射新值时，取消旧的内部 Flow、切换到新的

**项目中的异步流程（上传文件为例）：**
```
用户选择文件
  → viewModelScope.launch 启动协程
    → Dispatchers.IO:
        ContentResolver.query() → 读文件名和大小
        openInputStream() → 读文件内容
        FileOutputStream → 拷贝到内部存储
        创建 FileEntity → fileDao.insertFile() 入库
        recordTransfer() → 记录到最近转存
  → isLoading.value = false
  → Snackbar 提示"已上传"
```

**可能被问到：**
- **Q: `Dispatchers.IO` 和 `Dispatchers.Main` 的区别？**
  > IO：后台线程池，用于文件读写、数据库操作。Main：Android 主线程，用于更新 UI。所有数据库操作必须在 IO 线程，否则 ANR。

- **Q: `delay(500)` 在项目中是什么用途？**
  > 模拟网络请求延迟。在 `syncFromMockData` 中，need.md 要求"模拟网络请求，需要有数据解析"。

- **Q: `flatMapLatest` 和 `map` 的区别？**
  > `map`：一对一转换。`flatMapLatest`：上游每次变化时，内部 Flow 会取消旧值、切换到新值。项目中 `files` 用 `flatMapLatest` 是因为 `parentId` 变化时需要切换到新文件夹的查询。

- **Q: `SharingStarted.WhileSubscribed(5000)` 是什么意思？**
  > `stateIn` 的参数。含义：最后一个订阅者取消后，5 秒内还没有新订阅者就停止上游 Flow。避免后台无意义消耗。

---

### 5. 存储访问框架 (SAF) + FileProvider

**上传流程：**
```
ActivityResultContracts.GetContent()  → 系统文件选择器
  → ContentResolver.query() → 获取文件名(DISPLAY_NAME)和大小(SIZE)
  → openInputStream() → 读取文件内容
  → FileOutputStream → 写入 app 内部存储 (filesDir/uploads/)
  → FileEntity 入库
```

**FileProvider（调用系统播放器）：**
```
File(context.filesDir, "uploads/xxx.mp4")     → 内部存储文件
  → FileProvider.getUriForFile()              → content:// URI（安全共享）
  → Intent.ACTION_VIEW + mimeType "video/*"   → 系统播放器打开
  → FLAG_GRANT_READ_URI_PERMISSION            → 临时授权
```

**可能被问到：**
- **Q: 为什么不直接用 `file://` URI？**
  > Android 7.0+ 禁用了 `file://` URI（抛 `FileUriExposedException`），必须通过 FileProvider 生成 `content://` URI 来安全共享文件给其他 App。

- **Q: `FLAG_GRANT_READ_URI_PERMISSION` 是什么？**
  > 临时授权给接收方（系统播放器）读取这个文件的权限，接收方关闭文件后权限自动回收。

- **Q: 为什么需要把文件拷贝到内部存储？**
  > 系统文件选择器返回的 URI 是临时权限，App 关闭后可能无法再访问。拷贝到 `filesDir/` 后 App 永久拥有该文件。

- **Q: `openFileOutput` 有什么限制？**
  > 不支持路径分隔符（如 `"uploads/file.txt"`）。项目中最初用了这个导致上传静默失败，改用了 `File.mkdirs()` + `FileOutputStream`。

---

### 6. kotlinx-serialization（JSON 解析）

**模拟网络数据流程：**
```
assets/files.json（模拟服务器返回的 JSON）
  → Json.decodeFromString<List<FileDto>>()
  → flattenDtos() 递归展平层级结构
  → List<FileEntity>
  → Room 批量 insert (OnConflictStrategy.REPLACE)
```

**为什么选 kotlinx-serialization 而不是 Gson？**
- Kotlin 原生支持，编译期生成代码（不用反射），性能更好
- `@Serializable` 注解即可，不需要额外的 TypeToken

**可能被问到：**
- **Q: `@Serializable` 注解的作用？**
  > 告诉 Kotlin 编译器为这个类自动生成序列化/反序列化代码。`FileDto` 的 `children: List<FileDto>?` 字段支持递归结构。

- **Q: `ignoreUnknownKeys = true` 是什么？**
  > 如果 JSON 中有 Kotlin 类未定义的字段，不会报错而是忽略。提高容错性。

---

## 二、架构设计

```
┌─────────────────────────────────────────┐
│ UI 层 (Compose Screens)                 │
│ PanScreen, FilesScreen, FolderPicker... │
│ collectAsState() ← 订阅 ViewModel       │
├─────────────────────────────────────────┤
│ ViewModel 层                            │
│ PanViewModel, FilesViewModel            │
│ StateFlow 暴露状态, viewModelScope 协程  │
├─────────────────────────────────────────┤
│ Repository 层                           │
│ FileRepository, UserRepository, ShareRepo│
│ 封装 DAO, withContext(IO), delay模拟网络 │
├─────────────────────────────────────────┤
│ Data 层 (Room + Assets)                 │
│ AppDatabase → 4 DAOs → 4 Entities       │
│ assets/files.json → mock 模拟数据       │
└─────────────────────────────────────────┘
```

**分层职责总结：**

| 层 | 职责 | 不允许做的事 |
|----|------|-------------|
| UI (Screen) | 渲染界面、处理用户交互 | 直接访问数据库、启动协程 |
| ViewModel | 持有 UI 状态、编排业务逻辑 | 持有 Context 引用（泄漏风险） |
| Repository | 封装数据来源、线程切换 | 持有 UI 状态 |
| Data (Room DAO) | SQL 操作、数据持久化 | 包含业务逻辑 |

**可能被问到：**
- **Q: 为什么需要 Repository 层？不能直接从 ViewModel 调 DAO 吗？**
  > 解耦。如果以后接真实服务器（Retrofit），只需改 Repository 实现，ViewModel 和 UI 不改。而且 Repository 封装了"数据从哪来"（本地 / mock / 网络）的逻辑。

- **Q: `SimplePanApplication` 的作用？**
  > 作为全局单例持有 `AppDatabase` 和 Repository 实例，避免每个页面各自创建数据库连接（浪费资源）。通过 `LocalContext.current.applicationContext as SimplePanApplication` 在任意 Composable 中获取。

- **Q: ViewModel 怎么创建？`ViewModelProvider.Factory` 是干嘛的？**
  > ViewModel 默认只能无参构造。当 ViewModel 需要参数（如 `FileRepository`）时，必须自定义 Factory。项目中每个 ViewModel 都有一个 `class Factory : ViewModelProvider.Factory`，在 `create()` 中传入依赖。

---

## 三、老师可能问的问题（按 need.md 分类）

### UI 与架构

| 问题 | 回答要点 |
|------|---------|
| "为什么选 Compose 而不是 XML？" | need.md 要求"必须使用 Compose"；声明式 UI 开发效率高；状态管理比 findViewById 简洁 |
| "MVVM 三层分别做什么？" | View 负责显示和交互，ViewModel 持有 UI 状态并处理业务逻辑，Model/Repository 负责数据存取 |
| "底部导航怎么实现的？" | `Scaffold(bottomBar = NavigationBar{})` + `NavHost`，`selectedIndex` 追踪当前 Tab |
| "两个 Tab 切换时页面状态怎么保存？" | `saveState = true` + `restoreState = true`，切换 Tab 后回到之前的状态而不是重新创建 |
| "多选模式怎么实现的？" | `MutableStateFlow<Set<String>>` 存储选中文件 ID，长按进入选择模式，点击切换选中/取消 |

### 数据与存储

| 问题 | 回答要点 |
|------|---------|
| "数据库有哪些表？关系是什么？" | `files`（文件元数据）、`recent_browse`（浏览记录→关联 files）、`recent_transfer`（转存记录→关联 files）、`share_links`（分享 token 映射） |
| "模拟网络请求怎么实现的？" | `assets/files.json` → `kotlinx.serialization` 解析 → `delay(500)` 模拟延迟 → Room `insertFiles()` 入库 |
| "文件上传后数据存在哪？" | 文件本体存 `filesDir/uploads/`，元数据（名称、大小、路径、类型）存 Room `files` 表 |
| "异步处理怎么保证不卡 UI？" | `Dispatchers.IO` 处理文件/数据库操作，`StateFlow` + `collectAsState()` 在 UI 线程自动更新 |
| "首次启动初始化怎么做的？" | `InitialDataLoader` 检查 SharedPreferences 标记，首次启动从 assets 加载 mock 数据、刷新用户信息、计算已用空间 |

### 文件操作

| 问题 | 回答要点 |
|------|---------|
| "文件夹删除时子文件怎么处理？" | `deleteFileRecursively()` 先 `DELETE FROM files WHERE parentId = ?` 删子文件，再删自身（事务性） |
| "文件排序规则？" | `fileSortComparator`：文件夹最前(typeOrder=0) → 按类型分组(txt=1, image=2, video=3, audio=4) → 组内按名称 `lowercase()` 排序 |
| "移动目标选择器怎么防止移动到自己？" | 传递 `excludedFolderIds` 参数（待移动的文件夹 ID 集合），在目标列表中用 `filter { it.fileId !in excludedFolderIds }` 过滤 |
| "移动操作的完整流程？" | 长按→选文件→底部栏"移动"→跳转 FolderPickerScreen→选目标文件夹→结果回调→`moveFile()` 更新 parentId→Snackbar 提示 |
| "重名文件怎么处理？" | `@Insert(onConflict = REPLACE)` 自动覆盖同 fileId 的记录。但不同 fileId 的同名文件允许共存 |

### TXT 阅读器（待做，但老师可能问方案）

| 问题 | 回答要点 |
|------|---------|
| "分页算法怎么设计？" | 用 Compose `TextMeasurer.measure()` 测量文本，逐行累加高度直到超过页面可用高度，记录每页起止字符偏移量（`PageInfo(startOffset, endOffset)`） |
| "怎么实现左右滑动翻页？" | 使用 Material3 的 `HorizontalPager`，`pageCount` 为分页总数，每页根据 `currentPage` 显示 `text.substring(start, end)` |
| "中英文混排怎么处理？" | `TextMeasurer` 原生支持，内部调用 `StaticLayout` 的 `BREAK_STRATEGY_HIGH_QUALITY` 自动处理 |

### DeepLink 与分享

| 问题 | 回答要点 |
|------|---------|
| "分享链接为什么不包含文件明文信息？" | 链接只有 `sid=随机8位Base62字符串`，真正的 fileId 存在本地 `share_links` 表。即使链接泄露也看不到文件名/路径。need.md 要求"链接不包含直接的文件明文信息" |
| "DeepLink 怎么配置和处理的？" | `AndroidManifest.xml` 中 `<intent-filter>` 监听 `simplepan://share` scheme，Navigation Compose 通过 `navDeepLink` 或 `savedStateHandle` 传递 sid，查 `share_links` 表得到 fileId 后导航 |
| "自定义 scheme 和 App Links 的区别？" | 自定义 scheme (`simplepan://`)：简单但可能被其他 App 拦截。App Links (`https://`)：需要域名验证，更安全。项目中用自定义 scheme |

### 综合性/开放性

| 问题 | 回答要点 |
|------|---------|
| "遇到的最大难点是什么？怎么解决的？" | 示例回答：Compose Navigation 跨页面回传数据。最初用 `LiveData.observeForever` 结果会丢失，后来改用 `LaunchedEffect(backStackEntryId)` 监听返回事件，配合 `rememberSaveable` 保存待处理数据 |
| "如果接入真实服务器要怎么改？" | 只需改 Repository 层：`syncFromMockData()` → Retrofit HTTP 请求，DAO 和 ViewModel 保持不变 |
| "这个项目还有哪些可以改进的？" | (1) 实现 TXT 分页阅读器 (2) 完善 DeepLink 接收处理 (3) 添加单元测试 (4) 接入广告 SDK (5) 下拉刷新美化 |
| "app 的 `launchMode="singleTask"` 为什么？" | 确保 DeepLink 唤起 App 时不会创建新的 Activity 实例，复用已有的 → `onNewIntent` 处理新链接 |

---

## 四、踩坑记录（答辩加分项）

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 上传文件后不显示 | `openFileOutput("uploads/file")` 不支持路径分隔符 | `File.mkdirs()` 先建目录，再用 `FileOutputStream` |
| 长按菜单位置错误 | `DropdownMenu` 放在 Scaffold 顶层，与文件项位置无关 | 改用 `Popup` 嵌入 `FileListItem` 内部的 `Box` |
| 系统返回键直接退出 | `NavHost` 默认行为未拦截 | `BackHandler` 拦截，先处理文件夹返回 |
| 网盘 Tab 空间始终 0B | `usedSpace` 只在首次安装计算一次 | 移到 `if(!isInitialized)` 外面，每次启动都计算 |
| 移动操作失败 | `remember` 变量在导航跨页面时丢失 | 改用 `rememberSaveable` 持久化待移动 ID |
| 路径面包屑不更新 | `mutableListOf()` 修改不触发重组 | 改用 `mutableStateListOf()` |

---

## 五、演示路径建议（3-5 分钟录屏）

1. **启动** → 网盘 Tab 显示用户信息 + 空间进度条
2. **切换到文件 Tab** → 根目录文件列表（文件夹在前，各类文件分组）
3. **进入"文档"文件夹** → 点击 TXT 文件 → 跳转阅读器（目前是占位）
4. **返回** → 使用系统返回键先回上级文件夹
5. **上传** → 点击 FAB → 选择本地文件 → Snackbar 提示上传成功 → 出现在列表
6. **多选操作** → 长按进入多选，选中文件 → 底部操作栏：重命名/移动/删除
7. **移动文件** → 点击"移动" → FolderPicker 选目标 → 路径面包屑导航 → 确定
8. **分享** → 选中文件 → 点"分享" → 复制 DeepLink
9. **回到网盘 Tab** → 最近浏览/转存已更新 → 点"全部"查看完整列表
10. **关闭 App** → 重新打开 → 数据持久化，空间正确显示
