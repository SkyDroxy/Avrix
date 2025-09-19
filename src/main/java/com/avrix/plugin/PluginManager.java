package com.avrix.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.avrix.agent.Agent;
import com.avrix.enums.Environment;
import com.avrix.utils.Constants;
import com.avrix.utils.PatchUtils;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import zombie.core.Core;

/**
 * The PluginManager class manages the loading, initialization, and handling of plugins within the application context.
 */
public class PluginManager {
    private static final List<Metadata> pluginsList = new ArrayList<>(); // A list containing metadata for loaded plugins.

    /**
     * Prints information about loaded plugins to the console.
     * The information includes plugin names, IDs, and versions.
     */
    private static void printLoadedPluginsInfo() {
        StringBuilder sb = new StringBuilder("Loaded plugins:\n");
        for (Metadata plugin : pluginsList) {
            sb.append("    - ").append(plugin.getName())
                    .append(" (ID: ").append(plugin.getId())
                    .append(", Version: ").append(plugin.getVersion())
                    .append(")\n");
        }
        System.out.print(sb);
    }

    /**
     * Loads fallback internal logical modules (only if they were not discovered inside Avrix-Core.jar).
     */
    private static void loadFallbackInternalModulesIfMissing() {
        boolean hasPzCore = pluginsList.stream().anyMatch(m -> "pz-core".equals(m.getId()));
        boolean hasAvrixLoader = pluginsList.stream().anyMatch(m -> "avrix-loader".equals(m.getId()));
        if (hasPzCore && hasAvrixLoader) return; // nothing to do

        // These will only be added if not already provided by bundled metadata inside avrix-core
        if (!hasPzCore) {
            pluginsList.add(new Metadata.MetadataBuilder()
                .name("Project Zomboid")
                .id("pz-core")
                .author("The Indie Stone")
                .environment("both")
                .version(Core.getInstance().getVersion())
                .license("Complex")
                .entryPointsList(Collections.emptyList())
                .internal(true)
                .parent("avrix-core")
                .build());
        }

        if (!hasAvrixLoader) {
            pluginsList.add(new Metadata.MetadataBuilder()
                .name(Constants.AVRIX_NAME + " Loader")
                .id("avrix-loader")
                .author("Brov3r")
                .environment("both")
                .version(Constants.AVRIX_VERSION)
                .license("GNU GPLv3")
                .contacts("https://github.com/Brov3r/Avrix")
                .entryPointsList(Collections.emptyList())
                .internal(true)
                .parent("avrix-core")
                .build());
        }
    }

