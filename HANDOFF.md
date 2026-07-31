# Bluewire ARGB 动态变色 - 交接文档

## 项目位置

```
C:\Users\90389\.kun\default_workspace\Bluewire
```

GitHub: `Walkersifolia/Bluewire` 分支 `1.20`

## 功能现状

| 功能 | 状态 |
|------|------|
| 自定义高/低功率静态颜色 | ✅ 正常 |
| 信号强度自动渐变 | ✅ 正常 |
| ARGB 炫彩开关 | ✅ 配置页面可用 |
| ARGB 动态变色（颜色随时间流动） | ❌ 未解决 |

## 当前版本

`1.0.2-debug2`（gradle.properties 中）

## 架构概览

### 两套颜色路径

**Path A - 静态 tint（Mixin 烘焙，✅ 工作）**
- `src/main/java/com/example/mixin/ExampleMixin.java`
- `@Overwrite RedstoneWireBlock.getWireColor()` → `BluewireConfig.getWireColor()`
- 彩虹模式下返回 `0xFF888888`（中性灰），由 Path B 提供动态颜色

**Path B - 动态覆盖层（每帧渲染，⚠️ 部分工作）**
- `src/client/java/com/example/config/RainbowOverlayRenderer.java`
- 当前注册在 `WorldRenderEvents.BEFORE_DEBUG_RENDER`
- 红石位置缓存到 `LongOpenHashSet`

---

## 诊断会话关键发现（本轮）

### ✅ 已验证可用的渲染路径

**唯一在 Sodium 0.4.10 下工作的路径：**

```
VertexConsumerProvider.Immediate (自建 BufferBuilder)
  → provider.getBuffer(RenderLayer)
  → provider.draw()
```

此路径经过了以下阶段验证：

| 版本 | 阶段 | RenderLayer | flush | 结果 |
|------|------|-------------|-------|------|
| v5 | BEFORE_DEBUG_RENDER | getLines + getDebugFilledBox | 单次 | ✅ 相机前方闪烁方块可见 |
| v8 | BEFORE_DEBUG_RENDER | getDebugFilledBox | 单次 | ✅ 品红线条满天飞（TRIANGLE_STRIP连通问题） |
| v10 | BEFORE_DEBUG_RENDER | getLines + drawBox | 单次 | ✅ 每个红石位置均现线框（但偏移） |

### ❌ 已验证不可用的路径

| 路径 | 失败原因 |
|------|---------|
| `Tessellator + drawWithGlobalProgram + POSITION_COLOR` | Sodium 渲染状态冲突（v5/v7 验证） |
| `context.consumers()` + 依赖游戏 flush | 图形不出现（v4 验证） |
| 每红石独立 Immediate + flush（5397次/帧） | 渲染状态损坏 → 固定屏幕位置（v9 验证） |
| `RenderLayer.getTranslucent()` | 顶点格式复杂且不工作 |
| 自定义 RenderLayer（MultiPhase builder） | Yarn 映射中 API protected 不可访问 |

### ⚠️ 坐标系统问题（未解决）

v10（相机相对坐标 `bx - camPos.x`）：线框出现在红石位置附近，但随玩家位移偏移
v11（世界坐标 `bx`）：完全无可见输出

推测：modelView 矩阵在 `BEFORE_DEBUG_RENDER` 阶段与 `getLines()` 的 `startDrawing()` 设置之间存在不匹配。

---

## 已尝试方案汇总

| # | 方案 | 结果 |
|---|------|------|
| 1 | `Tessellator + drawWithGlobalProgram + POSITION_COLOR` | ❌ Sodium 冲突 |
| 2 | `Immediate + getTranslucent()` | ❌ 不可见 |
| 3 | `context.consumers() + getLines()` | ❌ 不可见 |
| 4 | `Immediate + getLines() + drawBox` 相机相对坐标 | ⚠️ 可见但偏移 |
| 5 | `Immediate + getLines() + drawBox` 世界坐标 | ❌ 不可见 |
| 6 | `Immediate + getDebugFilledBox()` 单 buffer 所有顶点 | ⚠️ 可见但 TRIANGLE_STRIP 连通 |
| 7 | `Immediate + getDebugFilledBox()` 每红石独立 flush | ❌ 5397次flush损坏状态 |
| 8 | 自定义 RenderLayer (MultiPhase builder) | ❌ API 不可访问 |
| 9 | `scheduleBlockRenders()` 强制区块重建 | ❌ 无效 |

---

## 文件清单（当前状态）

```
src/main/java/com/example/
├── ExampleMod.java              ─ 模组入口
├── mixin/ExampleMixin.java      ─ Path A：覆写红石颜色
└── config/BluewireConfig.java   ─ 配置读写、颜色计算；彩虹模式返回 0xFF888888

src/main/resources/
├── fabric.mod.json              ─ 模组元数据
├── bluewire.mixins.json         ─ Mixin 配置
└── assets/bluewire/icon.png

src/client/java/com/example/
├── ExampleModClient.java        ─ 客户端入口，含 [BluewireBuild] 日志
└── config/
    ├── BluewireConfigScreen.java
    ├── ModMenuIntegration.java
    └── RainbowOverlayRenderer.java ─ Path B：当前版本 v11（世界坐标）

src/client/resources/
└── bluewire.client.mixins.json  ─ 已清空（移除了不存在的 WorldRendererAccessor）
```

## 构建

```bash
cd C:\Users\90389\.kun\default_workspace\Bluewire
./gradlew build --no-daemon
# JAR: build/libs/bluewire-1.0.2-debug2.jar
```

## 用户环境

- Minecraft 1.20.1 Fabric
- Fabric Loader 0.14.22, Fabric API 0.88.1
- Sodium 0.4.10, Indium 1.0.18, Iris 1.6.4（shaders disabled）
- 173 个模组
- 游戏目录：`D:\Desktop\1.20\.minecraft`
- 日志：`D:\Desktop\1.20\.minecraft\logs\latest.log`
- JAR 需手动复制到 `mods/`（不可自动写入）

## 剩余关键问题

1. **坐标系统**：相机相对坐标偏移，世界坐标不可见。可能需要在绘制前检查并手动设置 RenderSystem 的 modelView 矩阵
2. **填色覆盖层 vs 线框**：`getDebugFilledBox()` 可产生填色方块但有 TRIANGLE_STRIP 连通问题；需要退化三角形分离或批量 flush
3. **Alpha/混合**：最终覆盖层应使用 alpha≈0.5 半透明混合，使红石纹理可见
