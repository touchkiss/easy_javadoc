## ADDED Requirements

### Requirement: Proto 元素注释生成
系统 SHALL 在 `.proto` 文件中响应 `ctrl+\` / `cmd+\` 快捷键，对光标所在的 proto 具名元素（message、field、service、rpc、enum、enum-value）生成 `//` 风格的单行注释，注释内容来自 `TranslatorService` 对元素名的翻译。

#### Scenario: 在 message 上触发
- **WHEN** 光标位于 `PbMessageDefinition`（proto message 声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该 message 声明上方插入 `// <翻译后的 message 名>` 注释

#### Scenario: 在 field 上触发
- **WHEN** 光标位于 `PbSimpleField`（proto 字段声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该字段声明上方插入 `// <翻译后的 field 名>` 注释

#### Scenario: 在 service 上触发
- **WHEN** 光标位于 `PbServiceDefinition`（proto service 声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该 service 声明上方插入 `// <翻译后的 service 名>` 注释

#### Scenario: 在 rpc 上触发
- **WHEN** 光标位于 `PbMethodDefinition`（proto rpc 声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该 rpc 声明上方插入 `// <翻译后的 rpc 名>` 注释

#### Scenario: 在 enum 上触发
- **WHEN** 光标位于 `PbEnumDefinition`（proto enum 声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该 enum 声明上方插入 `// <翻译后的 enum 名>` 注释

#### Scenario: 在 enum value 上触发
- **WHEN** 光标位于 `PbEnumValue`（proto enum 值声明）上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统在该 enum 值声明上方插入 `// <翻译后的 enum value 名>` 注释

#### Scenario: 非具名元素上触发
- **WHEN** 光标位于 import/option/syntax 等非具名元素上，用户按下 `ctrl+\` / `cmd+\`
- **THEN** 系统静默忽略，不插入任何注释

### Requirement: 软依赖保护
系统 SHALL 通过 protobuf 插件软依赖，保证在未安装 `com.intellij.protobuf` 插件的 IDE（如 Community Edition）中正常运行，不崩溃、不报错，仅静默禁用 proto 功能。

#### Scenario: Protobuf 插件未安装时触发快捷键
- **WHEN** 用户在未安装 `com.intellij.protobuf` 插件的 IDE 中打开 `.proto` 文件并按 `ctrl+\`
- **THEN** 系统不做任何响应，不抛出异常，不影响其他文件类型的功能

#### Scenario: Protobuf 插件已安装时正常工作
- **WHEN** 用户在已安装 `com.intellij.protobuf` 插件的 IDE（IntelliJ IDEA Ultimate）中操作
- **THEN** proto 注释生成功能正常可用

### Requirement: 名称翻译复用
系统 SHALL 使用现有 `TranslatorService.autoTranslate(name, psiElement)` 对 proto 元素名进行翻译，与 Java/Kotlin 注释生成保持一致的翻译逻辑。

#### Scenario: 元素名为英文（驼峰/下划线）
- **WHEN** proto 元素名为 `getUserInfo` 或 `get_user_info`
- **THEN** 系统调用 `TranslatorService.autoTranslate`，将元素名转换为可读描述并写入注释

#### Scenario: 翻译结果为空时跳过
- **WHEN** `TranslatorService.autoTranslate` 返回空字符串
- **THEN** 系统不插入任何注释，静默跳过

### Requirement: 注释幂等写入
系统 SHALL 在目标元素上方已存在 `//` 注释时，仍允许生成新注释（与 Java 行为一致，不去重），但不破坏元素原有内容。

#### Scenario: 元素上方已有注释时再次触发
- **WHEN** proto 字段上方已有 `// 旧注释`，用户再次触发 `ctrl+\`
- **THEN** 系统在已有注释上方再插入一条新注释（与 Java Javadoc 覆盖写入行为一致）
