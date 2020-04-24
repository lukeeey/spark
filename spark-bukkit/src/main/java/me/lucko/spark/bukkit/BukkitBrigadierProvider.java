package me.lucko.spark.bukkit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.commodore.Commodore;
import me.lucko.spark.common.command.Command;
import org.bukkit.command.PluginCommand;

import java.util.List;

public enum BukkitBrigadierProvider {
    ;

    public static void register(Commodore commodore, PluginCommand bukkitCommand, List<Command> sparkCommands) {
        LiteralCommandNode<Object> root = LiteralArgumentBuilder.literal("spark").executes(o -> 0).build();

        for (Command sparkCommand : sparkCommands) {
            for (String alias : sparkCommand.aliases()) {
                LiteralCommandNode<Object> node = LiteralArgumentBuilder.literal(alias).executes(o -> 0).build();

                for (Command.ArgumentInfo argument : sparkCommand.arguments()) {
                    if (argument.requiresParameter()) {
                        node.addChild(LiteralArgumentBuilder.literal("--" + argument.argumentName())
                                .then(RequiredArgumentBuilder.argument(argument.parameterDescription(), StringArgumentType.string())
                                        .redirect(node)
                                )
                                .build()
                        );
                    } else {
                        node.addChild(LiteralArgumentBuilder.literal("--" + argument.argumentName())
                                .redirect(node)
                                .build()
                        );
                    }
                }

                root.addChild(node);
            }
        }

        commodore.register(bukkitCommand, root, player -> player.hasPermission("spark"));
    }


}
