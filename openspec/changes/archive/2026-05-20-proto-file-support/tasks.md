## 1. 构建依赖配置

- [x] 1.1 在 `build.gradle` 的 `intellij.plugins` 列表中追加 `"protoeditor"` 以引入 proto PSI 编译依赖
- [x] 1.2 在 `src/main/resources/META-INF/plugin.xml` 中添加软依赖 `<depends optional="true">idea.plugin.protoeditor</depends>`

## 2. 实现 ProtobufDocHandler

- [x] 2.1 创建包目录 `src/main/java/com/star/easydoc/proto/`
- [x] 2.2 创建 `ProtobufDocHandler.java`，注入 `TranslatorService`
- [x] 2.3 语言 ID 检测策略：`GenerateJavadocAction` 用 `"protobuf".equalsIgnoreCase(psiFile.getLanguage().getID())` 检测，无 proto import；`ProtobufDocHandler` 在独立类中隔离所有 proto PSI import，通过 JVM 懒加载保护 CE 环境
- [x] 2.4 实现 `void handle(Project, PsiFile, PsiElement)`：用 `PsiTreeUtil.getParentOfType(element, PbNamedElement.class, false)` 找最近具名元素
- [x] 2.5 翻译逻辑内联在 `handle()` 中：调用 `TranslatorService.autoTranslate(name, psiElement)`，结果为空则静默返回
- [x] 2.6 实现私有 `writeComment()`：通过 `WriteCommandAction` + `Document.insertString` 将 `// comment` 写入元素上方，保留缩进

## 3. 修改 GenerateJavadocAction

- [x] 3.1 在 `GenerateJavadocAction.actionPerformed` 中增加语言 ID 检测：`"protobuf".equalsIgnoreCase(psiFile.getLanguage().getID())`，调用私有 `protoProcess` 方法委托处理
- [x] 3.2 通过 JVM 懒加载实现保护：`protoProcess()` 仅在语言 ID 为 `"protobuf"` 时执行，该 ID 仅在 protoeditor 插件安装时存在，`ProtobufDocHandler` 因此只在安全时被加载

## 4. 验证

- [x] 4.1 执行 `./gradlew build` 确保编译通过，无新编译错误
- [ ] 4.2 执行 `./gradlew runIde`，在沙箱 IDE 中打开一个 `.proto` 文件，光标置于 `message` 声明关键字上，按 `cmd+\`，验证在 message 上方插入了 `//` 翻译注释
- [ ] 4.3 验证 `field`、`service`、`rpc`、`enum`、`enum value` 各自均可生成注释
- [ ] 4.4 验证 `import` / `syntax` / `option` 等非具名元素上触发时无任何响应
- [ ] 4.5 验证 `ctrl+\` 在 Java 文件上的行为未受影响
