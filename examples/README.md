# 箱体外观示例

`box-appearances.json` 是可直接读取的奖池数组，包含六个独立 ID；奖励仅使用原版物品，不需要 TaCZ 或 GeckoLib。各示例使用 `csgobox:csgo_key0`，`consumption` 为 `both`，每次消耗一个箱子和一把钥匙。合成、掉落和交易默认关闭，可在编辑器里配置。奖励页也可切换为 `box`（仅箱子）或 `key`（仅钥匙，保留箱体）。

| appearance | 外观 | 示例奖池 ID |
|---|---|---|
| `classic` | 保留的旧款做旧金箱 | `csgobox:example_classic` |
| `gold` | 新金色武器箱 | `csgobox:example_gold` |
| `orange` | 橙色武器箱 | `csgobox:example_orange` |
| `green` | 绿色扁平箱 | `csgobox:example_green` |
| `rose_blue` | 铜粉雾蓝箱 | `csgobox:example_rose_blue` |
| `yellow_metal` | 芥末黄金属箱 | `csgobox:example_yellow_metal` |

新安装时，默认 `config/csbox/box.json` 已包含这些示例。已有配置不会在升级时被覆盖；需要时，把本文件数组中的对象合并到现有 `box.json` 数组，保证 ID 唯一，然后执行 `/csgobox reload`。不要把两个完整数组直接拼接，也不要覆盖自己的奖池。

工作区的原配置已备份，并添加这六个示例。进入游戏后，在创造物品栏的模组分类或编辑器左侧目录中找到它们，编辑器可直接“获取测试箱子”。也可使用：

```mcfunction
/give @s csgobox:csgo_box{PoolId:"csgobox:example_gold"}
/give @s csgobox:csgo_key0
```

其他外观替换上述 `PoolId` 即可。编辑器的“箱体外观”页可以选择并预览外观，保存、复制及重新加载都会保留选择。已有背包箱子按奖池 ID 即时解析外观；已加载的地面箱子在一秒内同步模型和碰撞范围。

这次接入 Java 闭合状态模型。完整可编辑源、开盖快照和动画工程保存在 `blockbench/appearances/`，未启用额外的运行时箱盖动画，也不要求安装 GeckoLib。模型几何、UV、显示变换及 PNG 均保留输入包数据，运行资源仅调整了路径以避免冲突。
