## Why

批量注释功能（`GenerateAllJavadocAction` 和 `GenerateClassMembersJavadocAction`）在 EDT（Event Dispatch Thread）上同步执行翻译 API 调用和 PSI 写入，导致 IDEA 界面卡顿甚至假死，在类成员较多时体验极差。

## What Changes

- 将批量注释的**翻译/生成阶段**移至后台线程（`ProgressManager` backgroundable task），并显示可取消的进度条
- PSI 写入操作仍在 EDT 上通过 `WriteCommandAction` 批量提交，减少锁竞争次数
- 两个批量 Action（`GenerateAllJavadocAction`、`GenerateClassMembersJavadocAction`）均适用同一后台任务模型
- `ProtobufMembersHandler` 中的批量处理同步迁移

## Capabilities

### New Capabilities
- `async-batch-generation`: 后台线程执行批量注释生成，提供进度条与取消支持，完成后一次性批量写入 PSI

### Modified Capabilities
<!-- 无现有 spec 层需求变更 -->

## Impact

- `action/GenerateAllJavadocAction.java` — `genClassJavadoc` 重构为后台任务
- `action/GenerateClassMembersJavadocAction.java` — `genMembersJavadoc` 重构为后台任务
- `proto/ProtobufMembersHandler.java` — 批量写入路径同步优化
- `service/WriterService.java` — 可能需要新增支持批量写入的方法，以减少 `WriteCommandAction` 调用次数
- 无 API 或配置破坏性变更
