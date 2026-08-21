# User & Policy Manager

Desktop app (JavaFX + Spring Boot) that manages **users** and **policies**. A policy is a
set of conditions on user fields (e.g. "born after 2007-01-01"). The app computes, for
every user, which policies match them, and shows that in the UI.

## Technical documentation

### Running

```
mvn javafx:run
```

Data is loaded once at startup from JSON files under `src/main/resources/data/` and
persisted back there on every change (no database).

### Data files

| File | Content |
|---|---|
| `users.json` | list of users |
| `policies.json` | list of policies |
| `organization-units.json` | flat list of valid organization unit names |
| `policy-assignments.json` | computed cache: `username → [policyId, ...]` |

`organization-units.json` is the only file meant to be **hand-edited**: the app only
reads it, never writes to it. The other three are fully managed by the app; editing
them by hand is possible but any bad JSON breaks startup, so backing up those files is strongly recommended.

`users.json`

```json
{
  "username": "jdoe",
  "firstName": "John",
  "lastName": "Doe",
  "emailAddress": "jdoe@evolveum.com",
  "organizationUnit": ["Software Development", "Support"],
  "birthDate": "2007-09-07",
  "registeredOn": "2024-05-07"
}
```

`username` is the unique identifier and cannot be changed after creation.
`organizationUnit` is a list — a user can belong to several units, each one must exist
in `organization-units.json`.

`policies.json`

```json
{
  "id": "underaged",
  "name": "Underaged User",
  "conditions": {
    "birthDate": { "greaterThan": "2007-01-01" }
  }
}
```

`conditions` maps a **user field name** to one or more `operator: value` assertions. A
policy matches a user only if **every** condition (and every operator within a
condition) matches — it's an AND, never OR. Example with two conditions on the same
field (a range):

```json
"conditions": {
  "birthDate": { "greaterThan": "2000-01-01", "lessThan": "2020-01-01" }
}
```

The UI's policy editor only allows one row per field, so multi-operator-per-field
conditions like this can only be created by editing the JSON by hand.

#### Assertable fields and operators

| Field | Type | Operators |
|---|---|---|
| `username` | string | `equals`, `notEquals`, `contains`, `notContains` |
| `firstName` | string |  `equals`, `notEquals`, `contains`, `notContains` |
| `lastName` | string |  `equals`, `notEquals`, `contains`, `notContains` |
| `emailAddress` | string |  `equals`, `notEquals`, `contains`, `notContains` |
| `organizationUnit` | string* |  `equals`, `notEquals`, `contains`, `notContains` |
| `birthDate` | date | `equals`, `greaterThan`, `lessThan` |
| `registeredOn` | date | `equals`, `greaterThan`, `lessThan` |

\* `organizationUnit` is a list on the user, but for matching it is joined into one
comma-separated string, so `contains`/`notContains` check membership (e.g.
`"organizationUnit": {"notContains": "CEO"}` means "not a CEO").

Dates are ISO strings (`"2007-01-01"`). `equals`/`notEquals`/`contains`/`notContains`
take a string value; `greaterThan`/`lessThan` take a date or number depending on field
type.

New assertable fields can be added in code via
`ConditionFactory.registerField(name, type, accessor)` — no other change needed.
However, user fields are added automatically since these are the basis 
([see the table above](#assertable-fields-and-operators)).

### How matching works

Nothing is stored on the user. Saving or deleting a **user** only recomputes that one
user's matching policy ids. Saving or deleting a **policy** recomputes it for **every**
user, since a policy change can affect anyone's match — this is unavoidable. Either way
the result is written to `policy-assignments.json`. 

A full recompute for every user also
runs once at application startup, in case the JSON files were hand-edited while the app
was closed. A small spinner is shown while a recompute runs.

Because policy changes recompute everyone, deleting a policy automatically removes it
from every user it used to apply to — there is nothing left to clean up manually.

### Known limitations

- **Recompute runs synchronously**, blocking the UI (a spinner is shown to make this
  visible). Fine at current data sizes; if users/policies grow large, move the recompute
  to an async background service instead.
- **Storage is plain JSON files**, no transactions, no concurrent-access safety, no
  query capability. A more robust setup would move to a real database (e.g. via
  Hibernate/JPA) once persistence needs to grow beyond a single-user desktop app.
- **`organization-units.json` has no GUI editor** — units are only added/removed by
  hand-editing that file. Could be extended with a management screen like the one for
  users and policies.
- **Not mobile-friendly**: it's a fixed-size JavaFX desktop window with no responsive
  layout, so it does not adapt to small/touch screens.
- **Duplicate-id check on Save & close**: on create, the id is
  validated by calling `userService.getUser(id)` / `policyService.getPolicy(id)` on
  every save. At larger scale this should be backed by a cache of existing user/policy
  ids instead of a direct lookup on each save.

## User documentation

### Using the app

**Home screen** → *View users* / *View policies*, each opens a list.

**Lists**: click a row to open its detail/edit screen; each row also has its own
*Delete* button (asks for confirmation first). *Create* opens a blank detail screen.

**Detail/edit screens**: `Save & close` and `Cancel` only appear once you actually
change something (or immediately, when creating something new — there is nothing saved
yet to compare against). The back arrow (top-left) behaves like `Cancel` and, like it,
asks for confirmation if you have unsaved changes.

**User detail** also lists the policies currently matching that user (click one to jump
to it); 

**Policy detail** lists the users currently matching it (click one to jump to
their detail). 
Policy conditions are edited as rows (`+` adds one, `✕` removes one); a
field already used in one row cannot be picked in another.

#### Validation on save

**User**: `username`, `firstName`, `lastName`, `emailAddress`, `birthDate`,
`registeredOn` are all required; email must look like an email; `birthDate` and
`registeredOn` cannot be in the future; `registeredOn` cannot be before `birthDate`.
`registeredOn` defaults to today when creating a new user.

**Policy**: `id`, `name`, and at least one condition are required; every condition row
needs a field, an operator, and a value.
