# HollowRealm Tweaks 0.2.0 实施

阿真 2026-11-16 授权扩展功能／修复，为发布打磨；沿用已选择的数据兼容路线。本轮仅本工程写入，保护正式实例、Fixes 工程、根状态与任务书。0.1.0 源码与产物已在 backups/ 快照。

目标版本仍为 MC1.20.1 / Forge47.4.29 / Java17。本次不新增依赖、Mixin、配置覆盖或玩法调参。

- [x] 红测试：湮灭瓶只接受两份已审计资源指纹；输出与主模组正确配方逐字相同，8 个产出及形状不变。任一漂移、附属已修正均不覆盖。
- [x] 红测试：4 个枪械词缀仅加 Forge `mod_loaded(scguns)` 条件；保留全部数值。仅 Scguns 缺失、Placebo8.6.3、原资源指纹匹配时生成覆盖。
- [x] 实现：保留原 pack ID，按目标独立收集覆盖，无目标／读取失败／漂移只跳过该项；单项失效不得阻止其他项注册。
- [x] 验证：8项JUnit、实际SRG JAR专服 baseline / active / absent，资源来源、真实配方匹配与产出8、错误材料拒绝、Placebo原生条件跳过／AffixRegistry不含无效项、reload、蛤蜊50/0回归；其余98个词缀key逐项一致。
- [x] 打包与只读审查；README、CHANGELOG及发布前清单已更新；旧候选完整归档，新候选唯一；正式实例不部署。代码审查无Critical／Important。
- [ ] 未验：正式283模组组合、KubeJS／JEI链路、客户端／多人、已有世界／外部pack、自然遭遇及玩家体验。

证据：evidence/0.2.0/input-baseline.json 记录 11-16 最新日志指纹、报错行、283正式JAR指纹与保护文档指纹。Placebo8.6.3 安装版字节码明确 DynamicRegistry 解码前调用 JsonUtil.checkConditions→CraftingHelper.processConditions，支持 conditions / forge:conditions；不能用空 JSON 伪装禁用。

已确认现状：正式实例现有0.1.0与此前交付哈希一致，当前日志无蛤蜊错误；不冒认本会话部署。当前残留湮灭瓶4条配方日志与4个gun affix失败，为本轮对象；其余资源／脚本错误不在本批覆盖范围。