    /**
     * Scans the provided Avrix-Core jar for bundled internal plugin metadata YAML files.
     * Internal plugin metadata YAML files must reside under the directory 'internal-plugins/' inside the jar
     * and have the suffix '.yml'. Each YAML file follows the standard metadata schema.
     *
     * Required minimal keys per internal plugin YAML: id, name, author, version, license.
     * Optional keys: description, environment, contacts, entrypoints, patches, dependencies, image, imageUrl.
     * The loader will automatically set internal=true and parent=avrix-core.
     */
    private static void loadBundledInternalPlugins(File avrixCoreJar, Metadata coreMeta) {
        try (JarFile jar = new JarFile(avrixCoreJar)) {
            jar.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().startsWith("internal-plugins/"))
                .filter(e -> e.getName().endsWith(".yml"))
                .forEach(entry -> addInternalMetadataFromEntry(avrixCoreJar, entry, coreMeta));
        } catch (IOException e) {
            System.out.println("[!] Failed to scan Avrix-Core jar for internal plugins: " + e.getMessage());
        }
    }

    private static void addInternalMetadataFromEntry(File jarFile, JarEntry entry, Metadata coreMeta) {
        // Reuse YamlFile jar constructor
        com.avrix.utils.YamlFile yamlFile = new com.avrix.utils.YamlFile(jarFile.getAbsolutePath(), entry.getName());
        if (yamlFile.isEmpty()) return;

        String id = yamlFile.getString("id");
        if (id == null) return; // invalid internal metadata
        // Avoid duplicates (fallback or already present)
        if (pluginsList.stream().anyMatch(m -> id.equals(m.getId()))) return;

        Metadata meta = new Metadata.MetadataBuilder()
            .name(yamlFile.getString("name"))
            .id(id)
            .description(yamlFile.getString("description"))
            .author(yamlFile.getString("author"))
            .version(yamlFile.getString("version"))
            .license(yamlFile.getString("license"))
            .contacts(yamlFile.getString("contacts"))
            .environment(yamlFile.getString("environment"))
            .entryPointsList(yamlFile.getStringList("entrypoints"))
            .patchList(yamlFile.getStringList("patches"))
            .dependencies(yamlFile.getStringMap("dependencies"))
            .image(yamlFile.getString("image"))
            .imageUrl(yamlFile.getString("imageUrl"))
            .internal(true)
            .parent(coreMeta.getId())
            // We can point pluginFile to the core jar so entry points/patches resolve correctly
            .pluginFile(jarFile)
            .build();

        pluginsList.add(meta);
        System.out.printf("[#] Discovered bundled internal plugin '%s' (ID: %s) inside Avrix-Core jar.%n", meta.getName(), meta.getId());
    }

    /**
     * Loading plugins into the game context
     *
     * @throws Exception in case of any problems
     */
    public static void loadPlugins() throws Exception {
        // Plugin loading mode (client, server)
        Environment loaderEnvironment = Environment.fromString(System.getProperty("avrix.mode"));

        // First attempt to load avrix-core jar (sibling to launcher executable) and any bundled internal plugins
        File avrixCoreJar = new File("Avrix-Core.jar");
        if (avrixCoreJar.exists()) {
            Metadata coreMeta = Metadata.createFromJar(avrixCoreJar, Constants.PLUGINS_METADATA_NAME);
            if (coreMeta != null) {
                pluginsList.add(coreMeta);
                // Scan for bundled internal plugin metadata inside the core jar
                loadBundledInternalPlugins(avrixCoreJar, coreMeta);
            } else {
                System.out.println("[?] Avrix-Core.jar present but no metadata.yml inside.");
            }
        }

        // Fallback: add internal logical modules programmatically if not provided by jar
        loadFallbackInternalModulesIfMissing();

        // Getting valid plugins from plugins folder
        for (File plugin : getPluginFiles()) {
            Metadata metadata = Metadata.createFromJar(plugin, Constants.PLUGINS_METADATA_NAME);

            if (metadata == null) {
                System.out.printf("[?] No metadata found for the potential plugin '%s'. Skipping...%n", plugin.getName());
                continue;
            }

            if (!metadata.getPluginFile().exists()) {
                System.out.printf("[!] Could not access the plugin file for '%s'. Skipping...%n", plugin.getName());
                continue;
            }

            if (metadata.getEnvironment() != loaderEnvironment && metadata.getEnvironment() != Environment.BOTH) {
                System.out.printf("[?] Plugin '%s' found with inappropriate environment (Loader: '%s', Plugin: '%s'). Skipping...%n",
                        plugin.getName(),
                        loaderEnvironment.getValue(),
                        metadata.getEnvironment().getValue());
                continue;
            }

            // creating a folder for configs
            File configFolder = metadata.getConfigFolder();
            if (!configFolder.exists()) {
                try {
                    configFolder.mkdir();
                } catch (Exception e) {
                    System.out.printf("[!] An error occurred while creating the config folder for plugin '%s': %s%n", metadata.getId(), e.getMessage());
                }
            }

            pluginsList.add(metadata);
        }

        // Loading the plugin
        for (Metadata metadata : Metadata.sortMetadata(pluginsList)) {
            File pluginFile = metadata.getPluginFile();

            if (pluginFile == null) continue;

            Environment environment = metadata.getEnvironment();

            // Checking the environment
            if (environment != loaderEnvironment && environment != Environment.BOTH) continue;

            // Creating a URL for the plugin
            URL pluginUrl = pluginFile.toURI().toURL();

            // Adding jar file to classpath
            Agent.addClassPath(pluginFile);

            ClassLoader classLoader = new PluginClassLoader(metadata.getId(), new URL[]{pluginUrl}, ClassLoader.getSystemClassLoader());

            // Extending ClassPoll with a new ClassLoader
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(classLoader));

            // Applying patches
            PatchUtils.applyPluginPatches(metadata, classLoader);

            // Loading the plugin
            System.out.printf("[#] Loading plugin '%s' (ID: %s, Version: %s)...%n", metadata.getName(), metadata.getId(), metadata.getVersion());
            loadPlugin(metadata, classLoader);
        }

        // Displaying information about loaded plugins
        printLoadedPluginsInfo();
    }

    /**
     * Loads and initializes plugin entry points.
     *
     * @param metadata    The {@link Metadata} of the plugin.
     * @param classLoader The {@link PluginClassLoader} to use for loading the plugin classes.
     * @throws ClassNotFoundException    If a specified class cannot be found.
     * @throws NoSuchMethodException     If the constructor with Metadata parameter is not found.
     * @throws InvocationTargetException If the underlying constructor throws an exception.
     * @throws InstantiationException    If the class that declares the underlying constructor represents an abstract class.
     * @throws IllegalAccessException    If the constructor is inaccessible.
     */
    private static void loadPlugin(Metadata metadata, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (metadata.getEntryPoints() == null || metadata.getEntryPoints().isEmpty()) {
            return;
        }

        for (String entryPoint : metadata.getEntryPoints()) {
            Class<?> pluginClass = Class.forName(entryPoint, true, classLoader);
            Plugin pluginInstance = (Plugin) pluginClass.getDeclaredConstructor(Metadata.class).newInstance(metadata);
            pluginInstance.onInitialize();
        }
    }

    /**
     * Finds all JAR plugins in the specified directory. Also checks for the presence of a folder and
     * creates it if it is missing.
     *
     * @return List of JAR files.
     * @throws IOException in cases of input/output problems
     */
    private static List<File> getPluginFiles() throws IOException {
        File folder = new File(Constants.PLUGINS_FOLDER_NAME);

        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("[!] Failed to create the plugins folder.");
        }

        if (!folder.isDirectory()) {
            throw new IOException("[!] Path '" + folder.getPath() + "' is not a directory. Remove and try again!");
        }

        ArrayList<File> jarFiles = new ArrayList<>();

        File[] files = folder.listFiles((File pathname) -> pathname.isFile() && pathname.getName().endsWith(".jar"));

        if (files != null) {
            Collections.addAll(jarFiles, files);
        }

        return jarFiles;
    }
}