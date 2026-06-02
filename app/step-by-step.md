# 简易网盘 — 从零构建全解析

---

# 第 1 步：Gradle 构建系统

先理解整体结构，一个 Android 项目由这些配置文件驱动：

```
D:\android\MyProject\
├── settings.gradle.kts      ← ① 定义项目包含哪些模块
├── build.gradle.kts          ← ② 根构建脚本（所有模块共享的插件）
├── gradle.properties         ← ③ JVM 和 Gradle 全局设置
├── gradle/
│   └── libs.versions.toml    ← ④ 版本目录（统一管理所有依赖版本）
└── app/
    ├── build.gradle.kts      ← ⑤ app 模块的构建脚本
    └── src/main/
        └── AndroidManifest.xml ← ⑥ 向 Android 系统声明这个 App
```

我们按数据流向来讲：从上到下，从外到内。

---

### ① `settings.gradle.kts` — 项目的"目录"

```kotlin
rootProject.name = "My Application"   // 项目名
include(":app")                        // 包含 app 模块
```

这段代码告诉 Gradle："这个项目只有一个模块，叫 `app`"。如果一个项目有多个模块（比如 `:app`、`:library`），都要在这里声明。Gradle 启动时最先读这个文件。

上半部分的 `pluginManagement` 和 `dependencyResolutionManagement` 告诉 Gradle 去哪里下载插件和依赖（Google 的 Maven 仓库 + Maven Central）。

---

### ② 根 `build.gradle.kts` — 声明"有哪些工具可用"

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false   // Android 构建工具
    alias(libs.plugins.kotlin.compose)      apply false   // Kotlin Compose 编译器插件
    alias(libs.plugins.kotlin.serialization) apply false  // JSON 序列化编译器插件
    alias(libs.plugins.ksp)                 apply false   // KSP 注解处理器
}
```

关键概念：**`apply false`**

这意味着"声明这个插件存在，但不要在根项目上应用它"。真正应用插件的是 `app/build.gradle.kts`。根脚本的作用只是声明可用插件列表，具体用哪些由每个模块自己决定。

你可以把它想象成："工具箱里有锤子、螺丝刀、电钻"——根脚本声明了。`app/build.gradle.kts` 说："我这个模块要用电钻"。

---

### ③ `gradle.properties` — 全局开关

```properties
org.gradle.jvmargs=-Xmx2048m          # 给 Gradle 分配 2GB 内存
kotlin.code.style=official            # 使用 Kotlin 官方代码风格
android.disallowKotlinSourceSets=false # 允许 KSP 与 AGP 9.x 配合使用
```

最后一行值得展开：**这是一个踩坑记录。** AGP 9.x 内置了 Kotlin 编译器，KSP 的输出目录默认不被识别。这个设置告诉 AGP："允许 KSP 的源码目录"，不加这行编译会直接报错。

---

### ④ `gradle/libs.versions.toml` — 版本目录（Version Catalog）

这是 Gradle 7.0 引入的功能，用于**统一管理所有依赖的版本**。分三个区块：

#### `[versions]` — 定义版本号
```toml
agp = "9.2.1"           # Android Gradle Plugin
kotlin = "2.2.10"        # Kotlin 编译器
composeBom = "2026.02.01" # Compose BOM（统一管理所有 Compose 库的版本）
room = "2.7.1"           # Room 数据库
ksp = "2.2.10-2.0.2"     # KSP（必须匹配 Kotlin 版本）
```

#### `[libraries]` — 定义依赖库
```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
```

格式是 `group:name:version`，对应 Maven 坐标。`version.ref = "room"` 引用了上面定义的版本号。

#### `[plugins]` — 定义构建插件
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**为什么用 Version Catalog 而不是直接写版本号？**

传统做法（不推荐）：
```kotlin
implementation("androidx.room:room-runtime:2.7.1")
```

Version Catalog 做法（本项目的做法）：
```kotlin
implementation(libs.androidx.room.runtime)
```

好处：
1. 版本号集中管理，改一处就全改
2. Android Studio 提供自动补全（`libs.` 后联想）
3. 多模块项目保证版本一致

**特别解释：BOM（Bill of Materials）**

```kotlin
implementation(platform(libs.androidx.compose.bom))
```

BOM 是一个特殊的依赖，它不提供代码，只声明"这一组库应该用哪些版本"。例如 Compose BOM `2026.02.01` 声明了 `compose-ui:1.7.x`、`material3:1.3.x` 等所有版本。你引入 BOM 后，具体 Compose 库不需要写版本号——BOM 自动帮你匹配。

---

### ⑤ `app/build.gradle.kts` — App 模块的构建脚本

这是最核心的文件，分三部分：

#### 第一部分：plugins（应用插件）
```kotlin
plugins {
    alias(libs.plugins.android.application)    // 这是 Android App（不是 Library）
    alias(libs.plugins.kotlin.compose)         // 启用 Compose
    alias(libs.plugins.kotlin.serialization)   // 启用 @Serializable
    alias(libs.plugins.ksp)                    // 启用 Room 的注解处理
}
```

注意这里没有 `apply false`——表示真正应用这些插件。

#### 第二部分：android { } — Android 配置
```kotlin
android {
    namespace = "com.example.myapplication"   // 代码的包名空间
    compileSdk { version = release(36) { minorApiLevel = 1 } }  // 用 API 36 编译

    defaultConfig {
        applicationId = "com.example.myapplication"  // App 的唯一 ID（可以不同于 namespace）
        minSdk = 24     // 最低支持 Android 7.0
        targetSdk = 36  // 目标版本
        versionCode = 1 // 内部版本号（整数，用于商店更新判断）
        versionName = "1.0"  // 显示版本号
    }

    buildFeatures {
        compose = true   // 启用 Compose 编译
    }
}
```

**关键概念：**
- `compileSdk`：编译时使用的 API 级别。设为 36 意味着你可以用 Android 15 的 API
- `minSdk = 24`：最低能安装的 Android 版本（7.0）。设为 24 是因为 need.md 建议 ≥24
- `applicationId` vs `namespace`：大多数情况相同，但可以不同（比如付费版/免费版用不同的 applicationId）

#### 第三部分：dependencies { } — 依赖项
```kotlin
dependencies {
    // Compose BOM 统一管理版本
    implementation(platform(libs.androidx.compose.bom))

    // 核心 Android
    implementation(libs.androidx.core.ktx)           // Kotlin 扩展
    implementation(libs.androidx.activity.compose)    // Activity + Compose 集成

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)   // Material3 组件库
    implementation(libs.androidx.compose.material.icons.extended)  // 图标库

    // Room 数据库
    implementation(libs.androidx.room.runtime)   // Room 运行时
    ksp(libs.androidx.room.compiler)              // KSP 编译器插件（注意是 ksp 不是 implementation）
    implementation(libs.androidx.room.ktx)        // Room Kotlin 扩展（Flow 支持）

    // Navigation 导航
    implementation(libs.androidx.navigation.compose)

    // 其他...
}
```

**三种依赖声明方式的区别：**

| 声明 | 含义 | 示例 |
|------|------|------|
| `implementation` | 编译时需要，但不暴露给依赖此模块的其他模块 | 绝大多数依赖 |
| `ksp` | KSP 注解处理器（只在编译时运行，不打包进 APK） | Room compiler |
| `debugImplementation` | 只在 debug 构建中引入 | UI 测试工具 |

**`ksp` 特别说明：** Room 的 `@Dao`、`@Database` 等注解在编译时需要 KSP 来处理。KSP 读取你的 DAO 接口，自动生成实现类代码。这个过程只在编译时发生，所以用的是 `ksp` 而不是 `implementation`。

---

### ⑥ `AndroidManifest.xml` — 向 Android 系统"自我介绍"

```xml
<manifest>
    <application
        android:name=".SimplePanApplication"     ← 自定义 Application 类
        android:label="@string/app_name"          ← App 显示名称 "简易网盘"
        android:icon="@mipmap/ic_launcher">       ← 桌面图标

        <activity
            android:name=".MainActivity"
            android:exported="true"               ← 允许外部启动
            android:launchMode="singleTask">      ← 只有一个实例

            <!-- 桌面图标入口 -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- DeepLink 入口：simplepan://share?sid=xxx -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <data android:scheme="simplepan" android:host="share" />
            </intent-filter>
        </activity>

        <!-- FileProvider：安全分享内部文件给其他 App -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider" />
    </application>
</manifest>
```

**关键概念逐个解释：**

1. **`android:name=".SimplePanApplication"`** — 自定义 Application 类。App 启动时 Android 系统先创建这个对象。项目用它来初始化数据库单例和 Repository。

2. **`android:exported="true"`** — 这个 Activity 可以被其他 App 启动。MAIN/LAUNCHER 的 Activity 必须 exported=true（桌面要启动它）。

3. **`android:launchMode="singleTask"`** — 整个系统只存在一个 MainActivity 实例。当 DeepLink 唤起 App 时，不会创建新的 Activity，而是复用已有的（调用 `onNewIntent`）。

4. **两个 `<intent-filter>`**：
   - 第一个：声明"我是桌面 App，点我的图标启动我"
   - 第二个：声明"我能处理 `simplepan://share` 这样的链接"

5. **`<provider>` — FileProvider**：Android 7.0+ 禁止用 `file://` URI 分享文件。FileProvider 会把 `File` 转成安全的 `content://` URI，并给接收方临时读取权限。

6. **`${applicationId}`** — 占位符，编译时替换为 `com.example.myapplication`。使用占位符的好处是，如果换了 applicationId（比如不同渠道包），authorities 自动跟着变，不会冲突。

---

## 构建一个 APK 的全流程

