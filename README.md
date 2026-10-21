# CsgoBox

Minecraft **1.20.1 / Forge 47.2.20 / Java 17** 的开箱模组。

## 构建和测试

Windows：

```powershell
$env:JAVA_HOME = '你的 JDK 17 目录'
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
```

Linux / macOS：

```sh
bash ./gradlew test build
bash ./gradlew runGameTestServer
```

模组文件在 `build/libs/csgobox-1.1.0.jar`。首次构建需要下载 Forge 和 Minecraft 依赖。
GameTest 在 `run-gametest/` 下使用独立的开发环境，请勿将该目录指向正式服务器。

## 配置与使用

服务端首次启动会生成 `config/csbox/box.json`。客户端不读取本地奖池配置。

- 右键主手的箱子查看奖池，点击开箱后由服务端扣除一个箱子和一把钥匙，再发放奖励。
- 奖励在播放动画前发放；关闭界面或正常断线不会取消发奖，也不能重新领取同一次奖励。
- 优先合并背包里已有的同类堆叠，只有剩余放不下的部分才掉落。
- 原有箱子仍按 `BoxItemInfo.name` 匹配服务端配置。新箱子只保存名字，旧箱子中的奖池、钥匙和权重不参与抽奖。
- 创造物品栏通过服务端同步的名称列表显示配置箱子；切换服务器时清空该列表。保存或重载后重新打开创造物品栏即可看到更新。
- 金色奖品在预览里只显示问号；预览会发送蓝、紫、粉、红等奖品信息，这些本来就是界面上公开展示的内容。

管理员命令（权限等级 2）：

```text
/csgobox reload
/csgobox give Weapons Supply Box
```

`give` 给执行命令的玩家一个指定箱子；名称可包含空格。编辑器物品也需要权限等级 2。
编辑器保存时从服务端 35 个槽位读取物品、NBT 和数量；同名保存只替换奖励，保留原有的钥匙、掉率、实体和权重设置。
编辑器关闭后归还放入的物品。新保存的配置即时生效。

## JSON 格式

兼容原有 `name/key/drop/random/entity/grade1…grade5` 字段。例如：

```json
[
  {
    "name": "示例箱",
    "key": "csgobox:csgo_key0",
    "drop": 0.12,
    "random": [2, 5, 6, 20, 625],
    "entity": ["minecraft:zombie"],
    "grade1": ["minecraft:diamond.withCount(3)"],
    "grade2": [],
    "grade3": [],
    "grade4": [],
    "grade5": ["minecraft:netherite_sword.withTags{Damage:0,Enchantments:[{id:\"minecraft:sharpness\",lvl:4s}]}"]
  }
]
```

- `random` 是相对权重，顺序仍为 **金、红、粉、紫、蓝**，不是百分比。空品质池不参与抽奖，其余有效权重重新归一化；权重为 0 的品质永远不会抽中。
- 同品质内部均匀抽取。幸运值对有正权重、非空的前四个品质增加 `2 × luck`（取整）权重，再统一计算总权重。负幸运值导致所有有效权重为零时回退到基础权重。
- `drop` 必须在 0 到 1 之间。
- 名称唯一且不超过 128 字符；请保持现有名称稳定，否则已有箱子找不到配置。
- `.withTags{...}` 使用原生 SNBT，无需 CraftTweaker。`.withCount(n)` 可在标签前或后，数量为 1 到该物品的最大堆叠数。
- 缺失物品、无效 NBT、负权重、空奖池及重复名称会报错。最多配置 512 个箱子，每个箱子最多 256 条奖励，奖励文本合计最多 65536 字符；文件上限 2 MiB。
- 重载先完整读取和校验，失败时继续使用旧配置；保存使用临时文件替换，且不会覆盖一个已经损坏的 JSON 文件。
- 热加载由 `/csgobox reload` 手动触发；没有文件轮询器。已打开的预览会失效，重新右键箱子即可。

## 升级说明

网络协议由 1 升到 2，**客户端和服务端都需要更新**。不兼容旧版客户端，以免继续使用旧的客户端发奖逻辑。
旧存档不需要批量转换物品，但对应名称必须存在于服务端配置。新版本移除了只用于临时界面数据的旧玩家 capability；真实背包物品不受影响。

重构范围、issue 对应关系和验证记录见 [REFACTOR.md](REFACTOR.md)。
