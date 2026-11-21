# 第三方材料

`src/test/resources/fixtures/inject_clams-original.json` 是只读提取的 Critters and Companions 2.3.5 测试夹具。来源：安装版 `crittersandcompanions-forge-1.20.1-2.3.5.jar`，其 `META-INF/mods.toml` 声明作者 Joosh、EterDelta，许可证 MIT；仅用于证明补丁保留原始数据语义。它不打包进入发布候选 JAR。

`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 来源于 MinecraftForge 官方 1.20.1-47.4.29 MDK，Gradle wrapper 属于 Gradle 项目，使用 Apache License 2.0；保留脚本中的版权与许可头。原 MDK 存于 `tooling/`，不进入候选 JAR。

生产 JAR 只包含本项目Java类、模组元数据和pack.mcmeta，没有捆绑上述模组、测试夹具或测试探针。源码自身暂按 All Rights Reserved 标记，未擅自确定公开发布许可。

0.2.0新增的 `src/test/resources/fixtures/bottle-*.json` 来自当前安装的 Legendary Monsters／Legendary Delicacies 同名配方，`fixtures/fga/` 来自 Fallen Gems & Affixes 当前安装资源；仅为本地机制验证夹具，不进入候选 JAR。生产代码运行时只读取使用者已安装模组的资源，并在匹配指纹后生成内存覆盖，不捆绑上游JSON或类。0.2.0候选包含六个本项目Java类。

0.3.0新增的 `src/test/resources/fixtures/fga/spell_lightning.json`、`hemolith_shard.json`、`hemolith_shard_leggings.json` 同样只读提取自当前安装的 `fallen_gems_affixes-1.20.1-2.1.5.jar`，仅作单元测试夹具，不打包进入发布候选 JAR（0.3.0生产JAR仅包含本项目Java类与元数据）。
