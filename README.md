<!-- 欢迎阅读 Ark-Pets 说明文档 -->
<!-- 仓库：https://github.com/isHarryh/Ark-Pets -->

<!--suppress HtmlDeprecatedAttribute -->
<div align="center" style="text-align:center">
   <h1> Ark-Pets </h1>
   <img alt="ArkPets icon" width="64" src="https://raw.githubusercontent.com/isHarryh/Ark-Pets/v3.x/assets/icons/icon.png">
   <p>Arknights Desktop Pets | 明日方舟桌宠 (ArkPets)</p>
   <p>
      <img alt="GitHub Latest Release" src="https://img.shields.io/github/v/release/isHarryh/Ark-Pets?display_name=tag&label=Release&sort=semver&include_prereleases">
      <img alt="GitHub Stars" src="https://img.shields.io/github/stars/isHarryh/Ark-Pets?label=Stars">
   </p>
   <hr>
   <p>
      <img alt="GitHub Top Language" src="https://img.shields.io/github/languages/top/isHarryh/Ark-Pets?label=Java">
      <img alt="GitHub License" src="https://img.shields.io/github/license/isHarryh/Ark-Pets?label=License">
      <img alt="Code Factor Grade" src="https://img.shields.io/codefactor/grade/github/isHarryh/Ark-Pets?label=CodeFactor">
      <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/isHarryh/Ark-Pets/build.yml?label=Build">
   </p>
   <p>
      <a href="https://arkpets.harryh.cn?from=readme">🔗官方网站 | Official Website</a>
   </p>
   <sub>
      <i> This project only supports Chinese docs. If you are an English user, feel free to contact us. </i>
   </sub>
</div>

