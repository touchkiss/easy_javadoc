## Context

Easy Javadoc 的入口是 `GenerateJavadocAction.actionPerformed`，通过 `psiFile instanceof PsiJavaFile` / `KtFile` 决定路由。IntelliJ IDEA Ultimate 内置 `com.intellij.protobuf` 插件，提供 proto 文件的 PSI 树（`PbFile`、`PbMessageDefinition`、`PbSimpleField`、`PbServiceDefinition`、`PbMethodDefinition`、`PbEnumDefinition`、`PbEnumValue`）。

当前项目 `build.gradle` 的 `intellij.plugins = ['Kotlin', 'java']`，未声明 protobuf 依赖，因此 proto PSI 类无法编译引用。

## Goals / Non-Goals

**Goals:**
- 在 `.proto` 文件中，`ctrl+\` 对光标所在的 message / field / service / rpc / enum / enum-value 生成 `//` 翻译注释。
- 复用 `TranslatorService` 做名称翻译，与 Java 行为保持一致。
- 通过软依赖（`optional="true"`）保证插件在无 Protobuf 插件的 IDE 中不崩溃（CE 版本）。

**Non-Goals:**
- 不支持批量生成（`ctrl+shift+\` / `ctrl+alt+\`）——留待后续迭代。
- 不生成带有参数说明的多行注释（proto 无 `@param` 等 Javadoc 标签概念）。
- 不修改已有 Java/Kotlin 生成逻辑。

## Decisions

### D1：软依赖 vs. 硬依赖

**选择**：在 `plugin.xml` 中使用 `<depends optional="true">com.intellij.protobuf</depends>` 软依赖。

**理由**：IntelliJ IDEA Community Edition 不自带 Protobuf 插件。若改为硬依赖，CE 用户将无法安装本插件。软依赖允许插件在 CE 中正常运行（proto 功能静默禁用），仅在 protobuf 插件存在时激活相关功能。

**替代方案**：硬依赖 → 被否定，会阻断 CE 用户。

---

### D2：通过反射或直接导入 proto PSI 类

**选择**：直接 `import com.intellij.protobuf.lang.psi.*` 编译期依赖，在 `GenerateJavadocAction` 中用 `try/catch (NoClassDefFoundError)` 或在动态类加载时保护。

实际上，由于软依赖，IDE 加载插件时 protobuf 类可用（若插件安装），因此可以直接在新的 `protoProcess()` 方法中使用 proto PSI 类型，但该方法的调用入口需用 `psiFile` 类型的字符串名称做判断（`psiFile.getClass().getName().contains("PbFile")`），避免在 protobuf 插件未加载时触发 `NoClassDefFoundError`。

更简洁的方案：将所有 proto 逻辑隔离到独立的 `ProtobufDocHandler` 类中，在 `GenerateJavadocAction` 里通过 class-name 检测决定是否委托，让 JVM 懒加载该类。

**选择（最终）**：创建独立 `ProtobufDocHandler`，`GenerateJavadocAction` 仅通过 `"PbFile".equals(psiFile.getClass().getSimpleName().substring(0, Math.min(6, ...)))` 或更健壮的 `psiFile.getLanguage().getID().equals("protobuf")` 检测文件类型，再反射委托，实现物理隔离。

**更简洁方案（采用）**：直接用 `psiFile.getLanguage().getID()` 判断（`"protobuf"` 或 `"Protocol Buffers"`），隔离在 `ProtobufDocHandler` 中 import proto PSI，通过软依赖保证不崩溃。

---

### D3：注释格式

**选择**：生成单行 `// <翻译结果>` 注释，写在目标元素上方。

**理由**：Proto 社区惯例使用 `//` 注释；Protobuf 的文档工具（protoc-gen-doc 等）也识别 `//` 注释作为元素描述。多行 `/* */` 虽合法但不常见。

**替代方案**：`/* */` 块注释 → 被否定，不符合社区惯例。

---

### D4：注释写入方式

**选择**：使用 `WriteCommandAction.runWriteCommandAction` 直接操作 `PsiElement` 的文档，通过 `PsiParserFacade.getInstance(project).createLineCommentFromText(Language, text)` 或直接创建 `PsiComment` 并插入到目标元素前。

具体：`PsiParserFacade.getInstance(project).createLineCommentFromText(psiElement.getLanguage(), "// " + translatedText)` → 插入到 `psiElement` 的前一个兄弟节点位置（或直接 `psiElement.getParent().addBefore(comment, psiElement)`）。

---

### D5：支持的 proto 元素

支持：`PbMessageDefinition`、`PbSimpleField`、`PbServiceDefinition`、`PbMethodDefinition`、`PbEnumDefinition`、`PbEnumValue`。  
忽略：`PbFile`（文件级）、`PbOptionStatement`、`PbImportStatement` 等非业务元素。

元素名称提取：所有目标元素均实现 `PbNamedElement`，通过 `getName()` 获取。

## Risks / Trade-offs

- **PSI API 稳定性**：`com.intellij.protobuf` 插件版本与 IDEA 版本之间 API 可能变化 → 对 proto PSI 类的引用在编译时锁定为 IDEA 2024.1；后续升级 IDEA 版本时需回归测试。
- **语言 ID 不稳定**：`psiFile.getLanguage().getID()` 的返回值依赖 protobuf 插件实现，可能变化 → 同时检查文件扩展名（`.proto`）作为备用判断。
- **软依赖类加载**：若检测代码写错导致 `ProtobufDocHandler` 在 protobuf 插件未加载时被实例化，会抛出 `NoClassDefFoundError` 导致插件崩溃 → 所有 proto PSI 类的 `import` 必须限制在 `ProtobufDocHandler` 类内，`GenerateJavadocAction` 本身不 import 任何 proto 类。

## Migration Plan

无数据迁移需求。`build.gradle` 增加 plugin 依赖后需重新 `./gradlew build`；`plugin.xml` 软依赖不影响现有功能。

## Open Questions

无。
