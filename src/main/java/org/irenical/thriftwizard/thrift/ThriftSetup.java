package org.irenical.thriftwizard.thrift;

import org.apache.thrift.TProcessor;
import org.irenical.thriftwizard.Application;
import org.irenical.thriftwizard.Setup;
import org.irenical.thrifty.ThriftServerLifeCycle;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public class ThriftSetup {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThriftSetup.class);

  private final Application application;
  private final Setup setup;

  public ThriftSetup(Application application, Setup setup) {
    this.application = application;
    this.setup = setup;
  }

  public void autoConfig() {
    autoConfig(application.getClass().getPackage().getName());
  }

  public void autoConfig(String packageName) {
    Class<?> serverClass = null;
    Constructor<? extends TProcessor> processorConstructor = null;
    Constructor<?> serverConstructor = null;

    // look for all subclasses of libthrift's TProcessor
    ConfigurationBuilder config = new ConfigurationBuilder().setScanners(new SubTypesScanner())
        .setUrls(ClasspathHelper.forPackage(packageName));
    Reflections subTypeReflector = new Reflections(config);
    for (Class<? extends TProcessor> processorClass : subTypeReflector.getSubTypesOf(TProcessor.class)) {
      // thrift stubs/ties have a TProcessor as an inner class
      Class<?> contractClass = processorClass.getEnclosingClass();
      if (contractClass == null) {
        // not a regular thrift generated contract class
        continue;
      }

      // thrift stubs/ties have a inner class called "Iface"
      Class<?> ifaceClass = null;
      for (Class<?> innerClass : contractClass.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("Iface")) {
          ifaceClass = innerClass;
        }
      }
      if (ifaceClass == null) {
        // again, not a regular thrift generated contract class
        continue;
      }

      // the processor class we got should have a constructor with a single argument of the exact
      // type of the iface we got, otherwise something is really wrong
      Constructor<? extends TProcessor> tentativeProcessorCtor;
      try {
        tentativeProcessorCtor = processorClass.getConstructor(ifaceClass);
      } catch (NoSuchMethodException e) {
        // someone is seriously messing with us now
        continue;
      }

      // get all implementations of our Iface, excluding the contract class
      ConfigurationBuilder config2 = new ConfigurationBuilder().setScanners(new SubTypesScanner())
          .setUrls(ClasspathHelper.forPackage(packageName))
          .filterInputsBy(new FilterBuilder().exclude(contractClass.getCanonicalName() + "\\$.*"));
      Reflections subTypeReflectorNoContract = new Reflections(config2);
      for (Class<?> implClass : subTypeReflectorNoContract.getSubTypesOf(ifaceClass)) {
        // did we already have a match before? we can't autoconfigure, then
        if (serverClass != null) {
          LOGGER.error("Multiple Thrift server implementations found in package " + packageName +
              " for " + contractClass.getCanonicalName() + ", unable to auto-configure server");
          return;
        }

        try {
          serverConstructor = implClass.getConstructor();
        } catch (NoSuchMethodException e) {
          LOGGER.error("Implementation of Thrift contract " + contractClass.getCanonicalName() +
              ", " + implClass.getCanonicalName() + " has no no-args constructor, cannot autoconfigure");
          return;
        }

        // everything checks out so far, we have a match
        serverClass = implClass;
        processorConstructor = tentativeProcessorCtor;
      }

      // search every other subclass of TProcessor to get the corresponding thrift contract class
      // it's okay if there's more contracts, but only one can have an implementation of its Iface
    }

    if (serverClass == null) {
      LOGGER.info("No Thrift servers in " + packageName + " available to configure");
      return;
    }

    Object thriftServer;
    try {
      thriftServer = serverConstructor.newInstance();
    } catch (ReflectiveOperationException e) {
      LOGGER.error("Error instantiating " + serverClass.getCanonicalName(), e);
      return;
    }

    TProcessor tProcessor;
    try {
      tProcessor = processorConstructor.newInstance(thriftServer);
    } catch (ReflectiveOperationException e) {
      LOGGER.error("Error initializing Thrift processor", e);
      return;
    }

    register(tProcessor);
  }

  /**
   * Registers a Thrift server in the application lifecycle. Will default to the name of the current
   * application and will read the following properties from the default configuration.
   *
   * <pre>
   * myappname.thrift.listenPort=1337 (mandatory)
   * myappname.thrift.selectorThreads=2
   * myappname.thrift.workerThreads=4
   * myappname.thrift.shutdownTimeoutMillis=10000
   * </pre>
   *
   * Can only be called during initialization phase, otherwise will throw an
   * {@link IllegalStateException}.
   *
   * @param thriftProcessor TProcessor from the Thrift generated classes
   */
  public void register(TProcessor thriftProcessor) {
    setup.lifeCycles().append(new ThriftServerLifeCycle(thriftProcessor, setup.config(), application.getName()));
  }

  /**
   * Registers a Thrift server in the application lifecycle. Will use the server name given in
   * {@code serverName} and will read the following properties from the default configuration, where
   * <code>myservername</code> corresponds to the given server name.
   *
   * <pre>
   * myservername.thrift.listenPort=1337 (mandatory)
   * myservername.thrift.selectorThreads=2
   * myservername.thrift.workerThreads=4
   * myservername.thrift.shutdownTimeoutMillis=10000
   * </pre>
   *
   * Can only be called during initialization phase, otherwise will throw an
   * {@link IllegalStateException}.
   *
   * @param thriftProcessor TProcessor from the Thrift generated classes
   * @param serverName name to be given to this Thrift server instance, also used as the prefix
   *                   for settings that will be read from the default configuration
   */
  public void register(TProcessor thriftProcessor, String serverName) {
    setup.lifeCycles().append(new ThriftServerLifeCycle(thriftProcessor, setup.config(), serverName));
  }
}
