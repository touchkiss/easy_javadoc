## Why

当光标停在类或文件上时，`cmd+\`（`GenerateJavadocAction`）只为类本身生成一条类级注释，无法一键批量生成类内所有字段和方法的 Javadoc。虽然 `cmd+shift+\`（`GenerateAllJavadocAction`）支持批量生成，但每次都需要弹出对话框勾选类型，操作流程较重。用户需要一个更轻量的快捷键，在类或文件上触发后，无需对话框直接批量生成所有成员（字段 + 方法）的 Javadoc。

## What Changes

- **新增** `GenerateClassMembersJavadocAction`：新的 Action 类，触发后直接为当前类（含内部类）所有字段和方法批量生成 Javadoc，不弹对话框。
- **新增快捷键** `ctrl+alt+\`（Windows/Linux）/ `cmd+alt+\`（macOS）：绑定到新 Action，与已有的 `ctrl+\`（单元素）和 `ctrl+shift+\`（带对话框批量）并存。
- **注册** 新 Action 到 `plugin.xml` 的 `<actions>` 块，并加入 `JavaGenerateGroup1`。

## Capabilities

### New Capabilities

- `batch-generate-class-members`: 在类或文件上触发快捷键时，静默批量为当前类所有方法和字段（递归含内部类）生成 Javadoc，无需对话框确认。

### Modified Capabilities

<!-- 无现有 spec 行为变更 -->

## Impact

- `src/main/java/com/star/easydoc/action/GenerateClassMembersJavadocAction.java`（新建）
- `src/main/resources/META-INF/plugin.xml`（新增 action 注册和快捷键）
- 复用现有 `JavaDocGeneratorServiceImpl`、`WriterService`、`PsiElementFactory` 等服务，无需修改 service 层
