# 盘点 — 条码盘点APP

极简条码盘点工具，运行于 Android 12+，支持摄像头扫码和扫码枪输入。

```
扫码 → 自动累计 → 导出CSV
```

---

## 功能

- **双模式扫码**：摄像头（ZXing） + 扫码枪（蓝牙/USB HID 键盘输入）
- **自动累加**：同条码重复扫描自动 +1
- **手动输入**：扫码后弹窗手动输入数量
- **新建盘点**：保存当前记录到历史，开始新一轮
- **单条删除**：列表项左滑删除
- **CSV 导出**：导出到自定义路径（默认 Downloads），含 BOM 兼容 Excel 中文
- **历史记录**：查看过往盘点会话，可重新导出
- **纯离线**：无网络依赖，数据存本地 SQLite

---

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

---

## 使用

| 操作 | 方式 |
|------|------|
| 扫码 | 点击右下角扫码按钮（二维码图标） |
| 切换模式 | 顶部"自动 +1"按钮 → 切换为"手动输入" |
| 编辑数量 | 点击列表中任意条目 |
| 删除条目 | 列表中条目**左滑** |
| 新建盘点 | 顶部"新建盘点"按钮 |
| 清空数据 | 左下角清除按钮（二次确认） |
| 导出 CSV | 右下角绿色分享按钮 |
| 设置导出路径 | **长按**导出按钮 |
| 查看历史 | 顶部 Tab 切换到"历史" |

### 扫码枪

扫码枪（蓝牙/USB HID）即插即用，无需额外配置。扫码枪输入会自动识别，等同于摄像头扫码。

---

## 技术栈

| 层 | 技术 |
|---|------|
| 语言 | Java 8 |
| UI | XML Layout + Material Components |
| 扫码 | ZXing (journeyapps/zxing-android-embedded) |
| 数据库 | SQLite（原生，继承 SQLiteOpenHelper） |
| 最低版本 | Android 7.0 (API 24) |
| 目标版本 | Android 14 (API 34) |

---

## CSV 导出格式

```csv
条码,数量,扫描时间
6901234567890,5,2026-07-31 15:30:00
6909876543210,2,2026-07-31 15:30:05
```

带 BOM 头，Excel / WPS 直接打开中文不乱码。

---

## 项目结构

```
app/src/main/java/com/module/dot/
├── DotApp.java
├── model/
│   ├── InventoryItem.java          # 盘点条目：条码 + 数量 + 时间
│   └── InventorySession.java       # 盘点会话：时间 + 统计
├── data/local/
│   ├── MyDatabaseManager.java      # SQLite 基类
│   └── InventoryDatabase.java      # 数据库操作：增删改查
├── utils/
│   ├── CsvExporter.java            # CSV 导出 + 路径管理
│   ├── FileManager.java            # 文件读写
│   ├── LocalFormat.java            # 日期格式化
│   └── Utils.java                  # 通用工具
└── view/
    ├── MainActivity.java           # 主 Activity + 扫码枪拦截
    ├── ScanFragment.java           # 盘点主屏
    ├── HistoryFragment.java        # 历史记录
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

---

## 基于

本项目基于 [Wiscarlens/Dot](https://github.com/Wiscarlens/Dot) 二开，感谢原作者。