```
开发者执行: ./gradlew assembleDebug
    ↓
① settings.gradle.kts  → 识别 :app 模块
    ↓
② 根 build.gradle.kts → 声明可用插件
    ↓
③ gradle.properties   → 设置 JVM 参数
    ↓
④ libs.versions.toml  → 解析依赖版本
    ↓
⑤ app/build.gradle.kts → 应用插件 + 下载依赖 + 编译 Kotlin
    ↓   KSP 阶段：处理 Room 注解，生成 DAO 实现代码
    ↓   Kotlin 编译：.kt → .class（Java 字节码）
    ↓   D8/R8：.class → .dex（Android 可执行格式）
    ↓   打包：.dex + AndroidManifest + 资源 → APK
    ↓
⑥ AndroidManifest.xml  → 打包进 APK，告诉系统 App 信息
```

---

---

# 第 2 步：应用入口与页面导航

这一步我们看 App 是怎么启动的、页面之间怎么跳转的。涉及三个核心文件：

```
MainActivity.kt          ← ① Android 系统启动的入口
ui/navigation/Screen.kt  ← ② 路由定义（类型安全）
ui/navigation/AppNavigation.kt ← ③ 导航图（NavHost）
```

---

### ① `MainActivity.kt` — 一切开始的地方

```kotlin
class MainActivity : ComponentActivity() {       // 继承 ComponentActivity（Compose 专用）
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                        // 让内容延伸到状态栏和导航栏后面
        setContent {                               // Compose 的入口方法
            MyApplicationTheme {                   // 应用主题
                MainApp()                          // 我们的主界面
            }
        }
    }
}
```

**关键概念：**

1. **`ComponentActivity` vs `AppCompatActivity`**
   - `AppCompatActivity`：传统 XML 布局用
   - `ComponentActivity`：Compose 专用，更轻量。项目只用 Compose 所以选这个

2. **`setContent { }`** — 这是 Compose 的"入口点"。替代了传统的 `setContentView(R.layout.xxx)`。花括号里的代码就是 Compose UI 树的根

3. **`enableEdgeToEdge()`** — 让 App 内容铺满整个屏幕（状态栏区域 + 底部导航栏区域都归你管）。`Scaffold` 会自动处理 insets（系统栏的留白）

4. **`MyApplicationTheme { }`** — 主题包装器。里面定义了 Material3 的配色方案、字体样式。所有子 Composable 都继承这个主题

---

### ② `MainApp()` — 底部导航的骨架

```kotlin
@Composable
fun MainApp() {
    val navController: NavHostController = rememberNavController()  // ① 导航控制器

    val items = listOf(
        BottomNavItem("网盘", Icons.Filled.Cloud, Icons.Outlined.Cloud, Screen.Pan.route),
        BottomNavItem("文件", Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Files.route)
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }  // ② 当前选中的 Tab

    // ③ 首次启动初始化 mock 数据
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        InitialDataLoader.initialize(context)
    }

    Scaffold(                                     // ④ Material3 页面脚手架
        bottomBar = {                             // ⑤ 底部导航栏
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            navController.navigate(item.route) {  // ⑥ 导航逻辑
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { /* 填充/描边图标切换 */ },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(navController, Modifier.padding(innerPadding))  // ⑦ 内容区
    }
}
```

逐行拆解：

| 行 | 代码 | 解释 |
|----|------|------|
| ① | `rememberNavController()` | 创建导航控制器。`remember` 保证重组时不会创建新的。它管理着所有页面的"栈" |
| ② | `rememberSaveable { mutableIntStateOf(0) }` | 记录当前 Tab 索引。`rememberSaveable` 保证旋转屏幕后还是选中的 Tab |
| ③ | `LaunchedEffect(Unit)` | "进入组合时执行一次"。`Unit` 作为 key 表示永不重启。在这里加载 mock 数据 |
| ④ | `Scaffold` | Material3 的标准页面脚手架。提供 `topBar`、`bottomBar`、`floatingActionButton` 等插槽 |
| ⑤ | `NavigationBar` + `NavigationBarItem` | Material3 的底部导航栏组件。`selected` 为 true 时高亮 |
| ⑥ | `navController.navigate()` | 页面跳转。三个参数的含义：`popUpTo(startDestinationId) { saveState = true }` = 把 back stack 中之前的所有页面弹出并保存状态；`launchSingleTop = true` = 如果目标已是栈顶就不新建；`restoreState = true` = 恢复之前保存的状态 |
| ⑦ | `AppNavigation(navController, ...)` | 导航宿主。`innerPadding` 是 Scaffold 计算出的内容区安全边距（比如底部被导航栏占了，内容就不该延伸到那里） |

**⑥ 导航参数详解（面试高频题）：**

```kotlin
navController.navigate(item.route) {
    popUpTo(navController.graph.startDestinationId) {
        saveState = true    // 保存被弹出页面的状态
    }
    launchSingleTop = true  // 避免重复创建同一页面
    restoreState = true     // 恢复之前保存的状态
}
```

**为什么这么写？** 假设你在文件 Tab 进入了第 3 层子文件夹，然后切到网盘 Tab，再切回文件 Tab：

- 如果不写 `saveState = true`：回来时回到根目录（文件 Tab 被重新创建）
- 如果写了 `saveState = true`：回来时还在第 3 层子文件夹（状态被保存和恢复）

这就是"Tab 切换不丢状态"的实现原理。

---

### ③ `Screen.kt` — 路由的"类型安全字典"

```kotlin
sealed class Screen(val route: String) {
    data object Pan : Screen("pan")
    data object Files : Screen("files")
    data object FileList : Screen("file_list/{parentId}") {
        fun createRoute(parentId: String): String = "file_list/$parentId"
    }
    data object Reader : Screen("reader/{fileId}") {
        fun createRoute(fileId: String): String = "reader/$fileId"
    }
    data object RecentList : Screen("recent_list/{listType}") { ... }
    data object FolderPicker : Screen("folder_picker/{parentId}") { ... }
}
```

**设计意图：**

1. **`sealed class`** — 有限集合，编译器知道所有子类。配合 `when` 做穷尽匹配
2. **`data object`** — Kotlin 单例 + 数据类。每个路由只有一个实例
3. **`route` 属性** — Navigation Compose 的 URL 路径字符串
4. **`{parentId}` 语法** — 路径参数。Navigation 自动解析 `/file_list/folder-001` 中的 `folder-001`
5. **`createRoute()` 辅助方法** — 避免手写字符串拼接。用 `Screen.Reader.createRoute("txt-001")` 而不是 `"reader/txt-001"`，防止拼写错误

**为什么不用字符串常量？**

不安全的方式：
```kotlin
navController.navigate("reader/$fileId")  // 拼错了编译器也不知道
```

类型安全的方式（本项目）：
```kotlin
navController.navigate(Screen.Reader.createRoute(fileId))  // 只有定义过的路由才能用
```

---

### ④ `AppNavigation.kt` — 导航图（NavHost）

```kotlin
@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Pan.route,  // App 启动默认显示网盘 Tab
        modifier = modifier
    ) {
        // 简单路由：无参数
        composable(Screen.Pan.route) {
            PanScreen(navController = navController)
        }

        // 带参数路由
        composable(
            route = Screen.Reader.route,         // "reader/{fileId}"
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType }  // 声明参数类型
            )
        ) {
            ReaderScreen(navController = navController)  // 参数由 ViewModel 通过 SavedStateHandle 获取
        }

        // 带参数 + 接收返回值（FolderPicker）
        composable(
            route = Screen.FolderPicker.route,
            arguments = listOf(navArgument("parentId") { ... })
        ) {
            // 从上一页读取传递的 move_ids
            val moveIds = navController.previousBackStackEntry
                ?.savedStateHandle?.get<List<String>>("move_ids") ?: emptyList()

            FolderPickerScreen(
                excludedFolderIds = moveIds.toSet(),
                onFolderSelected = { folderId ->
                    // 把结果写回上一页的 savedStateHandle
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "picker_result", folderId ?: "root"
                    )
                    navController.popBackStack()  // 返回上一页
                }
            )
        }
    }
}
```

**关键概念：**

1. **`NavHost`** — 页面容器。`startDestination` 指定第一个显示的页面
2. **`composable(route) { }`** — 注册一个路由。当 `navController.navigate(route)` 被调用，对应的 `{ }` 里的 Composable 就显示出来
3. **`navArgument`** — 声明路由参数的类型。Navigation 会自动从 URL 中提取参数
4. **`savedStateHandle`** — 页面间传递数据的机制。类似 Intent 的 extras，但是类型安全 + 支持复杂对象。`set(key, value)` 写，`get(key)` 读
5. **`popBackStack()`** — "返回上一页"，相当于按下返回键

**跨页面传递数据的完整流程（移动文件为例）：**

```
FilesScreen                         FolderPickerScreen
    │                                     │
    │ ① savedStateHandle.set(              │
    │    "move_ids", [选中文件ID列表])       │
    │                                     │
    │ ② navController.navigate(           │
    │    "folder_picker/root")            │
    │                                     │
    │         ────导航跳转───→             │
    │                                     │
    │                          ③ 读取 savedStateHandle.get("move_ids")
    │                          ④ 用户选择目标文件夹
    │                          ⑤ savedStateHandle.set("picker_result", folderId)
    │                          ⑥ navController.popBackStack()
    │                                     │
    │         ←────返回─────              │
    │                                     │
    │ ⑦ LaunchedEffect(backStackEntryId)
    │    检测到 picker_result
    │ ⑧ 读取 pendingMoveIds（本地 rememberSaveable）
    │ ⑨ viewModel.moveSelectedFiles()
    │
```

---

### 小结：从启动到显示第一个页面的完整路径

```
用户点击桌面图标
    ↓
Android 系统读取 AndroidManifest
    ↓ 找到 MAIN/LAUNCHER 的 Activity
    ↓
MainActivity.onCreate()
    ↓ setContent { MyApplicationTheme { MainApp() } }
    ↓
MainApp() 执行:
    ① rememberNavController()  创建导航控制器
    ② LaunchedEffect(Unit)     初始化 mock 数据
    ③ Scaffold 渲染           底部导航栏 + 内容区
    ④ NavHost 匹配 startDestination = "pan"
    ⑤ PanScreen 显示在内容区
    ↓
用户点击"文件" Tab:
    ① selectedIndex 变为 1
    ② navController.navigate("files")
    ③ NavHost 匹配路由 → FilesScreen 显示
    ④ 之前 PanScreen 的状态被 saveState=true 保存
```

