## 1. WriterService 扩展

- [x] 1.1 在 `WriterService` 中新增 `writeJavadocBatch(Project, List<WriteEntry>)` 方法，在单次 `WriteCommandAction` 内批量写入所有注释
- [x] 1.2 定义 `WriteEntry` 数据结构（`PsiElement element`, `PsiDocComment comment`, `int newlineCount`）

## 2. GenerateAllJavadocAction 重构

- [x] 2.1 在 `actionPerformed` 中同步收集所有待处理 `PsiElement` 列表（只读 PSI 遍历，保留在 EDT）
- [x] 2.2 用 `Task.Backgroundable` 包装注释生成循环，通过 `ProgressIndicator` 报告进度和元素名
- [x] 2.3 在每次 `docGeneratorService.generate()` 后调用 `indicator.checkCanceled()`
- [x] 2.4 后台任务完成后通过 `Task.Backgroundable.onSuccess()` 回到 EDT，调用 `writeJavadocBatch` 批量写入
- [x] 2.5 确保用户取消时不执行任何 PSI 写入

## 3. GenerateClassMembersJavadocAction 重构

- [x] 3.1 在 `actionPerformed` 中同步收集所有待处理成员列表
- [x] 3.2 用 `Task.Backgroundable` 包装生成循环，报告进度
- [x] 3.3 在每次 `generate()` 后检查取消信号
- [x] 3.4 后台任务完成后批量写入，取消时丢弃所有结果

## 4. ProtobufMembersHandler 重构

- [x] 4.1 识别 `ProtobufMembersHandler.handle()` 中的翻译调用路径
- [x] 4.2 用 `Task.Backgroundable` 包装 proto 批量生成，报告进度
- [x] 4.3 完成后批量写入注释到 proto 文件

## 5. 验证

- [ ] 5.1 手动测试：对含 20+ 方法的 Java 类触发批量注释，确认进度条显示且 IDE 不卡顿
- [ ] 5.2 手动测试：任务中途取消，确认文件无修改
- [ ] 5.3 手动测试：Ctrl+Z 一次即可撤销所有写入
- [ ] 5.4 手动测试：proto 文件批量注释不卡顿
- [x] 5.5 构建验证：`./gradlew build` 无错误
