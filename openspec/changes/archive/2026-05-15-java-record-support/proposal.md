## Why

Java Records (finalized in Java 16) are widely used in modern Java, but Easy Javadoc currently has no awareness of `PsiRecordComponent` — attempting to generate Javadoc for a record component silently produces nothing, and batch generation on a record class skips components entirely. This blocks users on Java 16+ projects from using the plugin effectively.

## What Changes

- Add a new `RecordComponentDocGenerator` that handles `PsiRecordComponent` PSI elements, generating Javadoc for individual record components
- Extend `JavaDocGeneratorServiceImpl` to route `PsiRecordComponent` to the new generator
- Extend `ClassDocGenerator` to detect record classes and auto-include `@param` tags for each component in the class-level Javadoc
- Extend `GenerateAllJavadocAction` to iterate `psiClass.getRecordComponents()` when batch-generating on a record class
- Add `recordComponentTemplateConfig` to `EasyDocConfig` for user-customizable record component templates
- Register the new template config in `EasyDocConfigComponent` defaults and the settings UI

## Capabilities

### New Capabilities

- `record-component-doc`: Generate Javadoc for individual record components (`PsiRecordComponent`) via `RecordComponentDocGenerator`, including template variable support (`$DOC$`, `$TYPE$`, `$NAME$`) and AI-mode support
- `record-class-params-doc`: Auto-generate `@param` tags for record components in the class-level Javadoc when the target class is a record
- `record-batch-generation`: Batch Javadoc generation (`GenerateAllJavadocAction`) respects record components alongside methods and fields

### Modified Capabilities

<!-- No existing spec-level requirements are changing — all changes are net-new behaviour. -->

## Impact

- `src/main/java/com/star/easydoc/javadoc/service/JavaDocGeneratorServiceImpl.java` — add `PsiRecordComponent` mapping
- `src/main/java/com/star/easydoc/javadoc/service/generator/impl/ClassDocGenerator.java` — detect `isRecord()`, inject component `@param` tags
- `src/main/java/com/star/easydoc/action/GenerateAllJavadocAction.java` — add record component iteration
- `src/main/java/com/star/easydoc/config/EasyDocConfig.java` — add `recordComponentTemplateConfig` field
- `src/main/java/com/star/easydoc/config/EasyDocConfigComponent.java` — initialise record component defaults
- New file: `src/main/java/com/star/easydoc/javadoc/service/generator/impl/RecordComponentDocGenerator.java`
- Settings UI may need a new tab/panel for record component template config (low priority, can ship without)