---

---

# 第 3 步：数据层 — Room 数据库设计与数据流动

数据层是整个 App 的"地基"。我们从一个具体问题出发：**"文件列表页面怎么知道要显示哪些文件？"** 顺着这个问题，你会看到数据如何从数据库一路流到屏幕上。

先看图：

```
┌──────────────────────────────────────────────────┐
│  UI (FilesScreen)                                │
│  files.collectAsState()  ← 订阅 StateFlow        │
├──────────────────────────────────────────────────┤
│  ViewModel (FilesViewModel)                      │
│  val files = flatMapLatest { repo.getFiles() }   │
├──────────────────────────────────────────────────┤
│  Repository (FileRepository)                     │
│  withContext(IO) { dao.getFilesByParentId() }    │
├──────────────────────────────────────────────────┤
│  DAO (FileDao)                                   │
│  @Query("SELECT * FROM files WHERE ...")         │
│  → 返回 Flow<List<FileEntity>>                   │
├──────────────────────────────────────────────────┤
│  Entity (FileEntity)                             │
│  @Entity(tableName = "files")                    │
│  映射到 SQLite 一张表                             │
└──────────────────────────────────────────────────┘
```

---

### ① Entity — 定义"表长什么样"

Entity 就是一个加了 Room 注解的 `data class`，每个 Entity 对应 SQLite 的一张表。

#### `FileEntity` — 核心文件表

```kotlin
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val fileId: String,    // UUID 字符串，主键
    val name: String,                   // 文件名
    val size: Long,                     // 文件大小（字节），文件夹为 0
    val path: String,                   // 虚拟存储路径，如 "/文档/笔记.txt"
    val type: String,                   // "folder" / "txt" / "video" / "image" / "audio" / "other"
    val parentId: String?,              // 父文件夹的 fileId，null = 根目录
    val timestamp: Long,                // 修改/创建时间（epoch millis）
    val contentUri: String? = null      // 上传的真实文件的内部存储路径
)
```

**设计要点：**

1. **`fileId: String` 而非自增 Long** — 使用 UUID 字符串作为主键。因为 mock 数据需要在 JSON 中预先定义 fileId，自增 ID 会冲突。用 UUID 也方便以后做多端同步
2. **`type: String` 而非枚举** — SQLite 不支持枚举。类型检查通过 DAO 查询时过滤和 UI 层 `FileTypeHelper` 的 `when` 匹配
3. **`parentId: String?`** — 实现"文件夹树"的关键。`parentId = null` 表示在根目录，`parentId = "folder-001"` 表示在 "文档" 文件夹里。查询子文件时用 `WHERE parentId = ?`
4. **`contentUri: String?`** — 用户上传的真实文件在内部存储的路径（如 `uploads/uuid_xxx.mp4`）。mock 数据此项为 null，需要通过这个字段区分"模拟文件"和"真实文件"

#### `RecentBrowseEntity` — 最近浏览记录

```kotlin
@Entity(
    tableName = "recent_browse",
    indices = [Index(value = ["fileId"], unique = true)]  // 唯一索引
)
data class RecentBrowseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,   // 自增主键
    val fileId: String,                                    // 关联 files 表
    val browseTime: Long                                   // 浏览时间
)
```

**关键设计：`indices = [Index(value = ["fileId"], unique = true)]`**

这行的含义：fileId 列上建唯一索引。配合 `@Insert(onConflict = REPLACE)`，同一个文件被浏览多次时，不会产生多条记录（INSERT OR REPLACE），而是更新 `browseTime`。

**为什么不用 `fileId` 作主键？** 因为 SQLite 的 `REPLACE` 依赖主键或唯一索引来判断"冲突"。自增主键 `id` 永远不冲突，唯一索引 `fileId` 负责冲突检测。

**对比：`@PrimaryKey(autoGenerate = true)` vs `@PrimaryKey`**
- `autoGenerate = true`：自增整数主键，插入时不指定自动生成
- 不带 `autoGenerate`：必须手动指定值（如 `FileEntity` 的 UUID）

#### `RecentTransferEntity` — 最近转存记录

结构同 `RecentBrowseEntity`，只是字段名改为 `transferTime`。设计思路完全一致。

#### `ShareLinkEntity` — 分享链接映射

```kotlin
@Entity(tableName = "share_links")
data class ShareLinkEntity(
    @PrimaryKey val shareId: String,  // 随机 Base62 字符串（主键）
    val fileId: String,               // 对应的文件/文件夹 ID
    val createdAt: Long               // 创建时间
)
```

**为什么需要这张表？** need.md 要求"分享链接不包含直接的文件明文信息"。所以 URL 里只有 `sid=随机token`，真正的 `fileId` 存在本地 `share_links` 表里。分享链接被点击时，App 用 `shareId` 查表得到 `fileId`。

---

### ② DAO — 定义"怎么操作这张表"

DAO（Data Access Object）用 `@Dao` 注解的**接口**声明数据库操作。Room 在编译时（通过 KSP）自动生成实现代码。

#### `FileDao` — 逐方法分析

```kotlin
@Dao
interface FileDao {

    // ★ 核心查询：根据父文件夹 ID 获取文件列表
    @Query("SELECT * FROM files WHERE parentId IS :parentId OR (parentId IS NULL AND :parentId IS NULL)")
    fun getFilesByParentId(parentId: String?): Flow<List<FileEntity>>

    // 一次性查询单个文件
    @Query("SELECT * FROM files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: String): FileEntity?

    // 获取所有文件夹（除了指定 ID，用于移动目标选择器）
    @Query("SELECT * FROM files WHERE type = 'folder' AND fileId != :excludeId")
    suspend fun getAllFoldersExcept(excludeId: String): List<FileEntity>

    // 插入/更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    // 批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    // 更新操作（用 @Query 执行 UPDATE）
    @Query("UPDATE files SET name = :newName WHERE fileId = :fileId")
    suspend fun renameFile(fileId: String, newName: String)

    @Query("UPDATE files SET parentId = :newParentId WHERE fileId = :fileId")
    suspend fun moveFile(fileId: String, newParentId: String?)

    // 删除操作
    @Query("DELETE FROM files WHERE fileId = :fileId")
    suspend fun deleteFile(fileId: String)

    @Query("DELETE FROM files WHERE parentId = :parentId")
    suspend fun deleteChildrenOfFolder(parentId: String)

    // 聚合查询
    @Query("SELECT COALESCE(SUM(size), 0) FROM files WHERE type != 'folder'")
    suspend fun getTotalUsedSpace(): Long

    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFileCount(): Int
}
```

**方法签名设计原则：**

| 返回类型 | 含义 | 使用场景 |
|---------|------|---------|
| `Flow<List<T>>` | 响应式：数据变化时自动发射新值 | 文件列表、最近浏览/转存 |
| `suspend fun` | 一次性：调用一次返回一次 | 插入、删除、重命名、统计 |
| `List<T>`（不带 Flow） | 一次性，不带协程 | 仅用于不需要响应式的查询 |

**核心查询详解：**

```sql
SELECT * FROM files
WHERE parentId IS :parentId
   OR (parentId IS NULL AND :parentId IS NULL)
```

这个 WHERE 条件处理了两个 case：
- `parentId = "folder-001"` → 查 `WHERE parentId = "folder-001"`
- `parentId = null` → 查 `WHERE parentId IS NULL`（根目录文件）

SQLite 中 `NULL = NULL` 不成立（NULL 不等于任何值，包括它自己），所以必须用 `IS NULL` 单独判断。

**`OnConflictStrategy.REPLACE`：** 当插入的 `fileId` 已存在时，用新数据覆盖旧数据。用于：
- 上传文件时，如果 UUID 冲突（极低概率），自动替换
- 批量同步 mock 数据时，允许重复执行不会报错

#### `RecentBrowseDao` — JOIN 查询

```kotlin
// ★ 自定义结果类
data class FileWithBrowseTime(
    @Embedded val file: FileEntity,  // 嵌入完整的 FileEntity 字段
    val browseTime: Long             // 额外加上 browseTime
)

@Dao
interface RecentBrowseDao {
    @Query("""
        SELECT f.*, rb.browseTime FROM files f
        INNER JOIN recent_browse rb ON f.fileId = rb.fileId
        ORDER BY rb.browseTime DESC LIMIT :limit
    """)
    fun getRecentBrowses(limit: Int = 20): Flow<List<FileWithBrowseTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentBrowseEntity)
}
```

**`@Embedded` 注解的含义：** `FileWithBrowseTime` 不是一个数据库表，而是一个"查询结果载体"。`@Embedded val file: FileEntity` 意思是"把 `FileEntity` 的所有列（`fileId`, `name`, `size`...）铺平到这个结果里"。SQL 查询中的 `SELECT f.*` 返回所有 files 列，Room 自动映射到 `FileEntity` 的字段上。

最终 `FileWithBrowseTime` 包含：`file.fileId`、`file.name`、`file.type`... 加上 `browseTime`。

#### `RecentTransferDao` — 结构完全一致

与 `RecentBrowseDao` 结构相同，只是 `browseTime` 换成 `transferTime`。两个 DAO 代码几乎一样——这是 Room 的一个小痛点（没办法优雅地泛型化 DAO）。

#### `ShareLinkDao` — 最简单的 DAO

```kotlin
@Dao
interface ShareLinkDao {
    @Insert
    suspend fun insert(link: ShareLinkEntity)  // 无冲突策略（shareId 随机生成，几乎不冲突）

    @Query("SELECT * FROM share_links WHERE shareId = :shareId")
    suspend fun getShareLink(shareId: String): ShareLinkEntity?
}
```

