# 箱子模型编辑文件

当前游戏支持 `gold`、`orange`、`green`、`rose_blue`、`yellow_metal` 五种外观，默认 `gold`。可编辑 Blockbench 工程、原 PNG、Java 开闭快照、分件和动画源位于 `appearances/<外观>/`。运行时不需要 GeckoLib。

根目录的 `csgo_box.bbmodel`、`csgo_box.png` 和 `preview.png` 是已移除旧款做旧金箱的历史源文件，仅供归档，不再被游戏资源引用。

## 修改与导出

1. 用 Blockbench 打开 `appearances/<外观>/` 中对应工程并修改模型和贴图。
2. Java 闭合模型导出到 `src/main/resources/assets/csgobox/models/block/boxes/<外观>.json`，贴图放到 `textures/item/boxes/<外观>.png`。
3. 模型贴图引用保持 `csgobox:item/boxes/<外观>`。不要覆盖负责外观切换的物品入口 `models/item/csgo_box.json`。
4. 同步更新 `appearances/<外观>/java_models/parts/` 的源分件，再运行 `python tools/export_ground_models.py`。
5. 执行 `gradlew.bat build` 并使用生成的 JAR。

路径映射见 `appearances/import-manifest.json`。地面动画输出到 `models/block/animated/<外观>_body.json` 和 `<外观>_lid.json`，清单见 `ground-animation-manifest.json`。箱盖和锁扣绕后侧铰链旋转，保留原箱体几何、UV、底面及装饰面。若改变尺寸或铰链，需同步修改 `BoxAppearance` 的尺寸与 `hingeY()` / `hingeZ()`。

空箱和未开箱共用这些分件。滑条由客户端绘制在箱体上方，与水平面成 30°，无需添加模型面片。
