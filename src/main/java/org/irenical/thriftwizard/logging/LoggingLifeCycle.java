package org.irenical.thriftwizard.logging;

import ch.qos.logback.classic.LoggerContext;

import org.irenical.lifecycle.LifeCycle;
import org.irenical.slf4j.LoggerConfigurator;
import org.slf4j.LoggerFactory;

public class LoggingLifeCycle implements LifeCycle {

  private boolean started = false;

  @Override
  public void start() {
    if (!started) {
      LoggerConfigurator configurator = new LoggerConfigurator();
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

      configurator.setContext(context);
      context.reset();
      configurator.configure(context);

//      StatusPrinter.printInCaseOfErrorsOrWarnings(context);

      // TODO: provide a way to configure levels for packages

      started = true;
    }
  }

  @Override
  public void stop() {
    ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    started = false;
  }

  @Override
  public boolean isRunning() {
    return started;
  }
}
