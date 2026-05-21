## Context

批量注释的核心瓶颈在于：每次调用 `docGeneratorService.generate()` 可能触发远程翻译 API（Baidu/Tencent/OpenAI 等），网络 I/O 阻塞 EDT。类成员多时（如 20+ 方法/字段），总耗时可达数秒甚至数十秒，IDE 完全无响应。

当前调用链（全在 EDT 上同步执行）：
```
ActionPerformed (EDT)
  → genClassJavadoc
    → for each member: docGeneratorService.generate()  ← 网络 I/O
    → writerService.writeJavadoc()                     ← PSI 写入
```

IntelliJ 平台规定：EDT 上不得执行耗时操作；PSI 写入必须在 EDT 上通过 `WriteCommandAction`。

## Goals / Non-Goals

**Goals:**
- 将翻译/生成阶段移至后台线程，EDT 不再卡顿
- 显示带百分比进度条，支持用户中途取消
- 两个批量 Action 均适用，保持行为一致
- 批量合并 PSI 写入，减少撤销栈条目数

**Non-Goals:**
- 多线程并发翻译（各翻译 API 有请求频率限制，并发会导致限流，保持串行即可）
- 修改单条注释生成（`GenerateJavadocAction`）的执行模型
- 引入新的外部依赖

## Decisions

### 1. 使用 `Task.Backgroundable` 而非 `runProcessWithProgressSynchronously`

IntelliJ 推荐 `Task.Backgroundable`（`ProgressManager.getInstance().run(new Task.Backgroundable(...))`）来在后台线程执行任务，并通过 `ProgressIndicator` 报告进度、响应取消。

`runProcessWithProgressSynchronously` 会阻塞调用线程直到任务完成，UI 线程在对话框期间虽不卡顿但仍受限，不如 `Task.Backgroundable` 干净。

**选用 `Task.Backgroundable`。**

### 2. 先收集所有待生成元素，再顺序执行

在 EDT（`actionPerformed`）中同步收集所有 `PsiElement`（方法/字段/类），得到元素列表后启动后台任务。后台线程按序调用 `generate()`，每完成一个元素即更新进度条分数。

原因：`PsiElement` 的遍历（`psiClass.getMethods()` 等）是只读 PSI 访问，在 EDT 上安全且瞬时；耗时的 I/O 只在 `generate()` 内部。

### 3. 批量写入：后台任务完成后一次 `WriteCommandAction` 提交所有结果

后台线程将 `(PsiElement, comment)` 收集到列表，全部生成完后，通过 `ApplicationManager.getApplication().invokeLater()` 回到 EDT，执行一次 `WriteCommandAction.runWriteCommandAction()` 批量写入。

好处：
- 只产生一个撤销栈条目（"Batch Generate Javadoc"）
- 减少 `reformatText` 调用次数（或合并为一次）

### 4. `WriterService` 新增 `writeJavadocBatch` 方法

避免修改现有 `writeJavadoc` 签名，新增一个接受 `List<WriteEntry>` 的方法在单次 `WriteCommandAction` 内批量执行写入，保持向后兼容。

## Risks / Trade-offs

- **PSI 失效风险**：后台任务执行期间，用户可能修改文件导致收集的 `PsiElement` 失效。缓解：写入前检查 `element.isValid()`，跳过已失效元素。
- **取消一致性**：用户取消后，已生成但未写入的注释全部丢弃（不做部分写入），保证操作原子性。
- **API 限流**：顺序执行翻译可避免并发限流，但大类（50+ 成员）整体耗时仍较长。进度条让用户感知进度，体验可接受。
- **`ProgressIndicator.checkCanceled()`**：必须在每次 `generate()` 调用后检查，否则取消响应不及时。

## Migration Plan

- 改动仅在 `Action` 层和 `WriterService`，无配置/存储变更，无需数据迁移
- 直接替换现有实现，不需要功能开关
- 回滚：还原对应 Action 文件即可

## Open Questions

- 是否对 proto 批量（`ProtobufMembersHandler`）同步优化？当前 proto 翻译走相同 `TranslatorService`，理论上也会卡顿，建议纳入本次范围。
