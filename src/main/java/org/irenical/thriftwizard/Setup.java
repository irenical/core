package org.irenical.thriftwizard;

import org.irenical.jindy.Config;
import org.irenical.lifecycle.builder.CompositeLifeCycle;
import org.irenical.thriftwizard.lifecycle.LifeCycleSetup;
import org.irenical.thriftwizard.thrift.ThriftSetup;

public class Setup {
  private final LifeCycleSetup lifeCycleSetup;
  private final ThriftSetup thriftSetup;
  private final Config config;

  public Setup(Application application, CompositeLifeCycle compositeLifeCycle, Config config) {
    this.config = config;
    this.lifeCycleSetup = new LifeCycleSetup(application, compositeLifeCycle);
    this.thriftSetup = new ThriftSetup(application, this);
  }

  public ThriftSetup thrift() {
    return thriftSetup;
  }

  public LifeCycleSetup lifeCycles() {
    return lifeCycleSetup;
  }

  public Config config() {
    return config;
  }

  public void autoConfig() {
    lifeCycleSetup.autoConfig();
    thriftSetup.autoConfig();
  }
}
