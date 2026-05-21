## ADDED Requirements

### Requirement: 批量注释生成在后台线程执行
批量注释功能（`GenerateAllJavadocAction` 和 `GenerateClassMembersJavadocAction`）的翻译/注释生成阶段 SHALL 在后台线程执行，不得阻塞 EDT。

#### Scenario: 触发批量注释时不卡顿
- **WHEN** 用户触发批量注释（`ctrl+shift+\` 或快捷键）
- **THEN** IDE 界面保持响应，用户可继续操作编辑器

#### Scenario: 显示进度条
- **WHEN** 批量注释任务启动
- **THEN** IDEA 底部或弹出窗显示进度条，包含当前处理的元素名称和完成百分比

### Requirement: 支持用户取消批量任务
批量注释任务 SHALL 响应用户取消操作。

#### Scenario: 用户取消任务
- **WHEN** 用户在进度条界面点击"Cancel"
- **THEN** 任务停止，已生成但未写入的注释全部丢弃，文件无任何修改

#### Scenario: 取消后文件保持原状
- **WHEN** 用户取消任务
- **THEN** 文件内容与任务启动前完全一致

### Requirement: 批量写入合并为单次 WriteCommandAction
所有生成结果 SHALL 在后台任务完成后通过单次 `WriteCommandAction` 批量提交到 PSI。

#### Scenario: 撤销栈只产生一条记录
- **WHEN** 批量注释写入完成
- **THEN** 用户执行一次 Ctrl+Z 即可撤销所有本次写入

#### Scenario: 失效元素被跳过
- **WHEN** 后台任务执行期间用户修改文件导致部分 PsiElement 失效
- **THEN** 失效元素被跳过，有效元素的注释正常写入

### Requirement: GenerateAllJavadocAction 使用后台任务模型
`GenerateAllJavadocAction` 中所有涉及翻译 API 调用的生成路径 SHALL 迁移至后台线程。

#### Scenario: Java 类批量注释
- **WHEN** 用户在 Java 类上触发 `ctrl+shift+\` 并确认选项
- **THEN** 后台任务顺序生成类/方法/字段注释并显示进度，完成后批量写入

### Requirement: GenerateClassMembersJavadocAction 使用后台任务模型
`GenerateClassMembersJavadocAction` 的批量生成路径 SHALL 迁移至后台线程。

#### Scenario: 快捷键触发批量成员注释
- **WHEN** 用户在类或文件上触发快捷键（`shift+ctrl+\`）
- **THEN** 后台任务顺序生成方法和字段注释并显示进度，完成后批量写入

### Requirement: ProtobufMembersHandler 使用后台任务模型
`ProtobufMembersHandler` 的批量注释生成路径 SHALL 迁移至后台线程。

#### Scenario: Proto 文件批量注释
- **WHEN** 用户在 proto message/service/enum 上触发批量注释
- **THEN** 后台任务执行，不阻塞 EDT
