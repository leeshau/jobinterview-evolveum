# User & Policy Manager

Web app (Apache Wicket + Spring Boot, served by an embedded Jetty) that manages
**users** and **policies**. A policy is a set of conditions on user fields (e.g. "born
after 2007-01-01"). The app computes, for every user, which policies match them, and
shows that in the UI.

## Technical documentation

### Running

```
mvn compile exec:java
```

or build a runnable jar and start it directly:

```
mvn package
java -jar target/evolveum-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Either way the app listens on `http://localhost:8080` (override with
`-Dserver.port=<port>`). The entry point is `JettyLauncher`: it starts a plain Spring
Boot context (no embedded web server of its own) for the backend beans, then boots a
hand-configured Jetty server hosting the Wicket application (`WicketApplication`) behind
a single servlet filter mapped to `/*`.

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
wasn't running.

Because policy changes recompute everyone, deleting a policy automatically removes it
from every user it used to apply to — there is nothing left to clean up manually.

### Web UI structure

The UI is a set of Apache Wicket pages under `org.lesek.usermanagement.ui`:

| Package/class | Purpose |
|---|---|
| `ui/JettyLauncher` | composition root: starts the Spring context, then a hand-configured embedded Jetty server hosting Wicket |
| `ui/WicketApplication` | mounts pages to clean URLs (`/users`, `/users/edit`, `/policies`, `/policies/edit`), registers webjars support |
| `ui/pages/BasePage` | shared layout (navbar, Bootstrap/CSS wiring, per-screen background tint) every page extends |
| `ui/pages/WelcomePage` | landing page |
| `ui/pages/user/` | `UserListPage`, `UserDetailPage` (list/create/edit) |
| `ui/pages/policy/` | `PolicyListPage`, `PolicyDetailPage` (list/create/edit) |
| `ui/pages/ErrorHighlightBehavior` | adds a red `is-invalid` outline to whichever form field actually failed validation |

Styling is [Bootstrap 5](https://getbootstrap.com/), pulled in as the `org.webjars:bootstrap`
Maven dependency (served via the `wicket-webjars` library) rather than a CDN — the app
renders correctly without outbound internet access. App-specific CSS lives in
`ui/pages/app.css`.

### Known limitations

- **Recompute runs synchronously**, blocking the request. Fine at current data sizes; if
  users/policies grow large, move the recompute to an async background service instead.
- **Storage is plain JSON files**, no transactions, no concurrent-access safety, no
  query capability. A more robust setup would move to a real database (e.g. via
  Hibernate/JPA) once persistence needs to grow beyond this scale.
- **`organization-units.json` has no GUI editor** — units are only added/removed by
  hand-editing that file. Could be extended with a management screen like the one for
  users and policies.
- **Single embedded Jetty instance, no session clustering** — fine for one server, but
  would need external session storage to run behind a load balancer with multiple
  instances.
- **Duplicate-id check on Save & close**: on create, the id is
  validated by calling `userService.getUser(id)` / `policyService.getPolicy(id)` on
  every save. At larger scale this should be backed by a cache of existing user/policy
  ids instead of a direct lookup on each save.

## User documentation

### Using the app

The navbar (brand name and *Home*/*Users*/*Policies* links) is available on every
screen; the user list is tinted a light cream and the policy list a light blue, so
it's visually obvious which section you're in.

**Home screen** → *View users* / *View policies*, each opens a list.

**Lists**: click *Edit* on a row to open its detail/edit screen; each row also has its
own *Delete* button, which opens a confirmation dialog before anything is actually
deleted. *Create user*/*Create policy* opens a blank detail screen.

**Detail/edit screens**: `Save & close` is always available; `Cancel` discards any
changes and returns to the list.

**User detail** also lists the policies currently matching that user (click one to jump
to it), or a note saying no policy currently matches if none do.

**Policy detail** lists the users currently matching it (click one to jump to
their detail).
Policy conditions are edited as rows (`+` adds one, `✕` removes one); a
field already used in one row cannot be picked in another. `birthDate`/`registeredOn`
use the browser's native date picker.

Any field that fails validation on save is outlined in red, in addition to the error
message shown above the form.

#### Validation on save

**User**: `username`, `firstName`, `lastName`, `emailAddress`, `birthDate`,
`registeredOn` are all required; email must look like an email; `birthDate` and
`registeredOn` cannot be in the future; `registeredOn` cannot be before `birthDate`.
`registeredOn` defaults to today when creating a new user.

**Policy**: `id`, `name`, and at least one condition are required; every condition row
needs a field, an operator, and a value.