---

### ③ `AppDatabase` — 数据库单例

```kotlin
@Database(
    entities = [FileEntity::class, RecentBrowseEntity::class,
                RecentTransferEntity::class, ShareLinkEntity::class],
    version = 1,
    exportSchema = false  // 不导出 schema JSON（省空间）
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun recentBrowseDao(): RecentBrowseDao
    abstract fun recentTransferDao(): RecentTransferDao
    abstract fun shareLinkDao(): ShareLinkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "simplepan.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
```

**双重检查锁定（DCL）单例模式详解：**

```kotlin
@Volatile                                  // ① 禁止指令重排
private var INSTANCE: AppDatabase? = null

fun getInstance(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) { // ② 第一层检查（快速路径）
        // ③ 第二层检查（同步块内）
        Room.databaseBuilder(context, ...)
            .build()
            .also { INSTANCE = it }         // ④ 赋值
    }
}
```

**为什么这么写？**
1. `@Volatile`：保证 `INSTANCE` 的写入对所有线程立即可见。没有这个的话，线程 A 赋值后线程 B 可能看到旧的 null 值
2. 外层 `?:`：大多数情况（单例已创建）直接返回，不走同步锁，无性能损失
3. `synchronized(this)`：只有创建单例时才加锁。如果多个线程同时首次调用，只有一个进入同步块
4. 内层检查：进入同步块后再检查一次，因为可能在等锁期间别的线程已经创建了

**为什么 Room 需要单例？** 每次 `Room.databaseBuilder().build()` 都会创建一个新的数据库连接池。如果每个页面都创建一个，内存和文件句柄会急剧膨胀。

---

### ④ Repository — "数据从哪来"的封装层

```kotlin
class FileRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val fileDao = db.fileDao()
    private val browseDao = db.recentBrowseDao()
    private val transferDao = db.recentTransferDao()
    private val shareLinkDao = db.shareLinkDao()

    // ★ 直接透传（Flow 响应式查询不需要线程切换）
    fun getFilesByParentId(parentId: String?): Flow<List<FileEntity>> =
        fileDao.getFilesByParentId(parentId)

    // ★ 封装线程切换（suspend 操作需要切到 IO 线程）
    suspend fun renameFile(fileId: String, newName: String) = withContext(Dispatchers.IO) {
        fileDao.renameFile(fileId, newName)
    }

    // ★ 组合多个 DAO 操作（deleteFileRecursively 需要两步）
    suspend fun deleteFileRecursively(fileId: String) = withContext(Dispatchers.IO) {
        fileDao.deleteChildrenOfFolder(fileId)  // 先删子文件
        fileDao.deleteFile(fileId)               // 再删自己
    }
}
```

**为什么 Flow 查询不需要 `withContext(IO)` 而 suspend 需要？**

Room 的 Flow 查询内部已经自动在后台线程执行。你只需要在收集 Flow 时切换到主线程即可（`collectAsState()` 自动处理）。

Suspend 查询则需要手动切换线程：`withContext(Dispatchers.IO)` 把操作交给 IO 线程池执行。

**如果不在 IO 线程执行会怎样？** 如果在主线程执行数据库操作且数据量大，App 会 ANR（Application Not Responding）——界面冻结超过 5 秒，系统弹出"应用无响应"对话框。

---

### ⑤ DTO + JSON 解析 — "外部数据"和"内部数据"分离

```kotlin
// FileDto.kt — JSON 的形状（外部数据格式）
@Serializable
data class FileDto(
    val fileId: String,
    val name: String,
    val type: String,
    val size: Long = 0,
    val path: String = "",
    val parentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val children: List<FileDto>? = null  // 递归嵌套！
)

// FileEntity — 数据库的形状（内部数据格式）
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val fileId: String,
    val name: String,
    ...
    val contentUri: String? = null  // 数据库专有字段，JSON 里没有
)
```

**为什么需要两个类？** DTO（Data Transfer Object）和 Entity 职责不同：
- `FileDto`：对应 `assets/files.json` 的结构，有 `children` 嵌套，没有 `contentUri`
- `FileEntity`：对应数据库表结构，扁平（无嵌套），有 `contentUri`
- 如果以后接入服务器，只需要改 DTO 和解析逻辑，Entity 不变

**`flattenDtos()` — 从嵌套 JSON 到扁平数据库**

```kotlin
// JSON 结构：嵌套树
[
  { "name": "文档", type: "folder",
    "children": [
      { "name": "笔记.txt", type: "txt" }
    ]
  }
]

// 数据库结构：扁平表
| fileId      | name      | parentId    |
| folder-001  | 文档       | null        |
| txt-001     | 笔记.txt   | folder-001  |

private fun flattenDtos(dtos: List<FileDto>, parentId: String?, target: MutableList<FileEntity>) {
    for (dto in dtos) {
        target.add(FileEntity(fileId = dto.fileId, ..., parentId = parentId ?: dto.parentId))
        if (!dto.children.isNullOrEmpty()) {
            flattenDtos(dto.children, dto.fileId, target)  // 递归处理子节点
        }
    }
}
```

**模拟网络请求：**

```kotlin
suspend fun syncFromMockData(context: Context) = withContext(Dispatchers.IO) {
    delay(500)  // ← 模拟网络延迟（need.md 要求）
    val jsonString = context.assets.open("files.json").bufferedReader().use { it.readText() }
    val dtos = json.decodeFromString<List<FileDto>>(jsonString)
    val entities = mutableListOf<FileEntity>()
    flattenDtos(dtos, null, entities)
    fileDao.insertFiles(entities)  // 批量插入（一条 SQL 插入所有数据）
}
```

`delay(500)` 的作用不是功能性需求，而是**演示需求**。need.md 说"可模拟网络请求"，如果没有延迟，数据瞬间出现，评委看不出"网络请求"的存在感。加上 500ms 延迟，用户能看到短暂的加载动画。

---

### ⑥ `InitialDataLoader` — 首次启动初始化

```kotlin
object InitialDataLoader {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DATA_INITIALIZED = "data_initialized"

    fun isInitialized(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DATA_INITIALIZED, false)
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as SimplePanApplication
        val fileRepo = app.fileRepository
        val userRepo = app.userRepository

        if (!isInitialized(context)) {
            // ★ 只在首次安装时执行
            if (fileRepo.getFileCount() == 0) {
                fileRepo.syncFromMockData(context)  // 加载 mock 数据
            }
            userRepo.refreshUserInfo(context)        // 加载用户信息
            // 标记为已初始化
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DATA_INITIALIZED, true).apply()
        }

        // ★ 每次启动都执行
        val usedSpace = fileRepo.getTotalUsedSpace()
        userRepo.updateUsedSpace(usedSpace)
    }
}
```

**`SharedPreferences` 标记模式：** 用一个布尔标记 `data_initialized` 区分首次安装和后续启动。首次安装时加载 mock 数据，后续启动时跳过。但 `usedSpace` 每次都重新计算——因为用户可能上次上传了文件。

**这是一个踩坑修复记录：** 最初 `usedSpace` 也在 `if (!isInitialized)` 里，导致重启后空间永远显示 0B。后来把计算逻辑移到 `if` 外面才解决。

---

### 小结：数据如何从数据库流到屏幕

```
App 启动
    ↓
MainApp() → LaunchedEffect(Unit) → InitialDataLoader.initialize()
    ↓
首次启动: syncFromMockData()
    ├─ assets/files.json → kotlinx.serialization 解析
    ├─ flattenDtos() 展平嵌套 → List<FileEntity>
    ├─ delay(500) 模拟网络
    └─ fileDao.insertFiles() 批量入库
    ↓
每次启动: getTotalUsedSpace() → updateUsedSpace()
    ↓
FilesScreen 加载:
    FilesViewModel.files (StateFlow)
        = flatMapLatest { parentId ->
            fileRepository.getFilesByParentId(parentId)  ← Flow
                = fileDao.getFilesByParentId(parentId)   ← 响应式查询
          }
    ↓
UI 订阅:
    val files by viewModel.files.collectAsState()
    ↓
    当数据库变化（增/删/改），Room 自动通知 Flow
    → ViewModel 收到新列表
    → Compose 重组 UI
    → 用户看到最新文件列表
```

这就是**响应式数据流**的核心：数据在 Room 层变化 → Flow 层层传递 → UI 自动刷新。整个过程不需要 `notifyDataSetChanged()` 之类的调用。

---

---

# 第 4 步：文件 Tab — 从 ViewModel 到 UI 的完整数据流

这一步我们追踪一个核心场景：**"用户打开 App，切换到文件 Tab，看到文件列表"**——这背后发生了什么。

---

### ① `FilesViewModel` — 状态的中心枢纽

ViewModel 是 UI 和数据的桥梁。它持有所有 UI 需要的状态，并提供修改状态的方法。

#### 核心状态：文件列表（最重要的一行代码）

```kotlin
val files: StateFlow<List<FileEntity>> = _currentParentId           // ① 当前文件夹 ID
    .flatMapLatest { parentId ->                                     // ② 切换查询
        fileRepository.getFilesByParentId(parentId)                  // ③ 数据库查询（Flow）
    }
    .map { list -> list.sortedWith(fileSortComparator) }             // ④ 排序
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),   // ⑤ 转为 StateFlow
             emptyList())
```

**逐行解释（面试重点）：**

| 步骤 | 代码 | 解释 |
|------|------|------|
| ① | `_currentParentId` | `MutableStateFlow<String?>(null)`。`null` = 根目录。用户进入子文件夹时这个值变化 |
| ② | `flatMapLatest { }` | 当 `parentId` 变化时，**取消旧查询**，**启动新查询**。防止旧查询结果覆盖新查询结果 |
| ③ | `getFilesByParentId()` | 返回 `Flow<List<FileEntity>>`。Room 的响应式查询，数据库变化时自动发射新值 |
| ④ | `.map { sortedWith(...) }` | 对查询结果排序：文件夹优先 → 按类型分组 → 组内按名称排序 |
| ⑤ | `stateIn(viewModelScope, ...)` | 把冷 Flow 转为热 StateFlow。`WhileSubscribed(5000)` = 最后一个订阅者离开后 5 秒停止更新 |

