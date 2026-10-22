# 三个范例奖池

`box-appearances.json` 与新安装的默认配置一致，仅含原版奖励，没有配置箱子或钥匙的合成、掉落、村民交易。

| 奖池 ID | 名称 | 外观 | 消耗模式 | 所需物品 |
|---|---|---|---|---|
| `csgobox:example_weapons` | 武器箱 · 示例 | `gold` | 箱子和钥匙 | 普通钥匙 `csgobox:csgo_key0` |
| `csgobox:example_tools` | 工具箱 · 示例 | `orange` | 仅钥匙 | 钥匙 `csgobox:csgo_key1` |
| `csgobox:example_supplies` | 补给箱 · 示例 | `green` | 仅箱子 | 无需钥匙 |

每个示例按 7/5/3/2 的建议数量配置四个常规品质，另有两件传奇奖励。编辑器还可选择铜粉雾蓝和芥末黄金属外观。旧款做旧金箱已移除。

进入游戏后可从创造物品栏或编辑器的“获取测试箱子”获取。JEI 会显示这三个配置箱子，但不会显示未配置的获取配方。

```mcfunction
/give @s csgobox:csgo_box{PoolId:"csgobox:example_weapons"}
/give @s csgobox:csgo_key0
```

升级不会自动覆盖已有奖池。需要手工使用这些示例时，将数组对象合并到 `config/csbox/box.json`，保持 ID 唯一，再执行 `/csgobox reload`。
