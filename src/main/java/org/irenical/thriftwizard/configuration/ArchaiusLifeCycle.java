package org.irenical.thriftwizard.configuration;

import com.netflix.config.DynamicConfiguration;

import org.apache.commons.configuration.AbstractConfiguration;
import org.irenical.jindy.ConfigFactory;
import org.irenical.jindy.archaius.ArchaiusBaseFactory;
import org.irenical.lifecycle.LifeCycle;
import org.irenical.thriftwizard.Application;

public class ArchaiusLifeCycle implements LifeCycle {
  private boolean started = false;
  private Application application;

  public ArchaiusLifeCycle(Application application) {
    this.application = application;
  }

  @Override
  public void start() {
    if (!started) {
      ConfigFactory.setDefaultAppName(application.getName());
      ConfigFactory.setDefaultConfigFactory(new ArchaiusBaseFactory() {
        @Override
        protected AbstractConfiguration getConfiguration() {
          return new DynamicConfiguration();
        }
      });

      // eagerly init main config singleton
      ConfigFactory.getConfig();

      started = true;
    }
  }

  @Override
  public void stop() {
    ConfigFactory.clear();
    started = false;
  }

  @Override
  public boolean isRunning() {
    return started;
  }
}