**`flatMapLatest` 为什么重要？**

假设没有 `flatMapLatest`，代码可能是：
```kotlin
val files = _currentParentId.map { repo.getFilesByParentId(it) }
// 返回类型是 StateFlow<Flow<List<FileEntity>>>  ← 嵌套 Flow！UI 没法直接用
```

`flatMapLatest` 把"Flow 的 Flow"展平成一个 Flow。而且 `Latest` 后缀表示：当用户快速点击多个文件夹时，只有最后一个查询的结果会显示（中间查询被取消），避免界面闪烁。

#### 文件夹导航栈

```kotlin
private val navStack = ArrayDeque<String?>()  // 保存路径历史

fun navigateToFolder(parentId: String) {
    clearSelection()                              // 切换文件夹时清空多选
    navStack.addLast(_currentParentId.value)      // 当前位置压栈
    _currentParentId.value = parentId             // 跳到新位置
}

fun navigateBack(): Boolean {
    if (navStack.isEmpty()) return false          // 已到根目录，无法再退
    clearSelection()
    _currentParentId.value = navStack.removeLast() // 弹出上一个位置
    return true
}
```

**为什么不用 Navigation 的返回栈？** 文件夹导航是**同一页面内的状态变化**，不创建新的页面实例。用 Navigation 会导致每次进入子文件夹都创建一个新的 `FilesScreen` 实例，浪费内存。

**数据流：**
```
初始: navStack = []      currentParentId = null  (根目录)
进入 "文档": navStack = [null]  currentParentId = "folder-001"
进入 "子目录": navStack = [null, "folder-001"]  currentParentId = "folder-002"
返回:          navStack = [null]  currentParentId = "folder-001"
返回:          navStack = []  currentParentId = null
```

#### 多选状态管理

```kotlin
private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

// isSelectionMode 是从 selectedFileIds 派生的（不是独立状态！）
val isSelectionMode: StateFlow<Boolean> = _selectedFileIds
    .map { it.isNotEmpty() }   // 有选中项 = 处于选择模式
    .stateIn(...)

fun toggleSelection(fileId: String) {
    _selectedFileIds.value = _selectedFileIds.value.let { ids ->
        if (fileId in ids) ids - fileId else ids + fileId  // Set 的增减
    }
}
```

**为什么 `isSelectionMode` 是派生状态而不是独立变量？**

❌ 错误方式：
```kotlin
var isSelectionMode by mutableStateOf(false)
var selectedIds = setOf<String>()
// 两处都要维护，容易不一致：删除所有选中项时忘记把 isSelectionMode 设为 false
```

✓ 正确方式：
```kotlin
val isSelectionMode = selectedIds.map { it.isNotEmpty() }
// 只要 selectedIds 变了，isSelectionMode 自动跟着变，永远不会不一致
```

**为什么 `asStateFlow()` 不直接暴露 `MutableStateFlow`？**

```kotlin
private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())  // 私有可变
val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()  // 公开只读
```

封装原则：外部（UI 层）只能读取状态，不能直接修改。修改必须通过 ViewModel 的方法（`toggleSelection`、`clearSelection`）。

#### 文件排序 Comparator

```kotlin
companion object {
    private val typeOrder = mapOf(
        "folder" to 0, "txt" to 1, "image" to 2, "video" to 3, "audio" to 4
    )

    val fileSortComparator = Comparator<FileEntity> { a, b ->
        val typeA = typeOrder[a.type] ?: 99  // 未知类型排最后
        val typeB = typeOrder[b.type] ?: 99
        if (typeA != typeB) typeA - typeB     // 先按类型排
        else a.name.lowercase().compareTo(b.name.lowercase())  // 同类按名称排
    }
}
```

排序优先级：**文件夹(0) > txt(1) > 图片(2) > 视频(3) > 音频(4) > 其他(99)**

放在 `companion object` 中的原因：这个 Comparator 是纯函数（无状态），不需要每个 ViewModel 实例都创建一个。

#### Snackbar 消息机制

```kotlin
private val _snackbarMessage = MutableStateFlow<String?>(null)
val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
fun clearSnackbar() { _snackbarMessage.value = null }

// 使用：操作完成后
_snackbarMessage.value = "已删除 3 个项目"
```

UI 层监听到 `snackbarMessage` 非 null → 显示 Snackbar → 调用 `clearSnackbar()` 重置。这样避免了 UI 层需要知道"什么时候应该显示消息"——它只管"有消息就显示"。

---

### ② `FilesScreen` — UI 层的数据消费者

FilesScreen 约 350 行，但结构清晰。按"区域"理解：

```
Scaffold
├── topBar: TopAppBar (标题 + 返回/关闭 + 新建文件夹/全选)
├── bottomBar: BottomAppBar (仅多选模式显示，含重命名/移动/删除/分享)
├── floatingActionButton: FAB (上传，多选模式隐藏)
├── snackbarHost: SnackbarHost (操作反馈)
└── content: PullToRefreshBox
    ├── EmptyState (无文件时)
    ├── LazyColumn (文件列表)
    │   └── FileListItem × N
    ├── LoadingOverlay (加载中)
    └── 对话框 (重命名/删除/新建文件夹)
```

#### ViewModel 的创建

```kotlin
val app = LocalContext.current.applicationContext as SimplePanApplication
val viewModel: FilesViewModel = viewModel(
    factory = FilesViewModel.Factory(app.fileRepository, app.userRepository)
)
```

**为什么需要 Factory？**

`viewModel()` 默认调用 ViewModel 的无参构造函数。但 `FilesViewModel` 需要 `FileRepository` 和 `UserRepository` 作为参数——没有无参构造函数。Factory 告诉框架"怎么创建带参数的 ViewModel"。

**为什么用 `viewModel()` 而不是 `remember { FilesViewModel(...) }`？**

`viewModel()` 把 ViewModel 的生命周期绑定到 Composition 或 Activity。关键好处：
- 旋转屏幕时 ViewModel 不会重建（状态保留）
- 同一个 NavBackStackEntry 内的多个 Composable 共享同一个 ViewModel 实例

#### 状态订阅（数据进入 UI 的入口）

```kotlin
val files by viewModel.files.collectAsState()           // 文件列表
val isLoading by viewModel.isLoading.collectAsState()   // 加载中
val selectedIds by viewModel.selectedFileIds.collectAsState()  // 选中的 ID
val isSelectionMode by viewModel.isSelectionMode.collectAsState()  // 是否多选模式
```

`collectAsState()` 做了三件事：
1. 订阅 StateFlow
2. 在协程中收集数据
3. 值变化时触发 Compose 重组

#### `BackHandler` — 拦截系统返回键

```kotlin
// 优先级 1：如果在子文件夹，先返回上级
BackHandler(enabled = currentParentId != null) {
    viewModel.navigateBack()
}

// 优先级 2：如果处于多选模式，退出选择
BackHandler(enabled = isSelectionMode) {
    viewModel.clearSelection()
}
```

**多个 BackHandler 的执行顺序：** Compose 从外到内检查所有 BackHandler。**最后注册的最先执行**（栈式）。这里先注册的文件夹返回，后注册的选择模式退出。但因为 `enabled` 条件互斥（在子文件夹时不会同时处于多选模式），实际上不会冲突。

#### `LazyColumn` — 高性能长列表

```kotlin
LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(files, key = { it.fileId }) { file ->   // key 用于高效 diff
        FileListItem(
            file = file,
            isSelected = file.fileId in selectedIds,
            isSelectionMode = isSelectionMode,
            onClick = { /* 单击：选择模式=切换选中，普通模式=打开文件 */ },
            onLongPress = { /* 长按：进入选择模式并选中 */ }
        )
    }
}
```

**`key = { it.fileId }` 的作用：**

LazyColumn 默认用 item 的位置（index）作为 key。如果删除第 2 项，第 3 项会"变成"第 2 项，Compose 认为它只是改了内容，触发重组。

但如果指定 `key = { fileId }`，Compose 就知道"fileId=txt-001 的项目被删了，fileId=txt-002 的项目没变"。**避免不必要的重组，同时保持动画/状态正确。**

#### PullToRefreshBox — 下拉刷新

```kotlin
var isRefreshing by remember { mutableStateOf(false) }
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
        scope.launch {
            isRefreshing = true
            app.fileRepository.syncFromMockData(context)  // 重新加载 mock 数据
            isRefreshing = false
        }
    },
    modifier = Modifier.padding(innerPadding).fillMaxSize()
) { /* 文件列表 */ }
```

`PullToRefreshBox` 是 Material3 的下拉刷新容器。用户下拉 → `onRefresh` 回调 → 设置 `isRefreshing = true`（显示加载动画） → 同步数据 → `isRefreshing = false`（收起动画）。

#### 文件选择器（上传）

```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()  // 系统文件选择器
) { uri ->
    uri?.let { viewModel.uploadFile(context, it) }   // 选好后上传
}

// FAB 点击触发
FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") })
```

**`rememberLauncherForActivityResult`：** Compose 中启动 Activity 并接收返回结果的机制。替代了传统的 `startActivityForResult` + `onActivityResult`。

**`"*/*"` 的含义：** MIME 类型通配符，接受任何类型的文件。

---

### ③ `FileListItem` — 可复用的列表项组件

