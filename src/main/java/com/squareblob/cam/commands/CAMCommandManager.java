package com.squareblob.cam.commands;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import com.squareblob.cam.CivArenaManager;
import com.squareblob.cam.models.CivStructure;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.commands.CommandManager;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

public class CAMCommandManager extends CommandManager {

    public CAMCommandManager(Plugin plugin) {
        super(plugin);
        init();
    }

    @Override
    public void registerCommands() {
        registerCommand(new CivStructureCommands());
    }

    @Override
    public void registerCompletions(@Nonnull CommandCompletions<BukkitCommandCompletionContext> completions) {
        super.registerCompletions(completions);
        completions.registerCompletion("structure", context -> CivArenaManager.getInstance().getStructureManager()
                .getStructures().stream().map(CivStructure::getName).collect(Collectors.toList()));
    }
}