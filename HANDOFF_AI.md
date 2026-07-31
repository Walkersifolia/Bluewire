# Bluewire — AI 模型交接文档

> **目标**：Minecraft 1.20.1 Fabric 模组，为红石粉实现 ARGB 动态变色（颜色随时间流动）。
> **当前状态**：渲染管线已验证可行，剩余坐标偏移问题待解决。
> **上一模型停止点**：v11（世界坐标）无可见输出，v10（相机相对坐标）线框可见但偏移。

---

## 1. 你要解决的问题

红石粉在开启"ARGB 炫彩"后应该是**每帧颜色都不同**的彩虹流动效果。当前：
- Path A（Mixin 烘焙的静态颜色）✅ 工作 — 已改为彩虹模式下返回中性灰 `0xFF888888`
- Path B（每帧动态覆盖层）⚠️ 渲染管线已验证可行，但坐标位置不正确

## 2. 项目结构

```
C:\Users\90389\.kun\default_workspace\Bluewire\
├── build.gradle                    # Fabric Loom 1.3, MC 1.20.1
├── gradle.properties               # mod_version=1.0.2-debug2
├── HANDOFF.md                      # 人类可读的交接文档
└── src/
    ├── main/java/com/example/
    │   ├── ExampleMod.java               # 入口，加载配置
    │   ├── mixin/ExampleMixin.java       # @Overwrite RedstoneWireBlock.getWireColor()
    │   └── config/BluewireConfig.java    # 配置 + 颜色计算（彩虹→0xFF888888）
    └── client/java/com/example/
        ├── ExampleModClient.java         # 客户端入口 + [BluewireBuild] 日志
        └── config/
            └── RainbowOverlayRenderer.java  # 动态覆盖层（你需要修改的文件）
```

## 3. 已验证的关键事实

### ✅ 渲染管线（Sodium 兼容）
**唯一在 Sodium 0.4.10 下工作的路径：**
```java
BufferBuilder builder = new BufferBuilder(capacity);
VertexConsumerProvider.Immediate provider = VertexConsumerProvider.immediate(builder);
VertexConsumer vc = provider.getBuffer(RenderLayer.xxx);
// ... 提交顶点 ...
provider.draw();  // 单次 flush，内部调 layer.startDrawing() → drawWithGlobalProgram → endDrawing()
```

### ❌ 不可用的路径
- `Tessellator + RenderSystem.setShader() + BufferRenderer.drawWithGlobalProgram()` — Sodium 冲突
- `context.consumers()` — 不产生可见像素
- 超过 ~100 次 `provider.draw()/帧` — 渲染状态损坏
- 自定义 `RenderLayer`（`MultiPhaseParameters.builder()`）— API protected

### ✅ 可用的 RenderLayer
| Layer | DrawMode | 深度测试 | 透明 | 说明 |
|-------|----------|---------|------|------|
| `getLines()` | LINES | 有 | 无 | 独立线段，`WorldRenderer.drawBox()` 配合使用 |
| `getDebugFilledBox()` | TRIANGLE_STRIP | **无** | 有 | 填色，4顶点=1方块，**但跨四边形连通** |

### ✅ 可用的渲染阶段
`WorldRenderEvents.BEFORE_DEBUG_RENDER` — 此阶段 RenderSystem 状态适合线框/调试渲染

### ❌ 不可用的阶段
`WorldRenderEvents.AFTER_TRANSLUCENT` — 配合 getLines 无输出

## 4. 坐标系统问题（核心待解决）

### 实验记录

| 版本 | 坐标方式 | MatrixStack | 结果 |
|------|---------|-------------|------|
| v5 | 相机相对 (cx-camX) | 自建 + translate | ✅ 相机前方方块可见 |
| v8 | 相机相对 (bx-camX) | 自建 + translate | ⚠️ 线框满天飞（TRIANGLE_STRIP连通）但位置正确 |
| v10 | 相机相对 (bx-camX) | 自建 + translate | ⚠️ 每个红石位置线框可见，但随玩家位移偏移 |
| v11 | **世界坐标 (bx)** | 自建 + translate | ❌ 完全无可见输出 |

### 推测根因

v10 偏移量约等于相机世界坐标。说明 modelView 矩阵中已经有 `translate(-camPos)`，而代码又手动减了一次 camPos → 双倍偏移。

v11 用世界坐标后消失，可能是因为 modelView 在 `BEFORE_DEBUG_RENDER` + `getLines().startDrawing()` 后状态与预期不同。

### 建议排查方向

1. **在 `provider.draw()` 之前手动设置 modelView 矩阵**：保存 → 设为单位矩阵或含相机变换的矩阵 → draw → 恢复
2. **尝试 `context.matrixStack()` 而非自建**：在 v4 中试过但配合 world 坐标无输出；配合相机相对坐标可能有效
3. **尝试在 `getLines().startDrawing()` 后立即 `RenderSystem.disableDepthTest()` 再手动绘制**（不用 Immediate，手动管理 buffer）

## 5. 当前代码关键片段

`RainbowOverlayRenderer.java` v11 的核心逻辑：
```java
// 注册：BEFORE_DEBUG_RENDER
// 红石扫描：ClientChunkEvents + ClientTickEvents → wirePositions (LongOpenHashSet)

private static void drawOverlay(Camera camera, BluewireConfig cfg, MinecraftClient client) {
    float timeSec = System.currentTimeMillis() / 1000.0F;
    float speed = cfg.rainbowSpeed;

    BufferBuilder builder = new BufferBuilder(524288);
    VertexConsumerProvider.Immediate provider = VertexConsumerProvider.immediate(builder);
    VertexConsumer vc = provider.getBuffer(RenderLayer.getLines());
    MatrixStack ms = new MatrixStack();

    for (long packed : wirePositions) {
        // ... 取 bx,by,bz, power ...
        float hue = (timeSec * speed * 360.0F + power * 12) % 360.0F;
        int color = hsvToRgb(hue, 1.0F, 1.0F);  // 动态彩虹色 ✅

        ms.push();
        ms.translate(bx, by + 0.025f, bz);  // 世界坐标
        WorldRenderer.drawBox(ms, vc, 0,0,0, 1,0.05f,1, r,g,b, 1);
        ms.pop();
    }
    provider.draw();  // 单次 flush
}
```

## 6. 恢复目标路径

一旦坐标位置正确锁定到红石粉上，下一步是：
1. 用 `getDebugFilledBox()` 替代 `getLines()` 产生填色方块（需解决 TRIANGLE_STRIP 连通：插入退化三角形或分批 flush）
2. alpha 调为 0.5~0.6 半透明叠加
3. 移除线框调试代码

## 7. 构建 & 测试

```bash
cd C:\Users\90389\.kun\default_workspace\Bluewire
./gradlew build --no-daemon
# 产物：build/libs/bluewire-1.0.2-debug2.jar
```

用户手动复制到：`D:\Desktop\1.20\.minecraft\mods\`

游戏日志：`D:\Desktop\1.20\.minecraft\logs\latest.log`
搜索关键词：`[BluewireBuild]`、`[RainbowRenderStage]`

## 8. 环境

- Minecraft 1.20.1, Fabric Loader 0.14.22, Yarn 1.20.1+build.9
- Fabric API 0.88.1, Sodium 0.4.10, Indium 1.0.18, Iris 1.6.4 (shaders off)
- Cloth Config 11.1.106, ModMenu 7.2.2
- Java 17, Gradle 8.2, Fabric Loom 1.3
