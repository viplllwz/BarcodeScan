# 盘点 — 条码盘点APP

极简条码盘点工具，运行于 Android 7.0+，支持 HID 扫码枪、手机摄像头扫码、手动输入三种录入方式。

## 功能

- **三模录入**：HID 扫码枪（蓝牙/USB） + 手机摄像头扫码（ZXing）+ 手动打字输入
- **连续扫码**：摄像头扫码后自动继续，无需反复点击，按返回键退出
- **双模式切换**：自动累计（同条码+1）/ 手动输入（扫码后弹窗确认数量）
- **重复条码提醒**：手动模式下已盘点的条码再次扫描时显示当前数量，可修改
- **新建盘点**：保存当前记录到历史，支持自定义命名
- **历史继续**：历史盘点可恢复继续盘点
- **单条删除**：列表项左滑删除
- **点击编辑**：点击列表条目修改数量
- **CSV 导出**：导出到自定义路径（默认 Downloads），含 BOM 兼容 Excel 中文
- **历史记录**：查看过往盘点会话，可重新导出或继续盘点
- **纯离线**：无网络依赖，数据存本地 SQLite

## 使用

| 操作 | 方式 |
|------|------|
| 扫描条码 | HID 扫码枪即插即用 / 点击 📷 摄像头扫码 |
| 手动输入 | 在输入框中输入条码 → 回车或点 ✓ |
| 切换模式 | 点击「自动累计」按钮切换为「手动输入」 |
| 编辑数量 | 点击列表中任意条目 |
| 删除条目 | 列表中条目**左滑** |
| 新建盘点 | 顶部「新建盘点」按钮，可输入名称 |
| 清空数据 | 左下角清除按钮（二次确认） |
| 导出 CSV | 右下角绿色分享按钮 |
| 设置导出路径 | **长按**导出按钮 |
| 继续历史盘点 | 切换到「历史」Tab → 点击「继续盘点」 |

## 界面布局

```
┌─────────────────────────────────────┐
│  [        新建盘点        ]         │
│  共 15 种商品  |  总计 38 件         │
│                                     │
│  [ 输入条码... ] [📷] [自动累计] [✓] │
├─────────────────────────────────────┤
│  6901234567890      × 5   14:30:01  │
│  6909876543210      × 2   14:30:05  │
│  ...                                │
└─────────────────────────────────────┘
```

## 安装

### 编译

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

或直接将 APK 传输到手机安装。

## 技术栈

| 层 | 技术 |
|---|------|
| 语言 | Java 8 |
| UI | XML Layout + Material Components |
| 扫码 | ZXing（journeyapps/zxing-android-embedded） |
| 数据库 | SQLite（原生，继承 SQLiteOpenHelper） |
| 最低版本 | Android 7.0 (API 24) |
| 目标版本 | Android 14 (API 34) |

## CSV 导出格式

```csv
条码,数量,扫描时间
6901234567890,5,2026-07-31 15:30:00
6909876543210,2,2026-07-31 15:30:05
```

带 BOM 头，Excel / WPS 直接打开中文不乱码。

## 项目结构

```
app/src/main/java/com/module/dot/
├── DotApp.java
├── model/
│   ├── InventoryItem.java          # 盘点条目：条码 + 数量 + 时间
│   └── InventorySession.java       # 盘点会话：名称 + 时间 + 统计
├── data/local/
│   ├── MyDatabaseManager.java      # SQLite 基类
│   └── InventoryDatabase.java      # 数据库操作：增删改查 + 会话恢复
├── utils/
│   ├── CsvExporter.java            # CSV 导出 + 路径管理
│   ├── FileManager.java            # 文件读写
│   ├── LocalFormat.java            # 日期格式化
│   └── Utils.java                  # 通用工具
└── view/
    ├── MainActivity.java           # 主 Activity + 扫码枪拦截
    ├── ScanFragment.java           # 盘点主屏（输入栏 + 模式切换 + 扫码）
    ├── HistoryFragment.java        # 历史记录（导出 + 继续盘点）
    ├── CaptureAct.java             # ZXing 扫码 Activity
    ├── adapters/
    │   ├── InventoryAdapter.java   # 盘点列表适配器
    │   ├── HistoryAdapter.java     # 历史列表适配器
    │   └── MainPagerAdapter.java   # ViewPager 适配器
    └── utils/
        ├── ScannerManager.java     # ZXing 扫码封装
        ├── ScannerGunHelper.java   # 扫码枪 HID 输入解析
        └── BarcodeManager.java     # 条码生成工具
```

## 更新日志

### 2026-08-01
- 📷 摄像头按钮改用相机图标
- 🔄 摄像头支持连续扫码（扫完自动继续）
- ✏️ 手动模式下重复条码显示当前数量，可修改
- 📋 历史盘点支持「继续盘点」
- 🏷️ 新建盘点支持自定义名称
- 🎨 输入栏重构为始终可见，模式按钮移入输入栏
- ⌨️ 新增手动打字录入条码功能

### 2026-07-31
- 初始版本，基于 [Wiscarlens/Dot](https://github.com/Wiscarlens/Dot) 二开
