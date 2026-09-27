# UI 约定与教训

新 UI 先复用 `ui/component/` 下的组件；改通用组件前读本页的教训，别把填过的坑再挖一遍。

## 组件

### AppDialog（全应用统一对话框壳）

- 大圆角 + surfaceContainerHigh，各处只换函数名替换原生 AlertDialog
- text 槽自动限高（55% 屏高）滚动 + 右缘细滚动条；**text 槽内禁止再套 verticalScroll**（嵌套滚动 → 无限高约束直接崩溃）
- 浮动菜单用 `AppDropdownMenu`（同观感）

### StepSlider（离散滑块）

- 两端 +/- 步进键、拖动吸附档位、边界自动禁用；页面缩放等场景直接用

### TriangleThumb（双三角 thumb）

- 上倒三角/下正三角夹住轨道，中间露出轨道颜色（thumb 填充色可跟随当前值，如色相滑块）

## 教训（都真实翻过车）

1. **滚动条子节点不能 fillMaxHeight**：AlertDialog 的 text 槽外层是 `weight(1f, fill=false)` 槽位，滚动条按份额取高会把槽撑满、按钮上方留半屏空洞；改 matchParentSize 又拿不到尺寸不绘制。**最终方案 drawBehind 直接画在容器上**（不参与测量、size 即视口、读状态自动重绘）
2. **Row 内容超出可用宽度会静默压缩末尾子项**（如主题色 7 个色块溢出，最后一个被压成竖条还点不到）：先算宽度预算，放不下就缩子项/间距，或换行
3. **M3 Slider 默认 thumb 两侧有白色 track gap**：要自定义观感时 track 画透明、用 Canvas 自绘，thumb 用 TriangleThumb
4. **对话框首帧位置**：Compose 对话框默认从顶部跳到中间（带系统动画时很难看）。解法是资源里同名覆盖 androidx 的 `DialogWindowTheme`/`FloatingDialogWindowTheme`，窗口动画换成纯 fade
5. **wrap 容器里给「跟随容器」的元素用 matchParentSize 不参与测量**，这是特性也是坑：希望它撑大容器时它不会
