# 车机桌面 - 航盛蓝屏车机版（凡尔赛C5X一级车）

## 两种获取 APK 的方式

### 方式一：GitHub Actions 云编译（推荐，免费）

1. 注册/登录 [GitHub](https://github.com)
2. 新建仓库，上传本项目所有文件
3. 点击仓库页面的 **Actions** 标签
4. 点击 **Build APK** → **Run workflow**
5. 等待约 3 分钟，完成后点击下载 **CarLauncher-C5X** 即可得到 APK

### 方式二：本地编译

安装 Android Studio → 打开项目 → Build → Build APK(s)

---

## 安装到航盛车机（ADB 方式）

### 准备工作
- 手机安装「甲壳虫ADB助手」（安卓）或电脑安装 ADB 工具
- 车机和手机连接同一 WiFi 热点（用手机开热点）

### 开启车机 ADB 调试
1. 车机进入 **设置 → 系统**
2. 连续点击**版本号** 7次，进入开发者模式
3. 进入调试界面，点击 **WiFi Mode** 开启无线调试
4. 记下显示的 **IP地址**（如 192.168.43.xxx:5555）

### 安装 APK
**手机端（甲壳虫）：**
1. 打开甲壳虫 ADB → 连接 → 输入车机IP
2. 安装 APK → 选择下载好的 APK 文件

**电脑端：**
```bash
adb connect 192.168.43.xxx:5555
adb install app-debug.apk
```

### 让桌面显示在车机上
安装后车机默认不显示新桌面，需执行：
```bash
adb shell am start -n com.carlauncher/.ui.MainActivity
```
或在甲壳虫中运行该命令。

---

## 注意事项
- 航盛车机 Android 4.1，内存小，建议不要安装太多应用
- 媒体控制功能在 Android 4.1 上不可用（需要4.3+），已自动隐藏
- 天气功能需要车机联网（连手机热点即可）
