package org.saintqd.vineriumcamera.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.saintqd.vineriumcamera.VinCamera;
import org.saintqd.vineriumcamera.VineriumCamera;
import org.saintqd.vineriumlib.utils.VinUtils;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (VinUtils.getCurrentTick() % 2 != 0) return;
        VinCamera camera = VineriumCamera.inst().getCameraInstance();
        if (camera == null) return;
        if (camera.getWatchedPlayer() == null) return;
        if (event.getPlayer() == camera.getWatchedPlayer())
            camera.setWatchedPlayerMoveEvent(event);

    }
}
