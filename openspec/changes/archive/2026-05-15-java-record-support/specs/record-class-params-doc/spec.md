## ADDED Requirements

### Requirement: Record class Javadoc includes @param tags for components
When generating the class-level Javadoc for a record class, the system SHALL automatically append a `@param` tag for each record component, placed after the main description block.

Each `@param` tag SHALL be of the form:
```
@param <componentName> <translated description>
```
where `<translated description>` is obtained by running the component name through the active translator.

This behaviour SHALL apply only when `psiClass.isRecord()` returns `true`.

#### Scenario: Record class gets @param tags for all components
- **WHEN** the user generates Javadoc for `record Point(int x, int y) {}`
- **THEN** the generated comment contains `@param x` and `@param y` tags with translated descriptions

#### Scenario: Non-record class is unaffected
- **WHEN** the user generates Javadoc for an ordinary class, interface, enum, or annotation
- **THEN** no `@param` tags are injected by the record-component logic

#### Scenario: Record with no components generates class Javadoc without @param
- **WHEN** the user generates Javadoc for `record Empty() {}`
- **THEN** the generated comment contains no `@param` tags from record component injection

### Requirement: Merge mode preserves existing @param tags for record components
When cover mode is "智能合并" and a record class already has a Javadoc comment with `@param` tags for some components, the system SHALL preserve existing `@param` values and only add `@param` tags for components that have no existing entry.

#### Scenario: Existing @param tags preserved on re-generation
- **WHEN** a record class already has `@param x the x-coordinate` and the user re-generates in merge mode
- **THEN** the `@param x` value `the x-coordinate` is preserved in the output

#### Scenario: Missing @param tags added in merge mode
- **WHEN** a record class has an existing `@param x` but no `@param y`, and the user re-generates in merge mode
- **THEN** a new `@param y` tag with a translated description is added while `@param x` is preserved
