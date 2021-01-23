
Bindings
=========================

This library expects consumers to register Guice bindings.
Some sensible implementations are already been provided and they must be bound
by the Gerrit libModule in order to work.

Alternatively libModules can decide to provide their own implementations (or
override existing ones) and bound those instead.

This is an example of binding that would allow consuming libModules getting up and
running with this library:

```java
public class FooModule extends FactoryModule {

  private final Configuration cfg;

  public FooModule(Configuration cfg) {
    this.cfg = cfg;
  }

  @Override
  protected void configure() {
    bind(SharedRefDatabaseWrapper.class).in(Scopes.SINGLETON);
    bind(SharedRefLogger.class).to(Log4jSharedRefLogger.class);
    factory(LockWrapper.Factory.class);
    factory(SharedRefDbRepository.Factory.class);
    factory(SharedRefDbRefDatabase.Factory.class);
    factory(SharedRefDbRefUpdate.Factory.class);
    factory(SharedRefDbBatchRefUpdate.Factory.class);
    factory(RefUpdateValidator.Factory.class);
    factory(BatchRefUpdateValidator.Factory.class);
    bind(SharedRefDbConfiguration.class).toInstance(cfg.getSharedRefDbConfiguration());
    bind(GitRepositoryManager.class).to(SharedRefDbGitRepositoryManager.class);
    if (cfg.getSharedRefDbConfiguration().getSharedRefDb().getEnforcementRules().isEmpty()) {
      bind(SharedRefEnforcement.class).to(DefaultSharedRefEnforcement.class).in(Scopes.SINGLETON);
    } else {
      bind(SharedRefEnforcement.class)
          .to(CustomSharedRefEnforcementByProject.class)
          .in(Scopes.SINGLETON);
    }
  }
}
```

## Ignored Refs - Optional

Consumers of this library can specify an optional set of refs that should not
be validated against the shared ref-db, this list should be provided via Guice
named binding, for example:

```java

public class FooModule extends AbstractModule {

@Override
  protected void configure() {
    // other bindings ...
    bind(new TypeLiteral<ImmutableSet<String>>() {})
        .annotatedWith(Names.named(SharedRefDbGitRepositoryManager.IGNORED_REFS))
        .toInstance(
            ImmutableSet.of(
                "refs/foo/bar",
                "refs/foo/baz"));
    // other bindings ...
  }
}
```