## Context

Easy Javadoc 目前有两个批注快捷键：
- `ctrl+\`（`GenerateJavadocAction`）：为光标所在的单个 PSI 元素生成注释；当元素是 `PsiClass` 时，仅生成类级注释。
- `ctrl+shift+\`（`GenerateAllJavadocAction`）：弹出 `GenerateAllView` 对话框，让用户勾选生成类/方法/字段/内部类，确认后批量生成。

新功能目标：新增第三个快捷键 `ctrl+alt+\`，在类或文件上触发后无对话框直接批量生成所有成员 Javadoc。

## Goals / Non-Goals

**Goals:**
- 新增 `GenerateClassMembersJavadocAction`，触发后自动为当前类所有方法、字段（及递归内部类）生成 Javadoc，不弹任何对话框。
- 绑定快捷键 `ctrl+alt+\`（Windows/Linux）/ `cmd+alt+\`（macOS）。
- 复用现有 `JavaDocGeneratorServiceImpl` 和 `WriterService`，不引入新服务依赖。
- 当光标在 `PsiClass` 或 `PsiFile`（Java 文件）上时触发有效。

**Non-Goals:**
- 不修改 `GenerateAllJavadocAction` 的已有行为。
- 不支持 Kotlin（KDoc）——KDoc 批量生成已有独立 TODO，不在本次范围。
- 不生成类本身的 Javadoc（仅生成成员：方法 + 字段）——与 `ctrl+\` 的类注释生成互补。
- 不新增任何持久化配置项。

## Decisions

### D1：新建独立 Action 类而非修改现有 Action

**选择**：新建 `GenerateClassMembersJavadocAction.java`。

**理由**：
- `GenerateJavadocAction` 已承担单元素生成 + 翻译 + 多光标等多个职责，继续扩展会降低可读性。
- `GenerateAllJavadocAction` 与对话框耦合，剥离会破坏已有配置持久化逻辑。
- 新类单一职责，便于独立测试和后续维护。

**替代方案**：在 `GenerateJavadocAction` 里用额外条件分支处理 → 被否定，因为会使该类变得更臃肿。

---

### D2：不弹对话框，默认生成方法 + 字段 + 递归内部类成员

**选择**：静默执行，无需用户确认；默认行为 = 生成所有方法 + 所有字段 + 递归内部类的方法和字段（不生成类本身注释）。

**理由**：
- 新快捷键的价值在于"快"，有对话框则退化为 `ctrl+shift+\` 的复制。
- 类本身的注释用 `ctrl+\` 生成，功能互补而不重叠。

**替代方案**：生成类注释 + 所有成员 → 被否定，与 `ctrl+\` 的类注释生成重叠，且用户更期望细粒度控制。

---

### D3：快捷键选择 `ctrl+alt+\` / `cmd+alt+\`

**选择**：`ctrl+alt+\`（Windows/Linux），`cmd+alt+\`（macOS），在已有 `\` 系列快捷键族中保持一致。

**理由**：与 `ctrl+\`、`ctrl+shift+\` 共享同一基础键，形成快捷键族，易于记忆。

**替代方案**：完全不同的快捷键组合 → 被否定，不符合已有键位习惯。

---

### D4：元素判断策略

当 `psiElement` 为 `PsiClass` 时直接处理；当 `psiElement` 为 `PsiJavaFile` 时取文件中所有顶级类处理。其他元素类型静默忽略（不报错）。

## Risks / Trade-offs

- **快捷键冲突**：`ctrl+alt+\` 可能与某些 IDE keymap 或系统快捷键冲突 → 通过 `plugin.xml` 按 keymap 分别注册，用户可在 IDE 设置中自行调整；影响可接受。
- **大文件性能**：类成员数量极大时串行生成可能较慢，尤其当翻译接口有网络调用时 → 现有 `GenerateAllJavadocAction` 也是串行，保持一致行为；后续可考虑并发优化，不在本次范围。
- **已有注释被覆盖**：`WriterService.writeJavadoc` 会替换已有注释 → 这是现有插件的一贯行为，非本次引入的新风险。

## Migration Plan

无数据迁移需求。新增文件和 `plugin.xml` 条目，不修改任何已有 Action 或 Service。直接发布新版本即可；回滚只需从 `plugin.xml` 移除新 action 注册。

## Open Questions

无。
