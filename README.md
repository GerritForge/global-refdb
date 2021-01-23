# Gerrit interface to a global-refdb

Global ref-database interface for use with Gerrit Code Review.

Enables the de-coupling between Gerrit, its libModules and the different
implementations of a globally shared ref-database.

## Design

[The design for a global ref-db interface](https://gerrit.googlesource.com/plugins/multi-site/+/refs/heads/master/DESIGN.md#global-ref_db-plugin)
can be found as part of the multi-site design documentation, where it first
originated and was approved by the community.

## Bindings

In order to consume this library, some Guice bindings need to be registered
appropriately. More information in the relevant [documentation](./bindings.md).