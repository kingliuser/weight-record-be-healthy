# 减重记录

一个纯本地的 Android 减重记录 App：填身高、体重、出生年份和计划每周减重，它反推出你每天的摄入上限和运动消耗下限，然后你每天手动记几笔，看热量账和体重账对不对得上。

## 隐私

- 零运行时权限，没有任何联网代码，数据只存在本机 Room 数据库里。
- 没有账号、没有后端、没有埋点、没有广告 SDK。卸载即清空。

## 功能

| 页面 | 做什么 |
| --- | --- |
| 今日 | 顶部大数字显示"今天还能吃多少 kcal"，下面分别记饮食和运动，可逐条删除 |
| 体重 | 每天一次称重，显示起始体重、当前体重、累计变化和距目标差值 |
| 月度 | 按月汇总摄入 / 消耗 / 净缺口，并把"热量推算应减"和"秤上实际减重"放在一起对照，差得多就提示可能漏记 |
| 成就 | 连掉七天、首个 5%、坚持控卡等进度，解锁状态持久化 |
| 设置 | 建档与改档，边填边实时给出 BMR / TDEE / 摄入上限 / 运动下限 / BMI 和不合理警告 |

目标不是你自己拍的数字，而是 `TargetRecommender` 按 Mifflin-St Jeor 公式和热量缺口换算出来的；每周减重超过 1 kg 会被自动压回上限。

## 模块结构

```
domain/   纯 Kotlin，无 Android 依赖：实体、仓库接口、TargetRecommender、AchievementEvaluator、EnergyMath
data/     Room 实现 domain 的仓库接口
app/      Compose UI + ViewModel + 手写的 AppContainer（无 Hilt / Koin）
```

`:app → :data → :domain`，依赖方向单向。业务规则全在 `:domain`，所以推荐引擎和成就判定用普通 JUnit 5 测，不需要 Robolectric 或仪器测试。

## 技术栈

这套版本组合是逐项验证过的，**升级前请确认四者互相兼容**（尤其别让 IDE 的升级弹窗顺手改掉）：

| 组件 | 版本 |
| --- | --- |
| Gradle | 9.8.2 |
| AGP | 9.4.1 |
| Kotlin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00（Material3 1.4.0） |
| Room | 2.8.5 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

UI 是 Material 3 薄荷绿主题，圆角 8-28dp 分五级；每个页面都拆成「ViewModel 壳 + 接收纯 state 的 `XContent` + `@Preview`」，所以在 Android Studio 里不装包也能直接看界面。

## 编译运行

前置：JDK 17-27（Android Studio 自带的 JBR 即可）、Android SDK 37。

```bash
./gradlew :app:assembleDebug          # 产物 app/build/outputs/apk/debug/app-debug.apk
./gradlew build                       # 编译 + 单测 + lint
./gradlew :domain:test                # 只跑业务规则测试
```

用 Android Studio 打开仓库根目录后，确认 `Settings → Build Tools → Gradle → Gradle JDK` 选的是 JDK 17 以上。这里有个坑：Studio 在启动 Gradle **之前**就校验 JVM，所以 `gradle.properties` 里的 `org.gradle.java.home` 对 Studio 无效，必须选对；同时工程 SDK 也不能停留在低版本 JDK，否则 sync 会直接报 `Incompatible Gradle JVM version`。

命令行则可以在 `~/.gradle/gradle.properties` 里写 `org.gradle.java.home=<你的 JDK 路径>`，对本机所有工程生效，不用污染仓库。

## 关于国内镜像

`settings.gradle.kts` 里把阿里云 Maven 排在 `mavenCentral()` / `google()` 前面，Gradle wrapper 的 `distributionUrl` 指向腾讯镜像。境外环境删掉这些镜像行即可，原始仓库都还留在后面兜底。

## License

MIT，见 [LICENSE](LICENSE)。
