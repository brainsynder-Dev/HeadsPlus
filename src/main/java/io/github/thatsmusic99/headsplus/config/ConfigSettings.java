package io.github.thatsmusic99.headsplus.config;

import io.github.thatsmusic99.headsplus.HeadsPlus;
import io.github.thatsmusic99.headsplus.util.CachedValues;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class ConfigSettings {

    protected FileConfiguration config;
    protected File configF;
    public String conName = "";

    protected void enable(boolean nullp) {
        reloadC(nullp);
        load(nullp);
    }

    protected void load(boolean nullp) {
        getConfig().options().copyDefaults(true);
        save();
    }

    public void reloadC(boolean nullp) {
        if (configF == null) {
            configF = new File(HeadsPlus.getInstance().getDataFolder(), conName + ".yml");
        }
        config = YamlConfiguration.loadConfiguration(configF);
        load(nullp);
    }

    public void save() {
        if (configF == null || config == null) {
            return;
        }
        try {
            config.save(configF);
        } catch (IOException e) {
            HeadsPlus.getInstance().getLogger().severe("Error thrown when saving the config. If there's a second error below, ignore me and look at that instead.");
            if (HeadsPlus.getInstance().getConfiguration().getMechanics().getBoolean("debug.print-stacktraces-in-console")) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getDefaultPath() {
        return "";
    }

    public Double getDouble(String path) {
        Double price = CachedValues.getPrice(path, getConfig());
        if (price != null) {
            return price;
        } else {
            return getDouble(getDefaultPath());
        }
    }

}
