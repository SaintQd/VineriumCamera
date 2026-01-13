package org.saintqd.vineriumcamera;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.saintqd.vineriumcamera.commands.VinCameraCommandsManager;
import org.saintqd.vineriumcamera.listeners.PlayerListener;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.ResourceUtils;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
        try {
            ResourceUtils.fetchAllResources(this,getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        HashMap<Key,String> langLines = VineriumLib.inst().getLangManager().loadLanguageFile(this,
                plugin.getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml");
        VineriumLib.inst().getLangManager().registerLangLines(langLines);

        cameraInstance.updateParams();
        getLogger().info("VineriumCamera params updated.");
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
