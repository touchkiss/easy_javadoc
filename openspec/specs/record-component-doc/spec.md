# Spec: record-component-doc

## Purpose

Define how Easy Javadoc generates Javadoc comments for individual `PsiRecordComponent` elements (the components declared in a Java record's header), including template configuration and variable resolution.

## Requirements

### Requirement: Generate Javadoc for record component
The system SHALL generate a Javadoc comment when the user invokes the single-element generation action (`ctrl+\` / `cmd+\`) with the cursor on a `PsiRecordComponent`.

The generated comment SHALL use a configurable template. The default template SHALL be:
```
/** $DOC$ */
```
where `$DOC$` is resolved by translating the component name using the active translator.

The following template variables SHALL be supported:
- `$DOC$` — translated description of the component
- `$NAME$` — component name as declared
- `$TYPE$` — canonical type name of the component

#### Scenario: Cursor on record component generates comment
- **WHEN** the user places the cursor on a record component (e.g., `int x` in `record Point(int x, int y)`) and invokes the generate action
- **THEN** a Javadoc comment is written above that component

#### Scenario: Template variables are resolved
- **WHEN** a record component named `userId` of type `String` is processed with the default template
- **THEN** `$DOC$` is replaced with the translated description of "userId", `$NAME$` with `userId`, and `$TYPE$` with `java.lang.String`

#### Scenario: AI mode generates component comment
- **WHEN** the active translator is an AI translator (OpenAI or ChatGLM) and the user invokes generation on a record component
- **THEN** the AI prompt is sent with the component's source text and the response is written as the comment

#### Scenario: Cover mode IGNORE skips existing comment
- **WHEN** cover mode is set to "忽略" and the record component already has a Javadoc comment
- **THEN** the system SHALL return empty and leave the existing comment unchanged

#### Scenario: Cover mode MERGE preserves existing comment body
- **WHEN** cover mode is set to "智能合并" and the record component already has a Javadoc comment
- **THEN** the system SHALL merge new content with the existing comment, preserving the existing description

### Requirement: Configurable record component template
The system SHALL store record component template configuration under `recordComponentTemplateConfig` in `EasyDocConfig`, using the existing `TemplateConfig` type.

On first load, the configuration SHALL be initialised to a default template of `/** $DOC$ */` with `isDefault = true`.

#### Scenario: Default template used when no custom template configured
- **WHEN** `recordComponentTemplateConfig.isDefault` is `true`
- **THEN** the generator SHALL use the built-in default template `/** $DOC$ */`

#### Scenario: Custom template used when configured
- **WHEN** `recordComponentTemplateConfig.isDefault` is `false` and a custom template is set
- **THEN** the generator SHALL use the custom template string