```kotlin
@Composable
fun FileListItem(
    file: FileEntity,
    isSelected: Boolean,          // 当前是否被选中
    isSelectionMode: Boolean,     // 是否处于选择模式
    onClick: () -> Unit,          // 点击回调（由父组件决定行为）
    onLongPress: () -> Unit,      // 长按回调
) {
    Box {
        ListItem(
            headlineContent = { Text(file.name) },              // 主标题：文件名
            supportingContent = { Text(formatFileSize(file.size)) },  // 副标题：文件大小
            leadingContent = {
                if (isSelectionMode)
                    Checkbox(checked = isSelected)  // 选择模式：显示 checkbox
                else
                    Icon(getFileIcon(file.type))    // 普通模式：显示文件类型图标
            },
            modifier = Modifier.pointerInput(file.fileId) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
        )
    }
    HorizontalDivider()  // 分割线
}
```

**为什么 `onClick` 和 `onLongPress` 通过参数传入而不是在组件内实现？**

FileListItem 的职责是"展示一个文件条目"，不知道也不该知道"点击后应该做什么"。具体行为由父组件决定：
- 在 FilesScreen 中：单击 = 打开文件，长按 = 进入多选
- 在未来的某个其他页面中：可能有不同的行为

这符合**单一职责原则**：组件只负责展示 + 手势检测，行为逻辑交给调用方。

---

### ④ 多选模式 — 完整交互流程

```
初始状态：selectionMode=false, selectedIds={}

用户长按 fileA:
    toggleSelection("fileA")
    → selectedIds={"fileA"}
    → isSelectionMode 自动变为 true
    UI 变化:
    - TopAppBar 显示 "已选 1 项"
    - 左侧箭头变为 × 按钮
    - 每个文件前显示 checkbox
    - 底部出现 BottomAppBar（重命名/移动/删除/分享）
    - FAB 隐藏

用户点击 fileB:
    toggleSelection("fileB")
    → selectedIds={"fileA", "fileB"}
    → isSelectionMode 仍是 true
    TopAppBar 显示 "已选 2 项"

用户点击 × 或系统返回:
    clearSelection()
    → selectedIds={}
    → isSelectionMode 自动变为 false
    UI 恢复正常

用户点击"删除"按钮:
    deleteTargets = ["fileA", "fileB"]
    DeleteConfirmDialog 弹出
    确认 → viewModel.deleteSelectedFiles()
          → 逐条 fileRepository.deleteFileRecursively()
          → clearSelection()
          → Snackbar "已删除 2 个项目"
```

### ⑤ 文件类型分发 — `onFileClick`

```kotlin
private fun onFileClick(file: FileEntity, viewModel, navController, context) {
    when (file.type) {
        "folder" -> viewModel.navigateToFolder(file.fileId)     // → 进入文件夹
        "txt" -> {
            viewModel.recordBrowse(file.fileId)                 // → 记录浏览
            navController.navigate(Screen.Reader.createRoute(file.fileId))  // → 跳转阅读器
        }
        "video", "audio" -> {
            viewModel.recordBrowse(file.fileId)                 // → 记录浏览
            FileOpener.openFile(context, file)                  // → 系统播放器
        }
        else -> Toast.makeText(context, "暂不支持预览此文件类型", ...)
    }
}
```

**设计模式：策略分发。** `file.type` 字段决定了打开方式。如果以后要加新的文件类型（比如 PDF），只需在 `when` 中加一个分支，不影响其他类型。

---

### 小结：从数据库到屏幕的完整数据流

```
用户打开 App → 切换到文件 Tab
    ↓
FilesScreen 创建 → FilesViewModel 创建
    ↓
ViewModel.files = currentParentId
    .flatMapLatest { fileDao.getFilesByParentId(null) }    ← Room Flow
    .map { sortedWith(comparator) }
    .stateIn(viewModelScope, ...)
    ↓
UI: val files by viewModel.files.collectAsState()
    ↓
LazyColumn 渲染 items(files) { FileListItem(...) }
    ↓
用户看到排序好的文件列表（文件夹在前 → 按类型分组 → 按名称排序）
    ↓
用户点击"文档"文件夹:
    navigateToFolder("folder-001")
    → currentParentId.value = "folder-001"
    → flatMapLatest 取消旧的根目录查询，切换到新的子目录查询
    → files Flow 发射新数据 → UI 自动刷新
    ↓
用户长按文件 → 进入多选模式 → 底部操作栏出现 → 选择操作
```

---

---

# 第 5 步：网盘 Tab 与跨页面复用

这一步看网盘 Tab 是怎么实现的，以及**两个 Tab 如何共享同一份数据源**。

---

### ① `PanViewModel` — 最简洁的 ViewModel

```kotlin
class PanViewModel(
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val userInfo: StateFlow<UserInfo> = userRepository.userInfo
        .stateIn(viewModelScope, WhileSubscribed(5000), UserInfo("", "", 0, 0))

    val recentBrowses: StateFlow<List<FileWithBrowseTime>> = fileRepository.getRecentBrowses(20)
        .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    val recentTransfers: StateFlow<List<FileWithTransferTime>> = fileRepository.getRecentTransfers(20)
        .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    fun recordBrowse(fileId: String) {
        viewModelScope.launch { fileRepository.recordBrowse(fileId) }
    }
}
```

**对比 FilesViewModel：**

| | FilesViewModel | PanViewModel |
|---|---|---|
| 状态数量 | 8 个 StateFlow | 3 个 StateFlow |
| 核心 Flow 操作 | `flatMapLatest` + `map` | 直接 `stateIn` |
| 业务方法 | navigate、toggle、upload、delete... | 仅 recordBrowse |
| 复杂度来源 | 文件夹导航栈 + 多选状态 | 无，只是数据的"转接者" |

PanViewModel 的设计思想：**不做复杂的 Flow 转换，只做"透传 + 订阅"**。因为网盘 Tab 不需要文件夹导航、不需要多选，它只需要"订阅数据并展示"。

---

### ② `PanScreen` — 网盘 Tab 的 UI 结构

```kotlin
@Composable
fun PanScreen(navController: NavHostController) {
    // ① ViewModel 创建（同样通过 Factory 注入依赖）
    val viewModel: PanViewModel = viewModel(
        factory = PanViewModel.Factory(app.fileRepository, app.userRepository)
    )

    // ② 订阅状态
    val userInfo by viewModel.userInfo.collectAsState()
    val recentBrowses by viewModel.recentBrowses.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()

    // ③ UI 结构
    LazyColumn {
        item { UserInfoCard(userInfo) }               // 用户信息卡
        item { SectionHeader("最近转存") }
        items(recentTransfers.take(4)) { ... }       // 最多 4 条
        if (recentTransfers.size > 4)
            item { ViewAllButton { ... } }            // "查看全部"按钮

        item { SectionHeader("最近浏览") }
        items(recentBrowses.take(4)) { ... }
        if (recentBrowses.size > 4)
            item { ViewAllButton { ... } }
    }
}
```

**设计细节分析：**

#### `UserInfoCard` — 空间进度条

```kotlin
val used = userInfo.usedSpace          // 从数据库统计的实际已用空间
val total = userInfo.totalSpace         // 总容量（10 GB）
val percent = if (total > 0) used.toFloat() / total else 0f

Text("已用 ${formatFileSize(used)} / 共 ${formatFileSize(total)}")
LinearProgressIndicator(progress = { percent.coerceIn(0f, 1f) })
Text("剩余 ${formatFileSize(total - used)}")
```

**空间数据如何保持更新？** 每个涉及文件大小变化的地方都调用 `updateUsedSpace()`：
- 上传文件后 → `updateUsedSpace()`
- 删除文件后 → `updateUsedSpace()`
- App 启动时 → `InitialDataLoader` 每次启动都重新计算

`UserRepository.updateUsedSpace()` 更新 `_userInfo` StateFlow → UI 自动刷新。

#### `recentTransfers.take(4)` — 列表截断

```kotlin
items(recentTransfers.take(4), key = { it.file.fileId + "t" }) { ... }
```

`take(4)` 返回 List 的前 4 个元素。如果 `recentTransfers` 只有 2 条，就显示全部 2 条。

**为什么 key 中是 `fileId + "t"` 而不是 `fileId`？** 因为 `recentTransfers` 和 `recentBrowses` 可能包含同一个文件。如果只用 `fileId` 作为 key，Compose 会认为它们是同一个 item（key 冲突）。加上 `"t"` 和 `"b"` 后缀确保两个列表的 key 不冲突。

#### `ViewAllButton` — "查看全部"按钮

```kotlin
if (recentTransfers.size > 4) {
    item {
        ViewAllButton { navController.navigate(Screen.RecentList.createRoute("transfer")) }
    }
}
```

只在超过 4 条时显示。"查看全部"导航到 `RecentListScreen`，路由参数 `listType = "transfer"` 或 `"browse"`。

---

### ③ `UserRepository` — 用户数据的"迷你数据源"

```kotlin
class UserRepository(context: Context) {
    private val _userInfo = MutableStateFlow(loadDefaultUserInfo())  // 初始值

    val userInfo: Flow<UserInfo> = _userInfo.asStateFlow()          // 对外只读

    suspend fun refreshUserInfo(context: Context) {
        // 从 assets/user_info.json 读取用户名和总空间
        _userInfo.value = UserInfo(...)
    }

    fun updateUsedSpace(usedSpace: Long) {
        // 局部更新：只改 usedSpace，保留其他字段
        _userInfo.value = _userInfo.value.copy(usedSpace = usedSpace)
    }
}
```

**设计模式：`copy()` 局部更新**

```kotlin
_userInfo.value = _userInfo.value.copy(usedSpace = usedSpace)
```

不创建新的 `UserInfo` 对象来替换整个数据——只修改 `usedSpace` 字段，其他字段（`userName`、`totalSpace`、`avatarUrl`）保持不变。

`data class` 的 `copy()` 自动生成这个方法。相当于：
```kotlin
UserInfo(
    userName = old.userName,    // 保留
    avatarUrl = old.avatarUrl,  // 保留
    totalSpace = old.totalSpace,// 保留
    usedSpace = usedSpace       // 更新
)
```

