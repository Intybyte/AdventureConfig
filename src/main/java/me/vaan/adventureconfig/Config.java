package me.vaan.adventureconfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private final File file;

    private final Map<String, Object> configMap = new HashMap<>();
    private final Map<String, Component> componentMap = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public Config(File config) {
        this.file = config;
        try (FileInputStream fis = new FileInputStream(config)) {
            Yaml yaml = new Yaml();

            Map<String, Object> map = yaml.load(fis);
            deepLoadKeys("", map);
        } catch (IOException e) {
            throw new RuntimeException("Error loading config file: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            configMap.clear();
            componentMap.clear();

            Map<String, Object> map = yaml.load(fis);
            deepLoadKeys("", map);

        } catch (IOException e) {
            throw new RuntimeException("Error reloading config file: " + e.getMessage());
        }
    }

    public Object get(String key) {
        Object value = configMap.get(key);
        if (value == null) {
            throw new RuntimeException("Error: " + key + " not found.");
        }

        return value;
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public List<String> getSList(String key) {
        return (List<String>) get(key);
    }

    public int getInt(String key) {
        return (Integer) get(key);
    }

    public boolean getBool(String key) {
        return (Boolean) get(key);
    }

    // don't deserialize stuff again if it has no argument to evaluate
    public Component getComponent(String key) {
        Component cmp = componentMap.get(key);
        if (cmp != null) {
            return cmp;
        }

        cmp = mm.deserialize(getString(key));
        componentMap.put(key, cmp);
        return cmp;
    }

    // Recursive method to deep load all keys
    private void deepLoadKeys(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();  // Create a "dot" notation for keys
            Object value = entry.getValue();

            if (value instanceof Map) {
                deepLoadKeys(key, (Map<String, Object>) value);
            } else {
                configMap.put(key, value);
            }
        }
    }
}