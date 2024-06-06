package net.uku3lig.oldinput;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.util.plugins.Plugins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * this class has been basically taken from jinput 2.0.11-SNAPSHOT on 06/06/24 and heavily modified to suck less.
 * I hate this so much, but I don't realistically have any other option unless I want to explode forever
 */
public class OldinputControllerEnvironment extends ControllerEnvironment {
    private static final Logger log = LogManager.getLogger(OldinputControllerEnvironment.class);

    /**
     * List of all controllers in this environment
     */
    private ArrayList<Controller> controllers;

    private final Collection<String> loadedPluginNames = new ArrayList<>();

    /**
     * Returns a list of all controllers available to this environment,
     * or an empty array if there are no controllers in this environment.
     */
    @Override
    public Controller[] getControllers() {
        if (this.controllers == null) {
            // Controller list has not been scanned.
            this.controllers = new ArrayList<>();
            scanControllers();

            List<String> classes = new ArrayList<>();

            //Check the properties for specified controller classes
            String osName = System.getProperty("os.name", "").trim();

            if (osName.equalsIgnoreCase("Linux")) {
                classes.add("net.java.games.input.LinuxEnvironmentPlugin");
            } else if (osName.equalsIgnoreCase("Mac OS X")) {
                classes.add("net.java.games.input.OSXEnvironmentPlugin");
            } else if (osName.contains("Windows")) {
                // i sincerely hope no one uses this stupid mod on Windows 98 or Windows 2000
                // because this will NOT work
                classes.add("net.java.games.input.DirectAndRawInputEnvironmentPlugin");
            } else {
                log.warn("Trying to use default plugin, OS name {} not recognised", osName);
            }

            for (String className : classes) {
                try {
                    if (!loadedPluginNames.contains(className)) {
                        log.info("Loading: {}", className);
                        Class<?> ceClass = Class.forName(className);
                        ControllerEnvironment ce = (ControllerEnvironment) ceClass.getDeclaredConstructor().newInstance();
                        // usually here we would check if the environment is supported
                        // HOWEVER, we don't do that here :3c
                        addControllers(ce.getControllers());
                        loadedPluginNames.add(ce.getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("Could not load class {}", className, e);
                }
            }
        }

        if (this.controllers.isEmpty()) {
            log.warn("could not find any controllers ?!?! o ja pierdole...");
        }

        return this.controllers.toArray(new Controller[0]);
    }

    /* This is jeff's new plugin code using Jeff's Plugin manager */
    public void scanControllers() {
        log.info("scanning controllers wee oo wee oo");

        String pluginPathName = System.getProperty("jinput.controllerPluginPath");
        if (pluginPathName == null) {
            pluginPathName = "controller";
        }

        scanControllersAt(System.getProperty("java.home") + File.separator + "lib" + File.separator + pluginPathName);
        scanControllersAt(System.getProperty("user.dir") + File.separator + pluginPathName);
    }

    private void scanControllersAt(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }

        try {
            Plugins plugins = new Plugins(file);
            @SuppressWarnings("unchecked") Class<ControllerEnvironment>[] envClasses = plugins.getExtends(ControllerEnvironment.class);
            for (Class<ControllerEnvironment> envClass : envClasses) {
                log.info("ControllerEnvironment {} loaded by {}", envClass.getName(), envClass.getClassLoader());
                ControllerEnvironment ce = envClass.getDeclaredConstructor().newInstance();
                if (ce.isSupported()) {
                    addControllers(ce.getControllers());
                    loadedPluginNames.add(ce.getClass().getName());
                } else {
                    log.warn("{} is not supported", envClass.getName());
                }
            }
        } catch (Exception e) {
            log.error("Could not scan controllers", e);
        }
    }

    /**
     * Add the array of controllers to our list of controllers.
     */
    private void addControllers(Controller[] c) {
        controllers.addAll(Arrays.asList(c));
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
