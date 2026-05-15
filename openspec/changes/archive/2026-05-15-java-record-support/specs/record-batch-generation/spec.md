## ADDED Requirements

### Requirement: Batch generation dialog includes record component option
The `GenerateAllView` dialog SHALL include a "Record Components" checkbox that controls whether record components are included in batch generation.

The checkbox SHALL be enabled (visible and interactable) for all classes; for non-record classes it has no effect since those classes have no record components.

The user's last selection SHALL be persisted in `EasyDocConfig.genAllRecordComponent` and pre-populated on next open.

#### Scenario: Dialog shows record component checkbox
- **WHEN** the user opens the "Generate All" dialog via `ctrl+shift+\`
- **THEN** the dialog displays a "Record Components" checkbox alongside the existing Class/Method/Field/Inner Class checkboxes

#### Scenario: Checkbox state is persisted
- **WHEN** the user selects the "Record Components" checkbox and confirms
- **THEN** on the next dialog open the checkbox is pre-selected

### Requirement: Batch generation processes record components when option is selected
When `isGenRecordComponent` is `true` and the target class is a record, the system SHALL iterate `psiClass.getRecordComponents()` and generate a Javadoc comment for each component.

#### Scenario: Record components are generated in batch for a record class
- **WHEN** "Record Components" is checked and generation is invoked on `record Person(String name, int age) {}`
- **THEN** Javadoc comments are written for both `name` and `age` components

#### Scenario: Batch generation on non-record class does not error when option is selected
- **WHEN** "Record Components" is checked and batch generation is invoked on an ordinary class
- **THEN** the system completes normally with zero record component comments generated (no exceptions thrown)

#### Scenario: Record components skipped when option is unchecked
- **WHEN** "Record Components" is unchecked and batch generation is invoked on a record class
- **THEN** no comments are written for record components, but class/method/field generation proceeds normally per other checkboxes

### Requirement: Batch generation on record class iterates components via getRecordComponents()
The system SHALL call `psiClass.getRecordComponents()` (not `getFields()`) to enumerate record components, because `PsiRecordComponent` is not a `PsiField`.

#### Scenario: getRecordComponents() used for record component iteration
- **WHEN** batch generation processes a record class with components
- **THEN** each `PsiRecordComponent` returned by `getRecordComponents()` is passed to the record component generator, not the field generator
