/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.forge;

import com.google.gson.JsonPrimitive;

import me.lucko.spark.monitor.data.DataProvider;
import me.lucko.spark.monitor.data.MonitoringManager;
import me.lucko.spark.monitor.data.providers.TpsDataProvider;
import me.lucko.spark.sampler.TickCounter;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForgeServerSparkPlatform extends ForgeSparkPlatform {

    private final TickCounter tickCounter;

    public ForgeServerSparkPlatform(SparkForgeMod mod) {
        super(mod);
        this.tickCounter = new ForgeTickCounter(TickEvent.Type.SERVER);
        this.tickCounter.start();

        MonitoringManager monitoringManager = getMonitoringManager();
        monitoringManager.addDataProvider("tps", new TpsDataProvider(this.tickCounter));
        monitoringManager.addDataProvider("players", DataProvider.syncProvider(() -> {
            return new JsonPrimitive(FMLCommonHandler.instance().getMinecraftServerInstance().getCurrentPlayerCount());
        }));

        super.scheduler.scheduleWithFixedDelay(monitoringManager, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void broadcast(ITextComponent msg) {
        FMLCommonHandler.instance().getMinecraftServerInstance().sendMessage(msg);

        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for (EntityPlayerMP player : players) {
            if (player.canUseCommand(4, "spark")) {
                player.sendMessage(msg);
            }
        }
    }

    @Override
    public TickCounter getTickCounter() {
        return this.tickCounter;
    }

    @Override
    public String getLabel() {
        return "spark";
    }

    @Override
    public String getName() {
        return "spark";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "spark");
    }
}