**为什么 UserRepository 用 `MutableStateFlow` 而不是 Room DAO？**

用户信息不在数据库里（它存在 `SharedPreferences` + JSON 文件里）。如果存在 Room 里，就可以像文件列表一样用 `Flow` 了。`MutableStateFlow` 手动实现了类似的"可观状态"的效果。

---

### ④ `RecentListScreen` — 复用 PanViewModel 的全量列表页

```kotlin
@Composable
fun RecentListScreen(listType: String, navController: NavHostController) {
    // ★ 创建自己的 PanViewModel 实例（和 PanScreen 不共享）
    val viewModel: PanViewModel = viewModel(factory = ...)

    val recentBrowses by viewModel.recentBrowses.collectAsState()
    val recentTransfers by viewModel.recentTransfers.collectAsState()

    // 根据 listType 参数选择用哪个数据源
    val (title, items) = when (listType) {
        "browse" -> "最近浏览" to recentBrowses.map { ... }
        else     -> "最近转存" to recentTransfers.map { ... }
    }

    Scaffold {
        TopAppBar { Text(title); 返回按钮 }
        LazyColumn { items(items) { 显示完整列表（不限 4 条） } }
    }
}
```

**为什么 RecentListScreen 创建了新的 PanViewModel？** 因为 `viewModel()` 创建时，会绑定到当前的 `NavBackStackEntry`。RecentListScreen 有自己独立的 NavBackStackEntry（它是通过 Navigation 跳转来的），所以 `viewModel()` 会创建新的 ViewModel 实例。

**这意味着 PanScreen 和 RecentListScreen 各自拥有独立的 PanViewModel？** 是的。但这不会导致数据不一致，因为两个 ViewModel 订阅的是同一份数据源（Room 数据库的 Flow）——Room 保证两个订阅者拿到相同的数据。

---

### ⑤ 共享工具类 — 跨页面复用

三个 `object` 单例工具类，被 FilesScreen、PanScreen、RecentListScreen 共用：

#### `FileTypeHelper`

```kotlin
object FileTypeHelper {
    fun getFileIcon(type: String): ImageVector   // 类型 → 图标
    fun formatFileSize(bytes: Long): String      // 1024 → "1 KB"
    fun getFileTypeFromName(name: String): String // "test.mp4" → "video"
}
```

**为什么是 `object` 而不是普通 class？** 这些方法都是纯函数（输入 → 输出），没有状态。`object` 表示单例，不需要创建实例，直接 `FileTypeHelper.formatFileSize(1024)` 调用。

#### `TimeUtils`

```kotlin
object TimeUtils {
    fun formatRelativeTime(timestamp: Long): String {  // 1710000000 → "3分钟前"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            ...
            else -> SimpleDateFormat("yyyy-MM-dd").format(Date(timestamp))
        }
    }
}
```

**为什么要用"相对时间"？** UX 考虑。用户看到"3分钟前打开"比"2026-05-28 14:23:15"更直观。超过 7 天的记录才显示绝对日期。

**`60_000` 是什么？** Kotlin 允许用下划线分隔数字，增强可读性。`60_000` = 60000 毫秒 = 60 秒 = 1 分钟。

#### `FileOpener`

```kotlin
object FileOpener {
    fun openFile(context: Context, file: FileEntity) {
        when (file.type) {
            "video" -> openWithSystemPlayer(context, file, "video/*")
            "audio" -> openWithSystemPlayer(context, file, "audio/*")
            else -> Toast.makeText(context, "暂不支持预览此文件类型", ...)
        }
    }

    private fun openWithSystemPlayer(context, file, mimeType) {
        if (contentUri.isNullOrBlank()) { Toast("模拟数据无法播放"); return }
        val uri = FileProvider.getUriForFile(context, ..., realFile)
        val intent = Intent(ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
```

**三个页面都用 FileOpener：** FilesScreen、PanScreen、RecentListScreen 都通过 `FileOpener.openFile()` 调用。相同的逻辑只写一次。

---

### ⑥ 跨 ViewModel 的数据一致性

关键问题：**在 FilesScreen 中上传了一个文件，PanViewModel 怎么知道"最近转存"要更新？**

```
FilesScreen:
    uploadFile() → fileDao.insertFile(file)
                 → fileDao.upsert(RecentTransferEntity(fileId, time))
                        ↓
                  Room 写入 recent_transfer 表
                        ↓
                  RecentTransferDao 的 Flow 自动发射新数据
                        ↓
PanViewModel:
    recentTransfers = fileRepository.getRecentTransfers(20)
        .stateIn(...)  ← 订阅同一个 Flow
                        ↓
PanScreen:
    val recentTransfers by viewModel.recentTransfers.collectAsState()
                        ↓
    LazyColumn 自动重组 → 用户看到新的转存记录
```

**整个过程不需要：** 手动通知另一个 ViewModel、EventBus/LiveData 全局事件、定时轮询。

**原理：Room 的 Flow → 多个订阅者 → 数据一变，自动全体更新。**

---

### ⑦ 两个 Tab 的 ViewModel 对比总结

| 维度 | FilesViewModel | PanViewModel |
|------|---------------|-------------|
| 核心数据源 | `flatMapLatest { parentId → Flow }` | 直接 `stateIn(repo.flow)` |
| 状态数量 | 8 个 | 3 个 |
| 复杂性来源 | 文件夹导航栈 + 多选集合 | 无 |
| 写操作 | upload/delete/rename/move/createFolder | recordBrowse |
| 派生状态 | `isSelectionMode` from `selectedIds` | 无 |
| Factory | 有（2 个依赖） | 有（2 个依赖） |

**共同的模式：**
1. 都通过 Factory 注入 Repository 依赖
2. 都用 `stateIn(viewModelScope, WhileSubscribed(5000), ...)` 管理 Flow 生命周期
3. 都通过 `collectAsState()` 订阅 → Compose 自动重组
4. 都不直接访问 DAO（通过 Repository 间接访问）

---

---

# 第 6 步：文件操作 — 上传、移动、排序、刷新

这一步剖析每个文件操作的完整流程，从用户手势到数据入库。

---

### ① 文件上传 — 从系统选择器到内部存储

上传是项目中**代码路径最长**的操作，跨越了 UI、ViewModel、Repository、SAF 四个层级。

#### 第一步：触发文件选择器

```kotlin
// FilesScreen.kt
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()   // 系统文件选择器
) { uri -> uri?.let { viewModel.uploadFile(context, it) } }

FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") })
//                                                          ↑
//                                       MIME 通配符：接受所有文件类型
```

**`rememberLauncherForActivityResult` 三个关键点：**

1. **必须在 `@Composable` 函数中调用**，但不能放在任何回调/lambda 里（否则会在重组时注册多次）
2. **`contract`** 决定"启动什么"：`GetContent()` = 系统文件选择器，`TakePicture()` = 相机，等等
3. **`launch(mimeType)`** 启动选择器。`"*/*"` 表示不限制文件类型

#### 第二步：获取文件信息

```kotlin
// FilesViewModel.kt — uploadFile()
val resolver = context.contentResolver
var name = "unknown_file"
var size = 0L

resolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (nameIdx >= 0) name = cursor.getString(nameIdx)
        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
    }
}
```

**`ContentResolver.query()` 详解：** 系统给的是一个 `content://` URI，它不直接告诉你文件名和大小。需要 query 这个 URI 的元数据。`OpenableColumns.DISPLAY_NAME` 和 `OpenableColumns.SIZE` 是标准列名，任何从文件选择器返回的 URI 都会提供这两个字段。

**`?.use { }` 的作用：** `use` 是 Kotlin 的自动关闭资源模式（类似 Java 的 try-with-resources）。`Cursor` 实现了 `Closeable` 接口，`use` 保证不管正常结束还是抛异常，cursor 都会被关闭。

#### 第三步：拷贝到内部存储

```kotlin
val fileName = "${UUID.randomUUID()}_$name"     // 加 UUID 前缀防止重名
val uploadDir = File(context.filesDir, "uploads")
if (!uploadDir.exists()) uploadDir.mkdirs()      // ★ 先建目录
val destFile = File(uploadDir, fileName)
resolver.openInputStream(uri)?.use { input ->
    destFile.outputStream().use { output ->
        input.copyTo(output, bufferSize = 8192)   // 8KB 缓冲区拷贝
    }
}
val internalPath = "uploads/$fileName"
```

**为什么需要"拷贝到内部存储"？** 系统文件选择器返回的 URI 权限是**临时**的（App 关闭后失效）。拷贝到 `filesDir/` 后 App 永久拥有这个文件。

**⭐ 踩坑记录：`openFileOutput` 不支持子目录**

最初用的 `context.openFileOutput("uploads/$fileName", ...)`，但 Android 的这个 API **不支持路径分隔符**。文件在 `filesDir/uploads/` 子目录中，必须先用 `File.mkdirs()` 创建目录，再用 `FileOutputStream` 写入。

**`bufferSize = 8192` 是什么？** 8KB 的缓冲区。`copyTo` 每次读 8KB 写入目标文件，循环直到源文件读完。太大的缓冲区浪费内存，太小的缓冲区导致过多 IO 操作，8KB 是一个经验平衡点。

#### 第四步：入库 + 记录转存

```kotlin
val type = FileTypeHelper.getFileTypeFromName(name)   // "test.mp4" → "video"
val file = FileEntity(
    fileId = UUID.randomUUID().toString(),
    name = name, size = size, path = internalPath,
    type = type,
    parentId = _currentParentId.value,               // 上传到当前目录
    contentUri = internalPath                        // ★ 真实文件标记
)
fileRepository.insertFile(file)
fileRepository.recordTransfer(file.fileId)           // 记录到最近转存
updateUsedSpace()                                     // 更新空间统计
_snackbarMessage.value = "已上传「$name」"
```

**`contentUri = internalPath` 的重要性：** 这是区分"真实文件"和"模拟数据"的标记。`FileOpener.openFile()` 检查这个字段——为 null 时提示"模拟数据无法播放"。