> **关于本仓库**
>
> 这是 [isHarryh/Ark-Pets](https://github.com/isHarryh/Ark-Pets) 的 **Linux 移植分支**（`v4.x-cross`），
> 也是 **Linux 发行包的发布来源**。Windows 版本请前往[上游仓库](https://github.com/isHarryh/Ark-Pets/releases)。
>
> Linux 支持包含：KWin 集成插件（窗口控制/置顶/去边框）、StatusNotifierItem + DBusMenu 托盘、
> 光标主题跟随桌面设置、XDG 自启动，以及针对 Wayland 合成器的适配。
>
> - **安装流程**见[下文](#在-linuxkde-plasma上安装)
> - **应用本体**由 [GitHub Actions](.github/workflows/build-linux.yml) 构建（x86_64 + aarch64）
> - **KWin 插件**必须在本机编译（与本机 KWin ABI 绑定），见下文第二步

## 介 绍 <sub>Intro</sub>

### 实现的功能

1. **支持将《明日方舟》角色模型作为桌宠启动。**<details><summary>查看详情</summary>
    现已支持的模型类型包括：
    1. 干员基建小人（含时装）；
    2. 干员动态立绘（含时装）；
    3. 敌方战斗小人。
2. **启动器提供图形用户界面以便浏览模型和调整各种设置。** <details><summary>查看详情</summary>
    1. 可以按名称、拼音、时装品牌搜索，或按类别筛选以查找模型；
    2. 可以从互联网中下载由社区维护的模型库；
    3. 可以自定义桌宠的动作交互、部署位置和物理参数等行为设置；
    4. 可以自定义桌宠的图像缩放、最大帧率和窗口边界等显示设置。
3. **支持模拟游戏内干员基建小人的行为。** <details><summary>查看详情</summary>
    1. 支持行走、坐下和躺下的动作；
    2. 能够被鼠标交互以执行戳一戳动作；
    3. 拥有特殊基建动作的干员，有概率触发这类动作。
4. **支持模拟游戏内敌方小人的行为。** <details><summary>查看详情</summary>
    1. 拥有行走动作的敌人能够行走；
    2. 拥有攻击动作的敌人能够被鼠标交互。
5. **实现了模拟平面重力场** <details><summary>查看详情</summary>
    1. 桌宠支持自由落体等物理现象；
    2. 桌宠可以被拖拽到扩展显示屏上；
    3. 桌宠可以站立在打开的窗口的边缘上。
6. **实现了系统托盘的菜单** <details><summary>查看详情</summary>
    1. 右键托盘图标或者桌宠本体均可弹出菜单；
    2. 菜单可用于开启手动模式和启用透明模式；
    3. 菜单可用于切换多形态角色的形态；
    4. 菜单可用于退出启动器或单个桌宠；
    5. 启动器运行时，已启动的桌宠将被整合到一个托盘中；
    6. 启动器若没有运行，每个桌宠将分别创建自己的托盘。
7. **支持开机自启动等[更多特性](#更多特性)**

### 效果预览图

<table style="margin-left: auto; margin-right: auto;">
    <tr>
        <td> <img alt="demo1" width="250" src="https://raw.githubusercontent.com/isHarryh/Ark-Pets/v3.x/docs/imgs/demo_1.png"> </td>
        <td> <img alt="demo2" width="250" src="https://raw.githubusercontent.com/isHarryh/Ark-Pets/v3.x/docs/imgs/demo_2.png"> </td>
        <td> <img alt="demo3" width="250" src="https://raw.githubusercontent.com/isHarryh/Ark-Pets/v3.x/docs/imgs/demo_3.png"> </td>
    </tr>
</table>

### 下一步计划

以下内容可能在遥远的将来被实现：

- 国际化与响应布局
- 支持按需下载资源
- 支持干员语音功能
- 全面更新依赖库的版本
- 支持透明模式等配置的记忆

### 常用文档

- **更新日志** > [点击查看](CHANGELOG.md)
- **常见问题解答** > [点击查看](docs/FAQ.md)
- **遥测功能说明** > [点击查看](docs/Telemetry.md)

## 使用方法 <sub>Usage</sub>

目前支持 Windows 7 及以上的图形操作系统；**Linux（KDE Plasma，x86_64 与 aarch64）为实验性支持**，
见[下文](#在-linuxkde-plasma上安装)。

### 快速上手

1. 请[**前往此页面**](https://github.com/isHarryh/Ark-Pets/releases)下载最新的 **ArkPets-Setup.exe** 安装包。
2. 运行安装包并进行软件的安装。安装完成后，打开 ArkPets 启动器。
3. 首次使用时需要**下载模型文件**。请在启动器 “模型” 页面中的 “模型库管理” 面板点击 “下载模型” 按钮。
4. 在启动器 “模型” 页面中检索并选中想要作为桌宠启动的角色，最后点击左下角 “启动” 按钮即可生成桌宠。

> 提示：
> - 如需关闭已启动的桌宠，请右键单击桌宠或系统托盘中的 ArkPets 图标，然后选择 “退出”。
> - 若无法在软件内下载模型，可以访问 [ArkModels 模型仓库](https://github.com/isHarryh/Ark-Models) 页面，手动下载模型压缩包后，在 “模型库管理” 面板点击 “导入压缩包” 按钮后导入模型。
> - 如需将软件从 v2.x 或 v3.x 更新到更高版本，无需预先手动卸载，直接运行新版安装包即可。

### 在 Linux（KDE Plasma）上安装

> **实验性支持。** 已在 Debian forky/sid (**x86_64**) 与 Debian 13 (**aarch64**) + KDE Plasma 6.7 /
> KWin 6.7 / Wayland 上验证。完整的步骤、验证方法与故障排查见 [`INSTALL.md`](https://github.com/SKRbCrEsOg/Ark-Pets/releases)。

**x86_64 与 aarch64 的发行包是分开的，请按自己的架构选择：**

| 架构 | 发行包 |
|:--|:--|
| x86_64（Intel / AMD） | [最新 x86_64 发行包](https://github.com/SKRbCrEsOg/Ark-Pets/releases?q=x86_64) |
| aarch64（ARM64） | [最新 aarch64 发行包](https://github.com/SKRbCrEsOg/Ark-Pets/releases?q=aarch64) |

#### 前置条件

- **KDE Plasma，Wayland 会话**（X11 会话下窗口控制不可用）
- 系统已装 **Qt 6**（提供 `qtpaths6`、`qdbus6`）
- 安装时需要 **`sudo`**（写入 Qt 插件目录）；日常运行不需要

#### 一、下载

从上面的发行包页面下载这三个文件（`INSTALL.md` 也在同一页面，建议一并下载）：

| 文件 | 说明 |
|:--|:--|
| `ArkPets-v3.13.1.zip` | 应用本体，**自带 JRE**，不需要系统 Java |
| `install-arkpets.sh` | 安装脚本 |
| `build-kwin-plugin.sh` | KWin 集成插件的本机编译脚本 |

#### 二、编译 KWin 集成插件（**这一步不能跳过**）

ArkPets 通过一个 KWin 插件来获取窗口位置、置顶桌宠、去掉边框。**该插件与本机 KWin 是 ABI 绑定的** ——
它链接 libkwin，而 KWin 的 C++ 类在版本之间会增删成员，libkwin 也只导出头文件所声明的一部分符号
（例如 `KWin::LayerShellV1Window` 完全没有导出符号）。**所以不存在适用于所有 KWin 版本的预编译插件。**

发行包里附带的 `ArkPetsIntegration2.so` 只是维护者在 KWin 6.7.4 上的参考构建。
**只要你的 KWin 不是这个版本，就必须在本机编译**：

```bash
bash build-kwin-plugin.sh
```

脚本会自动检测你的 KWin 版本与所需依赖；缺依赖时会按发行版打印准确的安装命令（Debian/Ubuntu、
Arch、Fedora、openSUSE、Gentoo、Void），也可以用 `--install-deps` 让它直接安装。

编译完成后，脚本会做一件关键的事 —— **ABI 校验**：检查插件未定义的每一个 `KWin::` 符号，都能在
本机 libkwin 的导出表里找到。**编译成功不代表能加载**，这一步才是真正能抓出「`kwin-dev` 过期或与
运行中的 KWin 不匹配」的检查：

```
==> ABI check against /usr/lib/x86_64-linux-gnu/libkwin.so.6
    21 KWin symbol(s) required by the plugin
    all of them are exported - the plugin matches this KWin
```

看到这一行才算成功。若报出缺失符号，说明 `kwin-dev` 与运行中的 KWin 不一致，先修好依赖再继续。

编译产物在 `build/bin/kwin/plugins/ArkPetsIntegration2.so`，把它复制到安装脚本旁边：

```bash
cp build/bin/kwin/plugins/ArkPetsIntegration2.so .
```

> 不想手动跑这一步，也可以直接把 `--install` 交给脚本：`bash build-kwin-plugin.sh --install`，
> 它会自行安装到 Qt 插件目录并请求运行中的 KWin 加载。

#### 三、安装

```bash
bash install-arkpets.sh --with-demo-model
```

脚本会做这些事：解压应用到 `~/.local/opt/ArkPets/`、写入配置（`window_system=KWIN`）、
把 `ArkPetsIntegration2.so` 装到 `$(qtpaths6 --query QT_INSTALL_PLUGINS)/kwin/plugins/`、
生成启动脚本与应用菜单项。

常用选项：

| 选项 | 作用 |
|:--|:--|
| `--with-demo-model` | 附带阿米娅演示模型，装完即可直接启动 |
| `--autostart` | 创建开机自启项 |
| `--no-plugin` | 只装应用本体，不动 KWin 插件 |
| `--plugin <path>` | 指定要安装的插件路径 |
| `--fresh` | 从零开始（丢弃现有模型、日志与配置） |

> **重复运行不会丢失用户数据。** 脚本会先把 `ArkPetsConfig.json`、模型与日志挪到一旁，装完再放回。
> 因此升级时直接重跑即可。

#### 四、注销并重新登录

KWin 只在启动时加载插件，**必须注销重登（或重启）一次**。

#### 五、启动

```bash
~/.local/opt/ArkPets/start-arkpets.sh        # 只启动桌宠
~/.local/opt/ArkPets/start-launcher.sh       # 打开启动器（下载/选择模型、改设置）
```

首次使用需要模型：在启动器的「模型」页面点「模型库管理 → 下载模型」。
若下载失败，可到 [ArkModels 模型仓库](https://github.com/isHarryh/Ark-Models) 手动下载压缩包后用
「导入压缩包」导入。

#### 验证安装是否成功

```bash
qdbus6 org.kde.KWin /ArkPets org.kde.KWin.ArkPets.List    # 应返回当前窗口列表而非报错
```

能返回窗口列表即说明插件工作正常。桌宠启动后可用这条命令确认它已被正确接管
（应看到 `keepAbove` / `skipTaskbar` 生效）：

```bash
qdbus6 --literal org.kde.KWin /ArkPets org.kde.KWin.ArkPets.List | grep ArkPets
```

#### 遇到问题时

- **桌宠沉进任务栏 / 悬浮在半空中**：在启动器「行为 → 位置」里调大「任务栏高度（下边界距离）」，
  点旁边的 `?` 有说明。这是某些外壳（如自绘的 Wayland 栏）没有正确向系统报告高度导致的。
- **桌宠不置顶 / 任务栏里有图标**：确认「选项 → 高级设置」里的「桌宠窗口置顶」已勾选；
  若勾了仍无效，通常是插件没加载成功，先做上面的「验证安装是否成功」。
- **插件加载失败**：`kwin-dev` 与运行中的 KWin 版本不一致是最常见原因，重跑第二步。
- 其余问题见发行包内的 `INSTALL.md` 第五节「故障排查」。

### 更多特性

以下列出了 ArkPets 的一些精彩特性，更多内容请在启动器中自行探索。

#### 桌宠相关

- **开机自启动** ：在启动器 “选项” 中可以设置开机自启动，设置后下一次电脑开机会自动生成最后一次启动的桌宠。
- **手动模式** ：托盘菜单中可以开启 “手动模式”，允许用户使用左右方向键来控制桌宠移动，或使用上下方向键来切换桌宠的动作。
- **透明模式** ：为防止用户在游戏、观看视频等情景下误触到桌宠，只需在托盘菜单中打开 “透明模式”，即可屏蔽桌宠和鼠标的一切交互（鼠标操作将穿透到下层窗口）。
- **下边界距离** ：桌宠在部分用户的电脑上无法正常检测任务栏位置（桌宠会沉入任务栏），此时您可以在 “行为” 页面手动设置任务栏高度。
- **高亮描边与阴影** ：高亮描边效果旨在复现游戏中基建系统的角色选中特效，阴影效果则提供了更加立体的视觉体验。在启动器 “选项” 中禁用这两种渲染特效可以降低性能消耗。

#### 启动器相关

- **公告栏** ：点击侧边栏中的 “公告” 按钮即可打开公告栏，用于显示更新日志等帮助信息。每次启动器启动时，如果发现有未读的重要公告，公告栏会自动弹出。
- **模型库联网下载与更新** ：在启动器 “模型” - “模型库管理” 面板中，您可以下载模型库和检查模型库更新。
- **软件更新** ：在启动器 “选项” 页面中，您可以检查软件更新并安装更新。
- **Mirror 酱接入** ：我们从 v3.9 版本开始接入了 [Mirror 酱](https://mirrorchyan.com/)，以提供高速的内容下载体验。

### 高级用法

在[快速上手](#快速上手)中介绍的是最简单和普遍的使用方法。除此之外：

- 您还可以下载 `zip` 版的程序压缩包解压，实现免安装使用。
- 如果您的电脑上存在 `JDK17` 的 [Java](https://www.java.com) 运行环境，您还可以下载 `jar` 版的程序文件直接运行（但无法使用开机自启动功能）。
- 如果您想用命令行启动桌宠，[点击查看](docs/CmdLine.md)说明。
- 如果您想添加自定义的模型，[点击查看](docs/CustomModel.md)说明。
- 如果您想使用直播流软件捕捉桌宠窗口，可以在启动器 “选项” 页面禁用 “桌宠作为后台程序启动”。

目前本程序暂未正式支持其他操作系统。Linux（aarch64 + KDE）已有实验性安装包（见上文），MacOS 支持仍在开发中。

## 关 于 <sub>About</sub>

### 鸣谢

感谢所有曾经为 ArkPets 的开发提供过各种形式的帮助的个人和组织。

- 另见 [鸣谢和第三方库说明](docs/Credits.md)

### 许可证

本项目基于 **GPL3协议**。任何人都可以自由地使用和修改项目内的源代码，前提是要在源代码或版权声明中保留作者说明和原有协议，且使用相同的许可证进行开源。

### 参与贡献

您可以通过提交 [Issues](https://github.com/isHarryh/Ark-Pets/issues) 等各种方式参与本项目的贡献。 提交 Issues 前，请确认您的议题与已有的议题不重复。提交 Issues 时，请您完整地填写议题模板。

- 另见 ~~很久没更新的~~ [开发者 Wiki](https://github.com/isHarryh/Ark-Pets/wiki)

### 友情链接

以下项目是 ArkPets 的关联或衍生项目：

- [isHarryh / Ark-Models](https://github.com/isHarryh/Ark-Models)：明日方舟 Spine 模型库
- [litwak913 / Ark-Pets-Integration](https://github.com/litwak913/Ark-Pets-Integration)：ArkPets 针对其他桌面系统的集成库
- [fuyufjh / ArkPets-Web](https://github.com/fuyufjh/ArkPets-Web)：ArkPets 在网页渲染器上的独立实现
- [isHarryh / Ark-Unpacker](https://github.com/isHarryh/Ark-Unpacker)：用于解包游戏资源的实用工具
- [Aloento / SuperSpineViewer](https://github.com/Aloento/SuperSpineViewer)：用于查看 Spine 模型的实用工具

-----

<div align="center">
   <p><i>GitHub Star History</i></p>
   <picture>
      <!--suppress HtmlUnknownTarget -->
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=isHarryh/Ark-Pets&type=date&theme=dark&legend=top-left&sealed_token=-hBNTJMB8JJLlN0Nzzg9Vd9lUkK3fVRMRHpHjy0cBTUzdR0niBpUbRQ8qVof2QRG1UC2k8L9OVvkra1_RHYwzV9IBgA2zY9nPbzZAsAUD3xfYHg5FtO-vw" />
      <!--suppress HtmlUnknownTarget -->
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=isHarryh/Ark-Pets&type=date&legend=top-left&sealed_token=-hBNTJMB8JJLlN0Nzzg9Vd9lUkK3fVRMRHpHjy0cBTUzdR0niBpUbRQ8qVof2QRG1UC2k8L9OVvkra1_RHYwzV9IBgA2zY9nPbzZAsAUD3xfYHg5FtO-vw" />
      <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=isHarryh/Ark-Pets&type=date&legend=top-left&sealed_token=-hBNTJMB8JJLlN0Nzzg9Vd9lUkK3fVRMRHpHjy0cBTUzdR0niBpUbRQ8qVof2QRG1UC2k8L9OVvkra1_RHYwzV9IBgA2zY9nPbzZAsAUD3xfYHg5FtO-vw" />
   </picture>
</div>
