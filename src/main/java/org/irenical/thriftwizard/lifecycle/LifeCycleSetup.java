package org.irenical.thriftwizard.lifecycle;

import org.irenical.lifecycle.LifeCycle;
import org.irenical.lifecycle.builder.CompositeLifeCycle;
import org.irenical.thriftwizard.Application;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifeCycleSetup {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifeCycleSetup.class);

  private final Application application;
  private final CompositeLifeCycle lifeCycles;

  public LifeCycleSetup(Application application, CompositeLifeCycle lifeCycles) {
    this.application = application;
    this.lifeCycles = lifeCycles;
  }

  public CompositeLifeCycle append(LifeCycle child) {
    return lifeCycles.append(child);
  }

  public void autoConfig() {
    autoConfig(application.getClass().getPackage().getName());
  }

  private void autoConfig(String packageName) {
    ConfigurationBuilder config = new ConfigurationBuilder().setScanners(new SubTypesScanner())
        .setUrls(ClasspathHelper.forPackage(packageName));
    Reflections subTypeReflector = new Reflections(config);
    for (Class<? extends LifeCycle> lifeCycleClass : subTypeReflector.getSubTypesOf(LifeCycle.class)) {
      try {
        lifeCycles.append(lifeCycleClass.newInstance());
      } catch (ReflectiveOperationException e) {
        LOGGER.error("LifeCycle " + lifeCycleClass.getCanonicalName() +
            " has no no-args constructor, cannot autoconfigure");
        return;
      }
    }
  }
}
