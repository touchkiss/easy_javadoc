# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Easy Javadoc is an IntelliJ IDEA plugin (group `com.star.easydoc`, version `4.5.3`) that auto-generates Javadoc/KDoc comments for Java and Kotlin code. It integrates multiple translation backends (Baidu, Tencent, Aliyun, Microsoft, Google, local dictionary, custom HTTP, OpenAI-compatible LLMs) and writes comments back into the IDE via IDEA's PSI/write-command APIs.

Minimum supported IDEA version: **2023.1**. Requires **JDK 17** and Gradle Wrapper (`gradle-8.14`).

## Build & Run Commands

```bash
# Build plugin package
./gradlew build

# Launch a sandboxed IDE instance for live testing (primary dev workflow)
./gradlew runIde

# Run tests
./gradlew test

# Clean build outputs
./gradlew clean

# Build a fat JAR with all runtime deps bundled
./gradlew fatJar
```

There is only one test file (`src/test/java/com/star/easydoc/MainTest.java`) with an empty body — no meaningful test suite exists yet.

When bumping a release, update the version in **both** `build.gradle` (`version = '...'`) and the `<change-notes>` block in `src/main/resources/META-INF/plugin.xml`, and add an entry to the `README.md` changelog.

## Architecture

### Layered design

```
Action layer  →  Service layer  →  Config layer
    ↓                  ↓
  IDEA platform APIs (PSI, WriteCommandAction, CodeStyleManager)
```

**Action layer** (`action/`)
- `GenerateJavadocAction` — handles `ctrl+\` / `cmd+\`: generates a single element's comment, performs Chinese→English naming, or pops a translation dialog, depending on cursor/selection state.
- `GenerateAllJavadocAction` — handles `ctrl+shift+\` / `cmd+shift+\`: batch-generates comments for all methods/fields/inner classes in a class.

**Service layer**
- `DocGeneratorService` (interface) — unified entry point for doc generation; implemented by `JavaDocGeneratorServiceImpl` and `KdocGeneratorServiceImpl`.
- `JavaDocGeneratorServiceImpl` — routes to one of four `DocGenerator` implementations based on PSI element type: `ClassDocGenerator`, `MethodDocGenerator`, `FieldDocGenerator`, `PackageInfoDocGenerator` (all under `javadoc/service/generator/impl/`).
- `JavadocVariableGeneratorService` / `KdocVariableGeneratorService` — resolve template variables (`$AUTHOR$`, `$DATE$`, `$PARAMS$`, `$RETURN$`, `$THROWS$`, `$SEE$`, `$DOC$`, etc.) via a map of `VariableGenerator` implementations.
- `TranslatorService` — aggregates all translation backends. Each backend extends `AbstractTranslator` and implements `Translator`. Current backends: Baidu, Tencent, Aliyun, Microsoft, MicrosoftFree, Youdao, YoudaoAi, Google, GoogleFree, Jinshan, Custom (HTTP), Local (bundled `words.json`), SimpleSplitter.
- `GptService` — OpenAI-compatible LLM adapter; prompts are stored as text files under `src/main/resources/prompts/` (`openai/` and `chatglm/` subdirectories, each with `class.prompt`, `method.prompt`, `field.prompt`).
- `WriterService` — wraps PSI writes in `WriteCommandAction` and calls `CodeStyleManager.reformatText` afterward.
- `PackageInfoService` — creates or patches `package-info.java` for directory-level comments.

**Config layer**
- `EasyDocConfig` — POJO holding all settings (author, date format, templates, override mode, word maps, translator choice, AI provider, timeouts, project-level word maps, excluded fields).
- `EasyDocConfigComponent` implements `PersistentStateComponent<EasyDocConfig>` — persists to IDEA's application-level XML state; initialises defaults on first load.

### Extension point registration (`plugin.xml`)

All services are registered as `<applicationService>` so they're singletons accessible via `ServiceManager`. Settings UI is registered as nested `<applicationConfigurable>` under `EasyDoc > EasyDocJavadoc` (class / method / field templates) and `EasyDoc > EasyDocKdoc`.

### Java vs Kotlin split

Java source (`src/main/java`) hosts all core logic. Kotlin source (`src/main/kotlin`) provides KDoc equivalents: `KdocGeneratorServiceImpl`, `KdocVariableGeneratorService`, and the KDoc settings UI. Both compile to JVM 17.

### Adding a new translator

1. Create `impl/MyTranslator.java` extending `AbstractTranslator`.
2. Add a constant to `Consts.java` and a branch in `TranslatorService`.
3. Add config fields to `EasyDocConfig` if credentials are needed.
4. Add UI controls to the common settings view.

### Adding a new template variable

1. Create `impl/MyVariableGenerator.java` extending `AbstractVariableGenerator`.
2. Register it in `JavadocVariableGeneratorService`'s constructor map.
3. Add the corresponding `VariableTypeEnum` entry in `EasyDocConfig`.
