package ru.mcfine.bungeejoin.bungeejoin;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class BungeeJoin extends Plugin {

    public static BungeeJoin plugin;
    public static Map<String, Long> names = new HashMap<>();
    public static boolean dirty = false;
    public static
    File file;
    public static Configuration configuration;

    @Override
    public void onEnable() {
        plugin = this;
        getProxy().getPluginManager().registerListener(this, new Listeners());
        file = new File(getDataFolder(), "players.yml");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (configuration.getSection("player-names") == null) {
            configuration.set("player-names", new HashMap<>() {{
                put("GrocerMC", System.currentTimeMillis());
            }});
            dirty=true;
        }
        if (configuration.getString("token", "null").equals("null")) {
            configuration.set("token", "null");
            dirty=true;
        }
        loadConfig();

        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                Listeners.permCache.clear();
                if (dirty) {
                    try {
                        configuration.set("player-names", names);
                        ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
                        dirty = false;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, 1, 200, TimeUnit.SECONDS);
    }

    private void loadConfig() {
        for (String key : configuration.getSection("player-names").getKeys()) {
            Long time = configuration.getLong("player-names." + key);
            names.put(key, time);
        }
    }

    @Override
    public void onDisable() {
        try {
            configuration.set("player-names", names);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            dirty = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
