package org.irenical.thriftwizard;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import org.irenical.jindy.ConfigFactory;
import org.irenical.lifecycle.builder.CompositeLifeCycle;
import org.irenical.thriftwizard.configuration.ArchaiusLifeCycle;
import org.irenical.thriftwizard.logging.LoggingLifeCycle;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public abstract class Application {
  static {
    setUpLogging();
  }

  /**
   * Set up basic, console-based logging to be used until we load configuration and reset logging
   * configuration accordingly.
   */
  private static void setUpLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.INFO);
  }

  protected final void run(String[] args) {
    ArchaiusLifeCycle archaiusLifeCycle = new ArchaiusLifeCycle(this);
    LoggingLifeCycle loggingLifeCycle = new LoggingLifeCycle();

    // start these two early, code after might depend on logging
    archaiusLifeCycle.start();
    loggingLifeCycle.start();

    CompositeLifeCycle compositeLifeCycle = new CompositeLifeCycle();
    compositeLifeCycle.append(archaiusLifeCycle).append(loggingLifeCycle);

    run(new Setup(this, compositeLifeCycle, ConfigFactory.getConfig()));

    compositeLifeCycle.withShutdownHook();
    compositeLifeCycle.start();
  }

  /**
   * Implement this to return the application's identifier.
   *
   * It should be a short string, unique for your domain, that identifies fully the application,
   * without version numbers.
   *
   * @return this application's identifier
   */
  public abstract String getName();

  /**
   * Override this method in order to perform manual setup of life cycles and Thrift servers.
   *
   * @param setup the setup object that provides access to application setup
   */
  public void run(Setup setup) {
    setup.autoConfig();
  }
}
