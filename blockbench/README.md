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

地面方块通过 models/block/csgo_box.json 继承上面的 item 模型，物品与地面箱子会同时更新。

如果明显改变箱子的长宽高，方块的选取/碰撞范围需要同步调整：
src/main/java/com/reclizer/csgobox/block/PlacedBoxBlock.java

验证
已在 Blockbench 5.2.2 中导入并导出用户提供的新 bbmodel；44 个部件的几何、面 UV 和旋转保持一致。8 个显示位置的变换等效保留，PNG 与新工程内嵌贴图的字节完全一致。1.6.1 已将新模型及贴图应用到游戏资源，外形尺寸与之前相同，沿用原碰撞范围。

多外观模型（1.8.0）
新增 appearances/，分别保留 gold、orange、green、rose_blue、yellow_metal 五款输入模型包的完整内容。每个目录包含带动画的 Blockbench 工程、Java 开闭快照、原 PNG、分部件和 GeckoLib 动画源；原 csgo_box.bbmodel / csgo_box.png 继续作为 classic 外观。

游戏当前使用 Java 闭合快照，路径映射记录在 appearances/import-manifest.json。修改后请将对应 Java 模型导出到 src/main/resources/assets/csgobox/models/block/boxes/<appearance>.json，贴图放到 textures/item/boxes/<appearance>.png，并将模型的 textures 路径保持为 csgobox:item/boxes/<appearance>。不要用输入包的同名 csgo_box.json 覆盖游戏物品入口，入口负责所有外观切换。
