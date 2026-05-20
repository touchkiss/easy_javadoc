# Spec: Batch Generate Class Members Javadoc

## Purpose

Allow users to batch-generate Javadoc for all methods and fields of a Java class (and its inner classes) in a single keystroke, without any dialog. A new dedicated Action is registered in the IDE with its own keyboard shortcut (`ctrl+alt+\` on Windows/Linux, `cmd+alt+\` on macOS) and appears in the Generate menu.

## Requirements

### Requirement: 新快捷键触发批量成员注释生成
系统 SHALL 在 Java 文件或类上绑定新快捷键 `ctrl+alt+\`（Windows/Linux）/ `cmd+alt+\`（macOS），触发后无需对话框，直接批量生成当前类所有方法和字段的 Javadoc。

#### Scenario: 在类元素上触发快捷键
- **WHEN** 光标位于 `PsiClass` 元素（Java 类声明）上，用户按下 `ctrl+alt+\` / `cmd+alt+\`
- **THEN** 系统为该类的所有 `PsiMethod` 和 `PsiField` 生成 Javadoc 注释，不弹出任何对话框，不生成类本身的 Javadoc

#### Scenario: 在 Java 文件上触发快捷键
- **WHEN** 光标位于 `PsiJavaFile` 层级（文件节点），用户按下 `ctrl+alt+\` / `cmd+alt+\`
- **THEN** 系统取文件中所有顶级类，对每个顶级类执行与"在类元素上触发"相同的批量成员注释生成

#### Scenario: 递归处理内部类成员
- **WHEN** 被处理的类包含一个或多个内部类（`innerClasses`）
- **THEN** 系统递归地对每个内部类也执行相同的字段和方法 Javadoc 生成

#### Scenario: 非类元素上触发快捷键
- **WHEN** 光标位于方法、字段或其他非 `PsiClass` / `PsiJavaFile` 元素，用户按下 `ctrl+alt+\` / `cmd+alt+\`
- **THEN** 系统静默忽略此次调用，不生成任何注释，不报错

### Requirement: 复用现有生成服务
系统 SHALL 使用 `JavaDocGeneratorServiceImpl.generate()` 和 `WriterService.writeJavadoc()` 完成实际注释写入，不引入新的服务依赖。

#### Scenario: 调用生成服务
- **WHEN** 新 Action 需要为某字段或方法写入 Javadoc
- **THEN** 系统调用 `JavaDocGeneratorServiceImpl.generate(psiElement)` 获取注释文本，再通过 `WriterService.writeJavadoc()` 写入 PSI 树

#### Scenario: 生成结果为空时跳过
- **WHEN** `JavaDocGeneratorServiceImpl.generate()` 返回空字符串或 null
- **THEN** 系统跳过该元素，继续处理下一个，不写入任何内容

### Requirement: 新 Action 注册到 plugin.xml
系统 SHALL 在 `plugin.xml` 的 `<actions>` 块中注册新 Action，并加入 `JavaGenerateGroup1`，使其出现在 IDE 的 Generate 菜单中。

#### Scenario: Action 在 Generate 菜单中可见
- **WHEN** 用户在 Java 文件编辑器中打开 "Generate" 菜单（Alt+Insert / Cmd+N）
- **THEN** 新 Action "Generate Members Javadoc"（或类似名称）出现在菜单列表中

#### Scenario: 快捷键按 keymap 分别注册
- **WHEN** plugin.xml 中的 action 定义被解析
- **THEN** `Mac OS X`、`Mac OS X 10.5+` keymap 绑定 `meta alt BACK_SLASH`；`Default for XWin` 和 `$default` keymap 绑定 `ctrl alt BACK_SLASH`
