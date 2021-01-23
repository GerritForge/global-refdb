
global-refdb Configuration
=========================

Configuration should be specified in the `$site_path/etc/<plugin>.config` file of
the plugin consuming this library.

## Configuration parameters

```ref-database.enabled```
:   Enable the use of a shared ref-database
    Defaults: true

```ref-database.enforcementRules.<policy>```
:   Level of consistency enforcement across sites on a project:refs basis.
    Supports two values for enforcing the policy on multiple projects or refs.
    If the project or ref is omitted, apply the policy to all projects or all refs.

    The <policy> can be one of the following values:

    1. REQUIRED - Throw an exception if a git ref-update is processed again
    a local ref not yet in sync with the shared ref-database.
    The user transaction is cancelled. The Gerrit GUI (or the Git client)
    receives an HTTP 500 - Internal Server Error.

    2. IGNORED - Ignore any validation against the shared ref-database.

    *Example:*
    ```
    [ref-database "enforcementRules"]
       IGNORED = AProject:/refs/heads/feature
    ```

    Ignore the alignment with the shared ref-database for AProject on refs/heads/feature.

    Defaults: No rules = All projects are REQUIRED to be consistent on all refs.

```ref-database.metricsRoot```
:   Name used to prefix ref-database metrics.
    Defaults: multi-site

```projects.pattern```
:   Specifies which projects should be validated against the shared ref-database.
    It can be provided more than once, and supports three formats: regular
    expressions, wildcard matching, and single project matching. All three
    formats match case-sensitive.

    Values starting with a caret `^` are treated as regular
    expressions. For the regular expressions details please follow
    official [java documentation](https://docs.oracle.com/javase/tutorial/essential/regex/).

    Please note that regular expressions could also be used
    with inverse match.

    Values that are not regular expressions and end in `*` are
    treated as wildcard matches. Wildcards match projects whose
    name agrees from the beginning until the trailing `*`. So
    `foo/b*` would match the projects `foo/b`, `foo/bar`, and
    `foo/baz`, but neither `foobar`, nor `bar/foo/baz`.

    Values that are neither regular expressions nor wildcards are
    treated as single project matches. So `foo/bar` matches only
    the project `foo/bar`, but no other project.

    By default, all projects are matched.