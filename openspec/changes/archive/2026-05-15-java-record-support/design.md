## Context

Easy Javadoc maps PSI element types to generator instances in `JavaDocGeneratorServiceImpl`. Currently the map covers `PsiClass`, `PsiMethod`, `PsiField`, and `PsiPackage`. Java 16 introduced `PsiRecordComponent` — a distinct PSI type that is neither a `PsiField` nor a `PsiMethod` — so the plugin produces no output when invoked on a record component, and `GenerateAllJavadocAction` silently skips components during batch generation. The fix must stay backwards-compatible: ordinary classes, enums, and interfaces must be unaffected.

## Goals / Non-Goals

**Goals:**
- Single-element Javadoc generation for `PsiRecordComponent` (cursor on a component → `ctrl+\`)
- Batch generation includes record components alongside methods and fields
- Class-level Javadoc for a record auto-includes `@param` for each component (matching standard Javadoc convention)
- Configurable template for record component comments (`$DOC$`, `$NAME$`, `$TYPE$`)
- AI mode (GPT/ChatGLM) works for record components the same way it does for fields

**Non-Goals:**
- KDoc / Kotlin data class support (separate concern)
- Settings UI panel for record component template (can be added in a follow-up; hardcode defaults for now)
- Accessor method detection or synthetic constructor Javadoc

## Decisions

### D1 — New `RecordComponentDocGenerator` rather than extending `FieldDocGenerator`

`PsiRecordComponent` extends `PsiVariable` but does not extend `PsiField`. Forcing `FieldDocGenerator` to accept both types via an `instanceof` check would violate single-responsibility. A dedicated generator keeps concerns separate and allows record-specific template variables (`$TYPE$`, `$NAME$`) without coupling.

*Alternative considered*: Subclass `FieldDocGenerator` — rejected because the inheritance hierarchy becomes confusing (`PsiRecordComponent` is not semantically a field) and the field-exclusion logic (`isExcludedField`) would incorrectly apply.

### D2 — Record `@param` tags injected by `ClassDocGenerator`, not a separate generator

The Javadoc specification for records places component documentation as `@param` tags on the class comment, not on each component. `ClassDocGenerator.generate()` already builds the class Javadoc string; the cleanest extension is to detect `psiClass.isRecord()` and append component params before calling `merge()`, reusing the existing `JavadocVariableGeneratorService` for the description translation of each component name.

*Alternative considered*: A separate `RecordClassDocGenerator` — rejected because it duplicates all class-level template and AI logic; a conditional branch inside the existing generator is simpler.

### D3 — `GenerateAllJavadocAction` adds a `recordComponentCheckBox` in `GenerateAllView`

Batch generation should follow the same opt-in pattern as methods/fields. Adding a checkbox preserves user control and matches existing UX. Default value stored in `EasyDocConfig.genAllRecordComponent`.

*Alternative considered*: Always generate components when `isGenField` is true — rejected because users may want field-style docs separately from component docs.

### D4 — `EasyDocConfig` holds `recordComponentTemplateConfig` of existing type `TemplateConfig`

`TemplateConfig` already supports `isDefault`, `template`, and `customMap`. Reusing it avoids a new type and plugs directly into existing variable-resolution infrastructure.

## Risks / Trade-offs

- **IDEA version compatibility** — `PsiClass.isRecord()` and `PsiClass.getRecordComponents()` are available from IDEA 2021.1+ (bundled Java plugin). The project already requires IDEA 2023.1 minimum, so this is safe.
- **`merge()` with record `@param` tags** — The existing `AbstractDocGenerator.merge()` treats `@param` tags specially (preserves existing values). Record component `@param` tags will therefore be preserved on re-generation in merge mode, which is the correct behaviour.
- **AI prompts for record components** — Reusing the existing `field.prompt` template is acceptable for the initial implementation. A dedicated `record_component.prompt` can be added later if the output quality is inadequate.

## Migration Plan

No data migration required. `EasyDocConfig` is persisted as XML via IDEA's `PersistentStateComponent`; the new `recordComponentTemplateConfig` field will be `null` on first load and initialised to defaults by `EasyDocConfigComponent.initComponent()`, consistent with how other template configs are bootstrapped.

Rollback: removing the plugin version reverts to the previous behaviour with no state corruption, because the new config field is additive.

## Open Questions

- Should record component `@param` tags be added to the class Javadoc even when the user's class template is fully custom (i.e., `isDefault = false`)? Current plan: yes, inject after template variable resolution so the user's custom description is preserved.
- Should the `genAllRecordComponent` checkbox default to `true` or `false`? Proposal: `true` (generate components by default) since the primary complaint is silent skipping.
