package org.saintqd.vineriumcamera;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.saintqd.vineriumcamera.commands.VinCameraCommandsManager;
import org.saintqd.vineriumcamera.listeners.PlayerListener;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

public class VineriumCamera extends JavaPlugin {

    private static VineriumCamera plugin;
    private VinCamera cameraInstance;
    private boolean placeholderAPIEnabled = false;
    private boolean cmiEnabled = false;

    public static VineriumCamera inst() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        setupDefaultConfig();
        this.cameraInstance = new VinCamera();

        loadData();

        VinCameraCommandsManager.setupCommands(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Подключаем плейсхолдеры
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
        } else {
            VinUtils.sendDebugMessage(0,"<yellow>Could not find PlaceholderAPI! Placeholders won't be supported.");
        }

        if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
            cmiEnabled = true;
        } else {
            VinUtils.sendDebugMessage(0,"<yellow>Could not find CMI! Some checks won't be performed.");
        }

    }

    @Override
    public void onDisable() {
        cameraInstance.stopCamera();
        VinUtils.updateJarFile(this,this.getFile());
    }

    public void loadData() {
        reloadConfig();

        String selectedLang = getConfig().getString("VineriumCamera.Language");
        if (selectedLang != null) {
            File langFile = new File(plugin.getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml");
            if (!langFile.exists() && langFile.mkdirs()) {
                InputStream langStream = VineriumCamera.class.getResourceAsStream("/lang/"+selectedLang+".yml");
                if (langStream != null) {
                    try {
                        Files.copy(langStream, Path.of(getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml"),StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            HashMap<String,String> langLines = VineriumLib.inst().getLangManager().loadLanguageFile(this,"lang" + File.separator + selectedLang + ".yml");
            VineriumLib.inst().getLangManager().registerLangLines(this,langLines);
        }

        cameraInstance.updateParams();
        getLogger().info("VineriumCamera params updated.");
    }

    private void setupDefaultConfig() {

        FileConfiguration config = this.getConfig();

        config.addDefault("VineriumCamera.Language","ru_ru");
        config.addDefault("VineriumCamera.Delay",40L);
        config.addDefault("VineriumCamera.TimeToPlayerChange",1200L);
        config.addDefault("VineriumCamera.MaxPositionTime",300L);
        config.addDefault("VineriumCamera.MinRadius",2.0);
        config.addDefault("VineriumCamera.MaxRadius",5.0);
        config.addDefault("VineriumCamera.MaxHeight",5.2);
        config.addDefault("VineriumCamera.MinHeight",-0.8);
        config.addDefault("VineriumCamera.MaxPositionTries",20);
        config.addDefault("VineriumCamera.CommandsOnStart", List.of("gm 3 %player_name% -s"));
        config.addDefault("VineriumCamera.DefaultCameraNickname", "VinCamera");
        config.addDefault("VineriumCamera.ReconnectEnabled", true);
        config.addDefault("VineriumCamera.LightSourceMaterials", List.of(
                Material.TORCH.name(),
                Material.LANTERN.name(),
                Material.GLOWSTONE.name(),
                Material.OCHRE_FROGLIGHT.name(),
                Material.VERDANT_FROGLIGHT.name(),
                Material.PEARLESCENT_FROGLIGHT.name(),
                Material.LIGHT.name(),
                Material.SHROOMLIGHT.name(),
                Material.REDSTONE_TORCH.name(),
                Material.LAVA_BUCKET.name())
        );

        config.options().copyDefaults(true);
        this.saveConfig();
    }

    public VinCamera getCameraInstance() {
        return cameraInstance;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public boolean isCmiEnabled() {
        return cmiEnabled;
    }
}
