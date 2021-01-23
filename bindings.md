
Bindings
=========================

This library expects consumers to register Guice bindings.
Some sensible implementations are already been provided and they must be bound
by the plugin's Guice module in order to work.

Alternatively plugins can decide to provide their own implementations (or
override existing ones) and bound those instead.

This is an example of binding that would allow consuming pluging getting up and
running with this plugin:

```java
public class PluginFooModule extends FactoryModule {

  private final Configuration cfg;

  public PluginFooModule(Configuration cfg) {
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

public class PluginModule extends AbstractModule {

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