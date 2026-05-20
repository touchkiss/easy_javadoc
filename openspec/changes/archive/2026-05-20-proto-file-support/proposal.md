## Why

Easy Javadoc 目前只对 Java（`PsiJavaFile`）和 Kotlin（`KtFile`）文件响应快捷键，在 `.proto` 文件中触发 `ctrl+\` 完全无效。随着 gRPC 服务在 Java 项目中大量使用，开发者需要为 proto 的 `message`、`field`、`service`、`rpc`、`enum` 等元素补充注释，缺乏工具支持导致 proto 注释质量差或缺失。

## What Changes

- **新增** `ProtobufDocGeneratorService`：为 proto PSI 元素（message/field/service/rpc/enum/enum-value）生成 `//` 风格的单行注释，内容来自 `TranslatorService` 对元素名的翻译。
- **新增** `ProtobufWriterService`（或扩展 `WriterService`）：将生成的 `//` 注释写入 proto 文件，利用 `WriteCommandAction` 在元素上方插入注释。
- **修改** `GenerateJavadocAction.actionPerformed`：增加 `psiFile instanceof PbFile` 分支，路由到新的 proto 处理逻辑。
- **修改** `build.gradle`：在 `intellij.plugins` 中追加 `"com.intellij.protobuf"` 以获取 proto PSI API。
- **修改** `plugin.xml`：添加 `<depends optional="true" config-file="...">com.intellij.protobuf</depends>` 软依赖，保持插件在无 Protobuf 插件环境中可用。

## Capabilities

### New Capabilities

- `proto-comment-generation`: 在 `.proto` 文件中，光标停留在 message/field/service/rpc/enum/enum-value 元素上时，触发快捷键可生成对应的 `//` 翻译注释。

### Modified Capabilities

<!-- 无现有规范行为变更 -->

## Impact

- `src/main/java/com/star/easydoc/action/GenerateJavadocAction.java`（新增 proto 分支）
- `src/main/java/com/star/easydoc/proto/`（新建包，含 `ProtobufDocGeneratorService` 和 `ProtobufWriterService`）
- `build.gradle`（新增 `com.intellij.protobuf` 插件依赖）
- `src/main/resources/META-INF/plugin.xml`（新增软依赖声明）
- 复用 `TranslatorService` 进行名称翻译，无需修改翻译层
