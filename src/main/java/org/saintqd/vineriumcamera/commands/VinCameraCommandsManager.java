package org.saintqd.vineriumcamera.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.saintqd.vineriumcamera.VinCamera;
import org.saintqd.vineriumcamera.VineriumCamera;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.*;

public class VinCameraCommandsManager {

    public static void setupCommands(VineriumCamera plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("vincamera")
                            .executes(commandContext -> {
                                commandContext.getSource().getSender().sendMessage(VineriumLib.inst().getLangManager().parseLangString(plugin,"not_enough_arguments"));
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.literal("reload")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        reloadCommand(ctx.getSource().getSender());
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("start")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        startCameraCommand(ctx.getSource().getSender(),null);
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ctx -> {
                                                startCameraCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("stop")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        stopCameraCommand(ctx.getSource().getSender());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("watched")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ctx -> {
                                                setWatchedPlayerCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("nextplayer")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        setNextPlayerCommand(ctx.getSource().getSender());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("info")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        infoCommand(ctx.getSource().getSender());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("lock")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        lockCommand(ctx.getSource().getSender());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("showorder")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.admin"))
                                    .executes(ctx -> {
                                        showOrderCommand(ctx.getSource().getSender(),1);
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("page", IntegerArgumentType.integer())
                                            .executes(ctx -> {
                                                showOrderCommand(ctx.getSource().getSender(),ctx.getArgument("page", Integer.class));
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("toggle")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcamera.toggleshow")
                                    && VineriumLib.inst().getVaultManager() != null)
                                    .executes(ctx -> {
                                        toggleShowCommand(ctx.getSource().getSender(),null);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ctx -> {
                                                toggleShowCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .build(),
                    "Основная команда."
            );

        });
    }

    private static void reloadCommand(CommandSender sender) {
        VineriumCamera.inst().loadData();
        if (sender instanceof Player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"command_reload"));
    }

    private static void startCameraCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;

        VinCamera camera = VineriumCamera.inst().getCameraInstance();
        if (camera.getTask() != null)
            camera.stopCamera();

        camera.startCamera(player);

        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_start", player.getName()));
    }

    private static void stopCameraCommand(CommandSender sender) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();
        camera.stopCamera();

        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_stop"));
    }

    private static void setWatchedPlayerCommand(CommandSender sender, Player player) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();
        if (camera.getTask() == null) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_not_active"));
            return;
        }

        camera.setLastPlayerWatchTime(VinUtils.getCurrentTick());
        camera.setWatchedPlayer(player);
        camera.setActionBarMessage(Component.text(player.getName()).color(NamedTextColor.GRAY));

        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_set_watched",player.getName()));
    }

    private static void setNextPlayerCommand(CommandSender sender) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();
        if (camera.getTask() == null) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_not_active"));
            return;
        }

        Player selectedPlayer = camera.selectNextPlayer();
        if (selectedPlayer != null) {
            camera.setLastPlayerWatchTime(VinUtils.getCurrentTick());
            camera.setWatchedPlayer(selectedPlayer);
            camera.setActionBarMessage(Component.text(selectedPlayer.getName()).color(NamedTextColor.GRAY));
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_set_watched",selectedPlayer.getName()));
        }
        else {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_next_player_not_found"));
        }

    }

    private static void infoCommand(CommandSender sender) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();

        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_info_header"));
        if (camera.getTask() == null) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_not_active"));
            return;
        }
        if (camera.getCameraPlayer() != null)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_info_source",camera.getCameraPlayer().getName()));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_info_source_not_found"));
        if (camera.getWatchedPlayer() != null)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_Info_target",camera.getWatchedPlayer().getName()));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_Info_target_not_found"));
        long timeToChange = VinUtils.getCurrentTick() - camera.getLastPlayerWatchTime() + camera.getTimeToPlayerChange();
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_Info_time_to_change",Long.toString(timeToChange)));
    }

    private static void lockCommand(CommandSender sender) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();

        if (camera.getLocked()) {
            camera.setLocked(false);
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_lock_off"));
        }
        else {
            camera.setLocked(true);
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_lock_on"));
        }
    }

    private static void showOrderCommand(CommandSender sender, int page) {

        VinCamera camera = VineriumCamera.inst().getCameraInstance();

        List<Player> possiblePlayers = camera.getPossiblePlayers();

        int maxPage = possiblePlayers.size() / 8 + 1;
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_order_header",Integer.toString(page),Integer.toString(maxPage)));
        if (page < 0 || page > maxPage) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_order_wrong_page"));
            return;
        }
        int startIndex = (page - 1) * 8;
        int endIndex = Math.min(possiblePlayers.size(), page * 8 - 1);
        for (int i = startIndex; i < endIndex; i++) {
            Player player = possiblePlayers.get(i);
            TriState permissionState = player.permissionValue("vineriumcamera.showforbid");
            if (camera.getCameraPlayer() == player)
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(), "camera_order_layout_camera", Integer.toString(i), player.getName()));
            else if (camera.getWatchedPlayer() == player)
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(), "camera_order_layout_current", Integer.toString(i), player.getName()));
            else if (player.hasPermission("vineriumcamera.toggleshow") && (permissionState == TriState.TRUE))
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(), "camera_order_layout_forbidden", Integer.toString(i), player.getName()));
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(), "camera_order_layout", Integer.toString(i), player.getName()));
        }
    }

    private static void toggleShowCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;

        TriState permissionState = player.permissionValue("vineriumcamera.showforbid");
        if (permissionState == TriState.FALSE || permissionState == TriState.NOT_SET) {
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerAdd(null,player, "vineriumcamera.showforbid");
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_toggle_on"));
        }
        else {
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerRemove(null,player, "vineriumcamera.showforbid");
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCamera.inst(),"camera_toggle_off"));
        }
    }

}
