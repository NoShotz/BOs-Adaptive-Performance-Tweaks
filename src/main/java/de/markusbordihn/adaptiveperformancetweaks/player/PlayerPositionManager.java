/**
 * Copyright 2021 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.adaptiveperformancetweaks.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import de.markusbordihn.adaptiveperformancetweaks.config.CommonConfig;
import de.markusbordihn.adaptiveperformancetweaks.Manager;

@Mod.EventBusSubscriber
public class PlayerPositionManager extends Manager {

  private static Map<String, PlayerPosition> playerPositionMap = new ConcurrentHashMap<>();
  private static int ticks = 0;

  public static final String LOG_NAME = PlayerPositionManager.class.getSimpleName();
  private static final Logger log = getLogger(LOG_NAME);

  @SubscribeEvent
  public static void onServerAboutToStartEvent(FMLServerAboutToStartEvent event) {
    playerPositionMap = new ConcurrentHashMap<>();
  }

  @SubscribeEvent
  public static void handleServerStartingEvent(FMLServerStartingEvent event) {
    log.info(
        "Player position will be expanded by factor x:{} y:{} z:{} and view area by factor {}.",
        CommonConfig.COMMON.viewAreaXFactor.get(), CommonConfig.COMMON.viewAreaYFactor.get(),
        CommonConfig.COMMON.viewAreaZFactor.get(),
        CommonConfig.COMMON.viewAreaDistanceFactor.get());
  }

  @SubscribeEvent
  public static void handlePlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event) {
    String username = event.getPlayer().getName().getString();
    if (!username.isEmpty()) {
      log.debug("Removing player {} from tracking.", event.getEntity());
      playerPositionMap.remove(username);
    }
  }

  @SubscribeEvent
  public static void handleServerTickEvent(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
      ticks++;
      return;
    }
    if (ticks == 50) {
      for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList()
          .getPlayers()) {
        if (player.isAlive() && !player.hasDisconnected()) {
          updatePlayerPosition(player);
        }
      }
      ticks = 0;
    }
  }

  public static List<PlayerPosition> getPlayerPositionsInsideViewArea(String world, int x, int y,
      int z) {
    List<PlayerPosition> playerPositions = new ArrayList<>();
    for (PlayerPosition playerPosition : playerPositionMap.values()) {
      if (playerPosition.isInsidePlayerViewArea(world, x, y, z)) {
        playerPositions.add(playerPosition);
      }
    }
    return playerPositions;
  }

  public static boolean isInsidePlayersViewArea(String world, int x, int y, int z) {
    for (PlayerPosition playerPosition : playerPositionMap.values()) {
      if (playerPosition.isInsidePlayerViewArea(world, x, y, z)) {
        return true;
      }
    }
    return false;
  }

  public static Map<String, PlayerPosition> getPlayerPositionMap() {
    return playerPositionMap;
  }

  private static void updatePlayerPosition(ServerPlayerEntity player) {
    String username = player.getName().getString();
    playerPositionMap.computeIfAbsent(username, k -> new PlayerPosition(player));
    PlayerPosition playerPosition = playerPositionMap.get(username);
    if (playerPosition.update(player)) {
      log.debug("Update player position for {} with {}", username, playerPosition);
    }
  }
}
