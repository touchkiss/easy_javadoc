## 1. 新建 Action 类

- [x] 1.1 创建 `src/main/java/com/star/easydoc/action/GenerateClassMembersJavadocAction.java`，继承 `AnAction`
- [x] 1.2 注入 `JavaDocGeneratorServiceImpl` 和 `WriterService`（与现有 Action 一致）
- [x] 1.3 实现 `actionPerformed`：从 `AnActionEvent` 获取 `psiElement` 和 `psiFile`，过滤非 `PsiJavaFile` 的调用
- [x] 1.4 实现 `javadocProcess`：当 `psiElement` 为 `PsiClass` 时调用 `genMembersJavadoc`；当 `psiElement` 为非 `PsiClass` 时静默忽略
- [x] 1.5 实现 `genMembersJavadoc(project, psiClass)`：遍历 `getMethods()` 和 `getFields()`，调用 `saveJavadoc`，再递归处理 `getInnerClasses()`
- [x] 1.6 实现 `saveJavadoc(project, psiElement)`：调用 `docGeneratorService.generate()` + `WriterService.writeJavadoc()`，结果为空时跳过

## 2. 注册 Action 到 plugin.xml

- [x] 2.1 在 `src/main/resources/META-INF/plugin.xml` 的 `<actions>` 块内，紧跟 `javadoc.generatorAll` action 后新增 `javadoc.generateClassMembers` action 定义
- [x] 2.2 为新 action 添加 `<add-to-group group-id="JavaGenerateGroup1" anchor="after" relative-to-action="javadoc.generatorAll"/>`
- [x] 2.3 绑定 macOS keymap（`Mac OS X` 和 `Mac OS X 10.5+`）：`meta alt BACK_SLASH`
- [x] 2.4 绑定 Windows/Linux keymap（`Default for XWin` 和 `$default`）：`ctrl alt BACK_SLASH`

## 3. 验证

- [x] 3.1 执行 `./gradlew build` 确保编译通过，无新的编译告警
- [ ] 3.2 执行 `./gradlew runIde`，在沙箱 IDE 中打开一个包含字段和方法的 Java 类，光标置于类声明上，按 `cmd+alt+\`（macOS），验证所有方法和字段均生成了 Javadoc，类本身无新注释生成
- [ ] 3.3 在沙箱 IDE 中，光标置于方法或字段上按 `cmd+alt+\`，验证无任何响应（静默忽略）
- [ ] 3.4 在沙箱 IDE 中，打开含内部类的 Java 文件，验证内部类的方法和字段也递归生成了注释
- [ ] 3.5 验证 `ctrl+\` 和 `ctrl+shift+\` 的已有行为未受影响