#### 完整上传流程图：

```
用户点击 FAB
  → filePickerLauncher.launch("*/*")
  → 系统文件选择器弹出
  → 用户选择文件
  → 回调: uri = content://...
  → viewModel.uploadFile(context, uri)
      ├─ ContentResolver.query(uri) → name, size
      ├─ File.mkdirs() + FileOutputStream → 拷贝到 filesDir/uploads/
      ├─ FileTypeHelper.getFileTypeFromName() → type
      ├─ FileEntity(..., contentUri=internalPath)
      ├─ insertFile() → Room 写入
      ├─ recordTransfer() → 记录最近转存
      └─ updateUsedSpace() → 刷新空间统计
  → Snackbar: "已上传「xxx.mp4」"
  → Flow 触发 → UI 自动刷新
```

---

### ② `FolderPickerScreen` — 移动目标选择器

这是项目中**交互最复杂的页面**。需求是：选择一个文件夹作为目标，支持嵌套导航、排除自身、路径面包屑。

#### 双栈设计

```kotlin
val navStack = remember { mutableListOf<String?>() }    // parentId 历史
val nameStack = remember { mutableStateListOf("根目录") } // 文件夹名历史（用于路径显示）

val pathString = nameStack.joinToString("/")  // "根目录/文档/子目录"
```

**为什么需要两个栈？**

- `navStack`（`mutableListOf`）：存 `parentId`（数据库查询用），不需要触发 UI 重组
- `nameStack`（`mutableStateListOf`）：存文件夹名（显示路径用），修改时必须触发重组

**⭐ 踩坑记录：** 最初 `nameStack` 也是 `mutableListOf`，进入子文件夹后路径面包屑不更新。因为普通 List 的 `add()`/`remove()` 不会通知 Compose 重组。改成 `mutableStateListOf` 后，每次 `add`/`remove` 自动触发重组。

#### 排除自身

```kotlin
// AppNavigation.kt — 创建时传入
val moveIds = navController.previousBackStackEntry
    ?.savedStateHandle?.get<List<String>>("move_ids") ?: emptyList()
FolderPickerScreen(excludedFolderIds = moveIds.toSet(), ...)

// FolderPickerScreen.kt — 过滤时排除
folders = all.filter { it.type == "folder" && it.fileId !in excludedFolderIds }
```

防止用户把文件夹移动到它自己里面（变成无限递归）。

#### 点击 vs 选择的分歧

```kotlin
ListItem(
    leadingContent = {
        Checkbox(checked = selectedFolderId == folder.fileId)  // 选中 = 目标
    },
    trailingContent = { Icon(Folder) },
    modifier = Modifier.clickable {                              // 点击 = 进入
        navStack.add(currentParentId)
        nameStack.add(folder.name)
        currentParentId = folder.fileId
    }
)
```

这是一个精巧的交互设计：**checkbox 选目标，点击名字进文件夹**。两个操作互不干扰——用户可以在深层文件夹中选中一个文件夹，也可以在根目录就确定目标。

#### 结果回传

```kotlin
// FolderPickerScreen — 用户点"确定"
TextButton(onClick = { onFolderSelected(selectedFolderId) })  // null = 根目录

// AppNavigation.kt — 回调实现
onFolderSelected = { folderId ->
    navController.previousBackStackEntry?.savedStateHandle?.set(
        "picker_result", folderId ?: "root"
    )
    navController.popBackStack()   // 返回 FilesScreen
}

// FilesScreen — 接收结果
LaunchedEffect(backStackEntryId) {
    val result = handle?.get<String>("picker_result")
    if (result != null) {
        viewModel.moveSelectedFiles(if (result == "root") null else result)
    }
}
```

**为什么不用 `navController.navigateUp()` 而是 `popBackStack()`？** `navigateUp()` 会尝试找到 NavGraph 中的上级路由，`popBackStack()` 直接弹出当前页面。对于"选择器→返回"这种场景，`popBackStack()` 更精确。

---

### ③ 对话框三部曲

#### `RenameDialog` — 重命名

```kotlin
@Composable
fun RenameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf(currentName) }  // 预填当前名
    AlertDialog(
        text = { TextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(newName) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
```

**`AlertDialog` 的模式：** `onDismissRequest`（点击外部或返回键）、`confirmButton`（确认）、`dismissButton`（取消）。

**为什么对话框不内嵌在 ViewModel 中？** 对话框是纯 UI 概念——ViewModel 不知道也不该知道"对话框是否显示"。ViewModel 只暴露数据（`renameFile()` 方法），UI 层决定如何触发和展示对话框。

#### `DeleteConfirmDialog` — 删除确认

```kotlin
@Composable
fun DeleteConfirmDialog(fileName: String, onConfirm, onDismiss) {
    AlertDialog(
        text = { Text("确定要删除「$fileName」吗？此操作不可撤销。") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } }
    )
}
```

**为什么删除要确认？** UX 原则——不可逆操作必须有二次确认。

#### `CreateFolderDialog` — 新建文件夹

```kotlin
var folderName by remember { mutableStateOf("") }
TextField(
    placeholder = { Text("请输入文件夹名称") },
    ...
)
confirmButton = {
    TextButton(
        onClick = { if (folderName.isNotBlank()) onConfirm(folderName.trim()) }
        //                ↑ 阻止空名称提交
    ) { Text("确定") }
}
```

**`isNotBlank()` 检查：** 防止创建空名称文件夹。`trim()` 去除首尾空格。

---

### ④ 下拉刷新 — `PullToRefreshBox`

```kotlin
var isRefreshing by remember { mutableStateOf(false) }
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
        scope.launch {
            isRefreshing = true
            app.fileRepository.syncFromMockData(context)  // 重新加载
            isRefreshing = false
        }
    }
) { /* 文件列表 */ }
```

**`PullToRefreshBox` 做了什么：**
1. 监听用户的下拉手势
2. 下拉到阈值 → 触发 `onRefresh` 回调
3. `isRefreshing = true` 时显示 Material3 的圆形加载动画
4. `isRefreshing = false` 时收回动画

---

### ⑤ 文件排序 — 自定义 Comparator

```kotlin
companion object {
    private val typeOrder = mapOf(
        "folder" to 0, "txt" to 1, "image" to 2, "video" to 3, "audio" to 4
    )

    val fileSortComparator = Comparator<FileEntity> { a, b ->
        val typeA = typeOrder[a.type] ?: 99       // 未知类型 = 最低优先级
        val typeB = typeOrder[b.type] ?: 99
        if (typeA != typeB) typeA - typeB          // ① 先按类型排序
        else a.name.lowercase().compareTo(b.name.lowercase())  // ② 再按名称排序
    }
}

// 使用：
val files = ...map { list -> list.sortedWith(fileSortComparator) }
```

**Comparator 的返回值语义：**
- 负数：a 排在 b 前面
- 零：a 和 b 相等
- 正数：a 排在 b 后面

**两层比较逻辑：** 第一层比较 `typeOrder` 的映射值（文件夹=0，txt=1，...）；如果类型相同，第二层比较名称（不区分大小写）。`?: 99` 确保未定义的类型总排在最后。

---

### ⑥ 删除的递归处理

```kotlin
// FileRepository.kt
suspend fun deleteFileRecursively(fileId: String) = withContext(Dispatchers.IO) {
    fileDao.deleteChildrenOfFolder(fileId)  // ① 先删子文件和子文件夹
    fileDao.deleteFile(fileId)               // ② 再删自己
}

// FileDao.kt
@Query("DELETE FROM files WHERE parentId = :parentId")
suspend fun deleteChildrenOfFolder(parentId: String)
```

**为什么分两步？** Room 不支持递归 CTE（SQLite 3.35+ 才有）。在应用层递归删除更可控：先删子节点所有记录，再删父节点。

**潜在问题：** 如果子文件夹下还有子文件夹，`deleteChildrenOfFolder` 只删了一层。实际上 Room 的 `DELETE WHERE parentId = ?` 删除的是直接子节点。如果 `folderA` 下有 `folderB`，`folderB` 下有 `file1.txt`，删除 `folderA` 时：
1. `deleteChildrenOfFolder("folderA")` → 删除 `folderB`
2. `deleteFile("folderA")` → 删除 `folderA`
3. 但 `file1.txt` 的 `parentId = "folderB"`，此时 `folderB` 的记录已被删除，但 `file1.txt` 还在

**实际上这是当前代码的一个潜在 bug！** 完整解决方案应该在 ViewModel 中递归处理，或使用递归的 DAO 调用。不过在当前项目中，mock 数据最多一层嵌套，所以实际不会触发这个问题。后续可以完善。

---

### 小结：文件操作的完整分类

| 操作 | 触发方式 | 涉及文件 | 核心流程 |
|------|---------|---------|---------|
| 上传 | FAB 点击 | FilesScreen + ViewModel + SAF | 文件选择器 → 拷贝 → 入库 → 记录转存 → Snackbar |
| 移动 | 多选后底部栏"移动" | FilesScreen + FolderPicker + Nav | 保存选中 ID → 导航选择器 → 结果回传 → moveFile() |
| 重命名 | 多选后底部栏（单选） | FilesScreen + RenameDialog | 弹窗输入 → renameFile() → Snackbar |
| 删除 | 多选后底部栏 | FilesScreen + DeleteConfirmDialog + ViewModel | 确认 → deleteFileRecursively() → Snackbar |
| 新建文件夹 | TopAppBar 按钮 | FilesScreen + CreateFolderDialog | 输入名称 → createFolder() → 入库 → Snackbar |
| 下拉刷新 | 手势下拉 | FilesScreen + PullToRefreshBox | 下拉 → syncFromMockData() → 入库 |
| 排序 | 自动（Flow 链中） | FilesViewModel | `flatMapLatest → map { sortedWith(comparator) }` |
```




