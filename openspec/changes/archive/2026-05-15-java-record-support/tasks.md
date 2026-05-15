## 1. Config & Data Model

- [x] 1.1 Add `genAllRecordComponent` boolean field (default `true`) to `EasyDocConfig`
- [x] 1.2 Add `recordComponentTemplateConfig` field of type `TemplateConfig` to `EasyDocConfig`
- [x] 1.3 Initialise `recordComponentTemplateConfig` defaults in `EasyDocConfigComponent.initComponent()` (isDefault=true, template=`/** $DOC$ */`)

## 2. RecordComponentDocGenerator

- [x] 2.1 Create `RecordComponentDocGenerator.java` in `javadoc/service/generator/impl/` extending `AbstractDocGenerator`
- [x] 2.2 Implement `generate(PsiElement)`: guard with `instanceof PsiRecordComponent`, respect cover mode IGNORE/MERGE
- [x] 2.3 Resolve template variables `$DOC$`, `$NAME$`, `$TYPE$` via `JavadocVariableGeneratorService` (add inner-variable map with `componentName`, `componentType`)
- [x] 2.4 Add AI-mode path in `RecordComponentDocGenerator` reusing the existing field prompt (`prompts/chatglm/field.prompt`)
- [x] 2.5 Register `PsiRecordComponent.class → RecordComponentDocGenerator` in `JavaDocGeneratorServiceImpl`'s `docGeneratorMap`

## 3. Record @param Injection in ClassDocGenerator

- [x] 3.1 In `ClassDocGenerator.generate()`, after building `targetJavadoc`, check `psiClass.isRecord()`
- [x] 3.2 If record, iterate `psiClass.getRecordComponents()` and translate each component name
- [x] 3.3 Append `@param <name> <description>` lines to the Javadoc string before calling `merge()`
- [x] 3.4 Verify merge mode correctly preserves existing `@param` tags (relies on existing `AbstractDocGenerator.merge()` logic — add a targeted test)

## 4. Batch Generation

- [x] 4.1 Add "Record Components" checkbox (`recordComponentCheckBox`) to `GenerateAllView` UI form
- [x] 4.2 Pre-populate and persist `recordComponentCheckBox` state via `config.getGenAllRecordComponent()` / `config.setGenAllRecordComponent()` in `GenerateAllJavadocAction.javadocProcess()`
- [x] 4.3 Pass `isGenRecordComponent` into `genClassJavadoc()` and add a `genRecordComponentJavadoc()` helper
- [x] 4.4 In `genClassJavadoc()`, record-aware class generation: when `isRecord() && isGenRecordComponent`, trigger class Javadoc generation which injects @param tags

## 5. Verification

- [ ] 5.1 Build plugin with `./gradlew build` and confirm zero compilation errors
- [ ] 5.2 Run sandbox IDE (`./gradlew runIde`) and manually test: cursor on record component → generates comment
- [ ] 5.3 Test: generate class Javadoc for a record → `@param` tags appear for each component
- [ ] 5.4 Test: batch generation with "Record Components" checked → all components get comments
- [ ] 5.5 Test: batch generation on non-record class with option checked → no errors, normal behaviour
- [ ] 5.6 Test: cover mode IGNORE on record component with existing comment → comment not overwritten
