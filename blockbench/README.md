CsgoBox 箱子模型编辑文件

打开方式
直接用 Blockbench 打开 csgo_box.bbmodel。项目类型为 Java 方块/物品，文件采用 Blockbench 4.10 项目格式，可由新版 Blockbench 读取，无需额外插件。

文件内容
- csgo_box.bbmodel：44 个模型部件、面 UV、原显示参数和内嵌原贴图。
- csgo_box.png：原始 128×128 PNG，可单独使用图像编辑器修改。
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
已在 Blockbench 5.2.2 中导入、导出并重新打开 bbmodel；44 个部件的几何、面 UV 和旋转保持一致。8 个显示位置的变换等效保留，原贴图字节完全一致。未修改游戏内当前使用的模型或贴图。
