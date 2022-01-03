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

package de.markusbordihn.adaptiveperformancetweaksplayer.player;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;

import de.markusbordihn.adaptiveperformancetweakscore.server.ServerLoadEvent;
import de.markusbordihn.adaptiveperformancetweaksplayer.Constants;
import de.markusbordihn.adaptiveperformancetweaksplayer.config.CommonConfig;

@EventBusSubscriber
public class PlayerViewDistance {

  private static final CommonConfig.Config COMMON = CommonConfig.COMMON;
  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static boolean optimizeViewDistance = COMMON.optimizeViewDistance.get();
  private static int viewDistanceDefault = COMMON.viewDistanceDefault.get();
  private static int viewDistanceMax = COMMON.viewDistanceMax.get();
  private static int viewDistanceMin = COMMON.viewDistanceMin.get();

  protected PlayerViewDistance() {}

  @SubscribeEvent
  public static void onServerAboutToStartEvent(ServerAboutToStartEvent event) {
    optimizeViewDistance = COMMON.optimizeViewDistance.get();
    viewDistanceDefault = COMMON.viewDistanceDefault.get();
    viewDistanceMin = COMMON.viewDistanceMin.get();
    viewDistanceMax = COMMON.viewDistanceMax.get();
  }

  @SubscribeEvent
  public static void onServerStarting(ServerStartingEvent event) {
    if (!optimizeViewDistance) {
      return;
    }
    log.info("View distance will be optimized between {} and {} with {} as default",
        viewDistanceMin, viewDistanceMax, viewDistanceDefault);
    setViewDistance(viewDistanceMin);
  }

  @SubscribeEvent
  public static void handleServerLoadEvent(ServerLoadEvent event) {
    if (!optimizeViewDistance) {
      return;
    }
    int numOfPlayers = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount();
    if (event.hasHighServerLoad()) {
      decreaseViewDistance();
    } else if (event.hasNormalServerLoad() && numOfPlayers >= 1) {
      decreaseViewDistance();
    } else if (event.hasNormalServerLoad() && numOfPlayers == 1) {
      increaseViewDistance();
    } else if (event.hasLowServerLoad() && numOfPlayers >= 1) {
      increaseViewDistance();
    }
  }

  public static void setViewDistance(int viewDistance) {
    if (viewDistance > viewDistanceMax) {
      viewDistance = viewDistanceMax;
    } else if (viewDistance < viewDistanceMin) {
      viewDistance = viewDistanceMin;
    }
    MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
    PlayerList playerList = minecraftServer.getPlayerList();
    if (!playerList.getPlayers().isEmpty() && playerList.getViewDistance() != viewDistance) {
      log.debug("Changing server view distance from {} to {} for {} players.",
          playerList.getViewDistance(), viewDistance, playerList.getPlayers().size());
      playerList.setViewDistance(viewDistance);
    }
  }

  public static void increaseViewDistance() {
    setViewDistance(getViewDistance() + 1);
  }

  public static void decreaseViewDistance() {
    setViewDistance(getViewDistance() - 1);
  }

  public static int getViewDistance() {
    final MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
    PlayerList playerList = minecraftServer.getPlayerList();
    return playerList.getViewDistance() > 0 ? playerList.getViewDistance() : viewDistanceDefault;
  }

  public static int getMaxViewDistance() {
    return viewDistanceMax;
  }

  public static int getMinViewDistance() {
    return viewDistanceMin;
  }
}