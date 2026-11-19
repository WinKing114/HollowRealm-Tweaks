# HollowRealm Tweaks 0.1.0 实施记录

本轮授权：阿真要求开始制作模组，并选择“数据兼容首版：独立高优先级数据包，先做最小样例”。这批跨模组数据兼容归本模组维护，HollowRealm Fixes 不重复实现。仅写本目录；不改项目状态、第五份任务书、既有 Fixes 工程或正式实例。

目标：Java 17 / MC 1.20.1 / Forge 47.4.29。第一个完整样例只纠正 Critters and Companions 2.3.5 蛤蜊 GLM 的三个条件 ID，不改概率、掉落或配方；不纳入村正、商店、进度及未判定的 affix 内容。

实现：`hollowrealm_tweaks` 自有 mod ID，MOD 总线 `AddPackFindersEvent` 注册必选 TOP 独立 SERVER_DATA pack；从目标模组读取原资源，匹配 SHA-256 才生成内存覆盖。没有 Mixin、磁盘数据包或运行时新依赖。可选目标缺失、已修复、内容漂移均跳过。资源所有权只包含蛤蜊一条 GLM，不批量修复。

- [x] TDD：真实原始 JSON 输入→三个 ID 纠正，其他 JSON 内容不变；无目标／已修正／漂移不覆盖。红阶段缺实现记录在 evidence/test-red.log 与 test-pack-red.log；最终 4 项 JUnit 通过。
- [x] 构建：官方 MDK wrapper + 固定 ForgeGradle / Forge 版本；test、build、SRG 重映射与 JAR 内容检查通过；三个生产类均为 Java17（major61）。
- [x] 覆盖机制：真实 PackRepository 验证独立 TOP pack 胜出，CLIENT_RESOURCES 不暴露覆盖；真实专服再确认实际来源与重复加载。
- [x] 隔离加载：本目录全新专服环境，baseline／active／absent 各自测试世界；GLM 解码、512固定种子沉船／地牢抽样及 reload 通过。专服只监听本机，测试后正常关闭。
- [x] 交付源码、唯一候选 JAR、SHA-256、验证分层和恢复说明（正式实例未部署）。只读审查未发现 Critical／Important；实施清单过时的 Minor 已修正。
- [ ] 未验：完整正式模组组合、已有世界、客户端／多人、另外两张目标表的实际生成、自然开箱和玩家体验。

输入证据：项目第五份任务书与 `reports/2026-11-15-tweaks/TWEAKS-PLAN.md`；既有 `BUILTIN-DATA-VERDICT.md` 的普通内置覆盖失败证据；本轮只读原 JAR 中 `data/crittersandcompanions/loot_modifiers/inject_clams.json`，SHA-256 `4273eaa880484a7f3b3ee8567d45b780929822f98b64a785a6ccbce054449355`。该源码资源在本轮安装 JAR（MIT）中核实。

构建隔离：项目根不是 Git 仓库，故采用新的独立子目录，不初始化或更改其他仓库。官方 Forge 1.20.1 文档要求 Java 17；47.4.29 MDK 当前模板已切 FG7，本项目采用已确认仍可获取的 FG6.0.54 + 本机 Gradle8.10，避免升级全局工具。

已解决的构建问题：最初强制 Gradle daemon 的 file.encoding=UTF-8，与本机 Java17 参数文件默认 GBK 不一致，导致含“Modpack项目”的测试类路径被误解码。删除本工程的强制 daemon 编码后测试正常；源文件编译编码仍显式 UTF-8。未修改系统编码或全局 Java 设置。
