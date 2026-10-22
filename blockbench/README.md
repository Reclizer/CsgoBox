CsgoBox 箱子模型编辑文件

打开方式
直接用 Blockbench 打开 csgo_box.bbmodel。项目类型为 Java 方块/物品，文件采用 Blockbench 4.10 项目格式，可由新版 Blockbench 读取，无需额外插件。

文件内容
- csgo_box.bbmodel：44 个模型部件、面 UV、原显示参数和内嵌原贴图。
- csgo_box.png：当前使用的 128×128 金色做旧贴图，可单独使用图像编辑器修改。
- preview.png：在 Blockbench 中打开工程后的参考截图。

模型分为“箱体与箱盖”（40 个部件）和“锁扣”（4 个部件），便于选取修改。原 UV 坐标分辨率 64×64、贴图像素尺寸 128×128 均已保留。修改和保存模型时，请同时保存贴图；单独发送 bbmodel 文件也能通过内嵌贴图正常打开。

导回模组
1. 在 Blockbench 中选择 文件 → 导出 → 导出方块/物品模型，导出为 csgo_box.json。
2. 替换源码内文件：
   src/main/resources/assets/csgobox/models/item/csgo_box.json
3. 将修改后的贴图保存为 csgo_box.png，替换：
   src/main/resources/assets/csgobox/textures/item/csgo_box.png
4. 导出模型中的贴图引用应保持 csgobox:item/csgo_box。当前工程已配置好命名空间 csgobox 和纹理文件夹 item。
5. 执行 gradlew.bat build 构建模组，再使用新 JAR。

物品入口通过 models/item/boxes/classic.json 使用该外观。从 1.11.0 起，地面方块改为分件动画渲染；修改模型后还需按下方「地面开合动画」说明重新导出分件。

如果明显改变箱子的长宽高，方块的选取/碰撞范围需要同步调整：
src/main/java/com/reclizer/csgobox/block/PlacedBoxBlock.java

验证
已在 Blockbench 5.2.2 中导入并导出用户提供的新 bbmodel；44 个部件的几何、面 UV 和旋转保持一致。8 个显示位置的变换等效保留，PNG 与新工程内嵌贴图的字节完全一致。1.6.1 已将新模型及贴图应用到游戏资源，外形尺寸与之前相同，沿用原碰撞范围。

多外观模型（1.8.0）
新增 appearances/，分别保留 gold、orange、green、rose_blue、yellow_metal 五款输入模型包的完整内容。每个目录包含带动画的 Blockbench 工程、Java 开闭快照、原 PNG、分部件和 GeckoLib 动画源；原 csgo_box.bbmodel / csgo_box.png 继续作为 classic 外观。

物品使用 Java 闭合快照，路径映射记录在 appearances/import-manifest.json。修改后请将对应 Java 模型导出到 src/main/resources/assets/csgobox/models/block/boxes/<appearance>.json，贴图放到 textures/item/boxes/<appearance>.png，并将模型的 textures 路径保持为 csgobox:item/boxes/<appearance>；地面动画分件也需同步更新。不要用输入包的同名 csgo_box.json 覆盖游戏物品入口，入口负责所有外观切换。


## 地面开合动画（1.11.0）

物品继续使用原闭合模型。地面箱使用 `src/main/resources/assets/csgobox/models/block/animated/<外观>_body.json` 与 `<外观>_lid.json`，箱盖与锁扣一起绕后侧铰链旋转，原贴图不变。

- 新五款外观的源分件在 `blockbench/appearances/<外观>/java_models/parts/`，铰链信息见各自 `model_manifest.json`；可编辑工程和动画源保留。
- 旧款 classic 从 `models/item/boxes/classic.json` 沿原箱盖分界拆分，并补上内部空腔。外观、贴图及背包模型保留。
- 新外观源分件中的封顶主体也会生成底板和内壁，保留原贴图；游戏中的内部空腔为箱内滑条留出可见空间。
- 修改源分件后运行 `python tools/export_ground_models.py`，重新生成游戏资源及 `blockbench/ground-animation-manifest.json`；保持原铰链位置即可直接应用。若移动铰链，同时调整 `BoxAppearance.hingeY()` / `hingeZ()`。
- 空箱与未开箱使用同一套地面动画资源，空箱物品模型为 `models/item/empty_box.json`。箱内滑条由客户端渲染，不需要在模型中加入 GUI 面片，也不依赖 GeckoLib。
