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

import me.lucko.spark.monitor.data.MonitoringManager;
import me.lucko.spark.monitor.data.providers.TpsDataProvider;
import me.lucko.spark.sampler.TickCounter;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForgeClientSparkPlatform extends ForgeSparkPlatform {

    public static void register(SparkForgeMod mod) {
        ClientCommandHandler.instance.registerCommand(new ForgeClientSparkPlatform(mod));
    }

    private final TickCounter tickCounter;

    public ForgeClientSparkPlatform(SparkForgeMod mod) {
        super(mod);
        this.tickCounter = new ForgeTickCounter(TickEvent.Type.CLIENT);
        this.tickCounter.start();

        MonitoringManager monitoringManager = getMonitoringManager();
        monitoringManager.addDataProvider("tps", new TpsDataProvider(tickCounter));

        super.scheduler.scheduleWithFixedDelay(monitoringManager, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void broadcast(ITextComponent msg) {
        Minecraft.getMinecraft().player.sendMessage(msg);
    }

    @Override
    public TickCounter getTickCounter() {
        return this.tickCounter;
    }

    @Override
    public String getLabel() {
        return "sparkc";
    }

    @Override
    public String getName() {
        return "sparkc";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("sparkclient");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
