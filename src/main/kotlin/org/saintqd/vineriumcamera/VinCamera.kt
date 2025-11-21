package org.saintqd.vineriumcamera

import com.Zrips.CMI.CMI
import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.math.Position
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.util.TriState
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.saintqd.vineriumlib.utils.VinUtils
import java.util.concurrent.ThreadLocalRandom

class VinCamera {

    var cameraPlayer : Player? = null
    var watchedPlayer : Player? = null
    var watchedPlayerMoveEvent : PlayerMoveEvent? = null
    var task : BukkitTask? = null
    var reconnectTask : BukkitTask? = null
    var lastPlayerWatchTime : Long = 0
    var lastPositionChange : Long = 0
    var actionBarMessage : Component = Component.empty()
    var lightSourceMaterials = mutableSetOf<Material>()
    var locked = false;
    var lastCameraNickname = "";
    var reconnectEnabled = true;

    var cameraDelay : Long = 40
    var timeToPlayerChange : Long = 1200
    var maxPositionTime : Long = 300
    var minRadius : Double = 2.0
    var maxRadius : Double = 3.0
    var maxHeight : Double = 5.2
    var minHeight : Double = -0.8
    var maxPositionTries : Int = 20

    var commandsOnStart = listOf<String>()

    fun updateParams() {
        cameraDelay = VineriumCamera.inst().config.getLong("VineriumCamera.Delay")
        timeToPlayerChange = VineriumCamera.inst().config.getLong("VineriumCamera.TimeToPlayerChange")
        maxPositionTime = VineriumCamera.inst().config.getLong("VineriumCamera.MaxPositionTime")
        minRadius = VineriumCamera.inst().config.getDouble("VineriumCamera.MinRadius").coerceAtLeast(0.0)
        maxRadius = (VineriumCamera.inst().config.getDouble("VineriumCamera.MaxRadius") - minRadius).coerceAtLeast(0.0)
        maxHeight = VineriumCamera.inst().config.getDouble("VineriumCamera.MaxHeight").coerceAtMost(255.0)
        minHeight = VineriumCamera.inst().config.getDouble("VineriumCamera.MinHeight")
        maxPositionTries = VineriumCamera.inst().config.getInt("VineriumCamera.MaxPositionTries").coerceAtLeast(1)
        commandsOnStart = VineriumCamera.inst().config.getStringList("VineriumCamera.CommandsOnStart")
        lastCameraNickname = VineriumCamera.inst().config.getString("VineriumCamera.DefaultCameraNickname","")!!
        reconnectEnabled = VineriumCamera.inst().config.getBoolean("VineriumCamera.ReconnectEnabled",true)
        for (sourceMaterialString in VineriumCamera.inst().config.getStringList("VineriumCamera.LightSourceMaterials")) {
            val material = EnumUtils.getEnum(Material::class.java,sourceMaterialString.uppercase())
            if (material != null)
                lightSourceMaterials.add(material);
            else
                VinUtils.sendDebugMessage(0,"<yellow>Error loading light source info: Material ${sourceMaterialString.uppercase()} is not valid.")
        }
        reconnectTask?.cancel()
        if (reconnectEnabled) {
            reconnectTask = object: BukkitRunnable() {
                override fun run() {
                    reconnectTask()
                }
            }.runTaskTimer(VineriumCamera.inst(),1L,600)
        }
    }

    fun startCamera(player : Player) {
        cameraPlayer = player
        lastCameraNickname = player.name
        cameraPlayer?.let {
            for (command in commandsOnStart) {
                var commandParsed = command.replace("%player_name%",it.name)
                if (VineriumCamera.inst().isPlaceholderAPIEnabled)
                    commandParsed = PlaceholderAPI.setPlaceholders(cameraPlayer,command)
                Bukkit.dispatchCommand(Bukkit.getServer().consoleSender, commandParsed)
            }
            it.viewDistance = 12
            VinUtils.sendDebugMessage(0,"VineriumCamera started with player ${it.name} as camera.")
            lastPlayerWatchTime = VinUtils.getCurrentTick()
            watchedPlayer = selectNextPlayer()
            task = object: BukkitRunnable() {
                override fun run() {
                    cameraTask()
                }
            }.runTaskTimer(VineriumCamera.inst(),1L,cameraDelay)
        }
    }

    fun stopCamera() {
        if (task != null)
            VinUtils.sendDebugMessage(0,"VineriumCamera stopped.")
        task?.cancel()
        task = null
        cameraPlayer = null
        watchedPlayer = null
        lastPlayerWatchTime = 0
    }

    fun reconnectTask() {
        cameraPlayer?.let {
            if (it.isOnline)
                return
        }
        val lastCameraPlayer = Bukkit.getPlayer(lastCameraNickname)
        VinUtils.sendDebugMessage(1,"<gray>Trying to reconnect last camera player with nickname ${lastCameraNickname}...")
        if (lastCameraPlayer != null && lastCameraPlayer.isOnline)
            startCamera(lastCameraPlayer)
        else
            VinUtils.sendDebugMessage(1,"<gray>Could not reconnect ${lastCameraNickname}: Player is null or offline.")
    }

    fun cameraTask() {
        if (cameraPlayer == null || !cameraPlayer!!.isOnline) {
            stopCamera()
            return
        }
        if (lastPlayerWatchTime < VinUtils.getCurrentTick() - timeToPlayerChange) {
            lastPlayerWatchTime = VinUtils.getCurrentTick()
            watchedPlayer = selectNextPlayer()
        }
        watchedPlayer?.let {
            if (!it.isOnline || it.hasPotionEffect(PotionEffectType.INVISIBILITY) || it.gameMode == GameMode.SPECTATOR
                || (VineriumCamera.inst().isCmiEnabled && CMI.getInstance().playerManager.getUser(it).isVanished)) {
                lastPlayerWatchTime = VinUtils.getCurrentTick()
                watchedPlayer = selectNextPlayer()
            }
            cameraPlayer!!.sendActionBar(actionBarMessage)

            val watchedPlayer = it.player
            if (cameraPlayer!!.location.world != watchedPlayer!!.location.world) {
                lastPositionChange = VinUtils.getCurrentTick()
                val nextLoc = findLoc()
                cameraPlayer!!.teleport(nextLoc)
                cameraPlayer!!.lookAt(watchedPlayer, LookAnchor.EYES,LookAnchor.EYES)
            }
            else {
                val direction = cameraPlayer!!.location.direction
                val towardsEntity = watchedPlayer.location.subtract(cameraPlayer!!.location).toVector().normalize()
                val inFront = direction.distance(towardsEntity)
                if (inFront > 0.9
                    || !cameraPlayer!!.hasLineOfSight(watchedPlayer)
                    || lastPositionChange < VinUtils.getCurrentTick() - maxPositionTime
                    || cameraPlayer!!.location.distance(watchedPlayer.location) > 30) {
                    lastPositionChange = VinUtils.getCurrentTick()
                    cameraPlayer!!.spectatorTarget = null
                    var nextLoc = findLoc()
                    var cameraOffset = 3
                    watchedPlayerMoveEvent?.let { lastMoveEvent ->
                        val moveDistance = lastMoveEvent.to.distance(lastMoveEvent.from)
                        cameraOffset = cameraOffset + (cameraOffset * moveDistance).toInt()
                        if (moveDistance < 0.1)
                            cameraOffset = 0
                    }
                    if (nextLoc.block.lightLevel < 5 && nextLoc.block.lightFromSky < 4
                        && !lightSourceMaterials.contains(watchedPlayer.inventory.itemInMainHand.type)
                        && !lightSourceMaterials.contains(watchedPlayer.inventory.itemInOffHand.type))
                        cameraPlayer!!.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION,600,0,false,false))
                    else
                        cameraPlayer!!.removePotionEffect(PotionEffectType.NIGHT_VISION)

                    cameraPlayer!!.teleport(nextLoc)
                    cameraPlayer!!.lookAt(Position.fine(watchedPlayer.eyeLocation.add(watchedPlayer.eyeLocation.direction.multiply(cameraOffset))),LookAnchor.EYES)
                }
            }
        }
    }

    private fun findLoc() : Location {
        if (watchedPlayer == null)
            return cameraPlayer!!.location
        val playerLoc = watchedPlayer!!.location
        var playerEyeLoc = Location(playerLoc.world,playerLoc.x,playerLoc.y+1.62,playerLoc.z)
        var possibleHeight = 0
        var maxPossibleHeight = (playerEyeLoc.blockY+maxHeight).toInt()
        for (possibleY in playerEyeLoc.blockY..maxPossibleHeight) {
            if (playerEyeLoc.world.getBlockAt(playerEyeLoc.blockX, possibleY, playerEyeLoc.blockZ).type != Material.AIR) {
                break
            }
            possibleHeight++
        }
        var finalMaxHeight = if (possibleHeight == maxPossibleHeight) maxHeight else (possibleHeight-1).toDouble()
        finalMaxHeight = finalMaxHeight.coerceAtLeast(minHeight+0.01)
        for (currentTries in 1..maxPositionTries) {
            var minRadius = maxRadius * -1
            var randomX = ThreadLocalRandom.current().nextDouble(minRadius,maxRadius)
            if (randomX >= 0)
                randomX += minRadius
            else
                randomX -= minRadius
            var randomZ = ThreadLocalRandom.current().nextDouble(minRadius,maxRadius)
            if (randomZ >= 0)
                randomZ += minRadius
            else
                randomZ -= minRadius
            val randomY = ThreadLocalRandom.current().nextDouble(minHeight,finalMaxHeight)
            var possibleLoc = Location(playerEyeLoc.world,playerEyeLoc.x()+randomX,playerEyeLoc.y()+randomY,playerEyeLoc.z()+randomZ)
            if (possibleLoc.world.lineOfSightExists(possibleLoc,playerEyeLoc)) {
                possibleLoc.y -= 1.0
                if (possibleLoc.world.getBlockAt(possibleLoc.blockX, possibleLoc.blockY, playerEyeLoc.blockZ).type != Material.AIR) {
                    possibleLoc.y -= 1.0
                }
                return possibleLoc
            }
        }
        var fallbackLoc = playerLoc
        fallbackLoc.pitch = 0f
        fallbackLoc = fallbackLoc.add(fallbackLoc.direction.multiply(-2))
        if (!fallbackLoc.world.lineOfSightExists(fallbackLoc,playerEyeLoc))
            cameraPlayer!!.spectatorTarget = watchedPlayer!!.player
        return fallbackLoc
    }

    fun getPossiblePlayers() : List<Player> {
        val possiblePlayers = mutableListOf<Player>()
        val onlinePlayers = Bukkit.getOnlinePlayers().sortedBy { player -> player.name }

        for (possiblePlayer in onlinePlayers) {
            // Игрокам с премиум-статусом выдаётся перм vineriumcamera.toggleshow.
            //   Командой /camera toggle они могут отключить показ себя на камере.
            //   Отключение производится за счёт выдачи им перма vineriumcamera.showforbid.
            //   Админам и модерам
            if (possiblePlayer.gameMode == GameMode.SPECTATOR
                || possiblePlayer.isDead || possiblePlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                continue
            }
            val permissionState = possiblePlayer.permissionValue("vineriumcamera.showforbid")
            if (possiblePlayer.hasPermission("vineriumcamera.toggleshow") && (permissionState == TriState.TRUE)) {
                continue
            }
            if (VineriumCamera.inst().isCmiEnabled) {
                val cmiUser = CMI.getInstance().playerManager.getUser(possiblePlayer)
                if (cmiUser.isAfk || cmiUser.isVanished || cmiUser.isJailed) {
                    continue
                }
            }
            possiblePlayers.add(possiblePlayer)
        }
        possiblePlayers.remove(cameraPlayer)
        return possiblePlayers
    }

    fun selectNextPlayer() : Player? {
        val possiblePlayers = getPossiblePlayers()
        if (locked && watchedPlayer != null)
            return watchedPlayer

        var lastPlayerIndex = 0
        if (watchedPlayer != null) {
            for (indexedPlayer in possiblePlayers) {
                if (indexedPlayer == watchedPlayer!!.player)
                    break
                lastPlayerIndex++
            }
            if (possiblePlayers.size > lastPlayerIndex+1) {
                lastPlayerIndex = lastPlayerIndex+1
            }
            else lastPlayerIndex = 0
        }

        if (possiblePlayers.isEmpty()) return null
        val selectedPlayer = possiblePlayers[lastPlayerIndex]
        actionBarMessage = Component.text(selectedPlayer.name).color(NamedTextColor.GRAY)
        VinUtils.sendDebugMessage(2,"VineriumCamera is now watching player ${selectedPlayer.name}.")
        return selectedPlayer
    }
}