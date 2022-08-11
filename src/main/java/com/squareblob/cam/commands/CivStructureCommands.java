package com.squareblob.cam.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.squareblob.cam.CivArenaManager;
import com.squareblob.cam.CivStructureManager;
import com.squareblob.cam.models.CivStructure;
import isaac.bastion.Bastion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

import java.sql.SQLException;
import java.util.stream.Collectors;

import static com.squareblob.cam.CivDatabaseUtility.*;
import static com.squareblob.cam.MiscUtility.*;

public class CivStructureCommands extends BaseCommand {

    @CommandAlias("savecivstructure|savecs")
    @Syntax("<name>")
    @Description("Saves structure in current WorldEdit selection")
    @CommandPermission("cam.admin")
    public void saveCivStructure(Player player, String structure_name) {
        CivStructureManager structureManager = CivArenaManager.getInstance().getStructureManager();
        if (structureManager.getStructures().stream().map(CivStructure::getName).collect(Collectors.toList())
                .contains(structure_name)) {
            player.sendMessage(ChatColor.RED + "Failed, there already exists a structure with this name");
            return;
        }
        CuboidRegion selection;
        try {
            selection = getCuboidRegion(player);
        } catch (IncompleteRegionException e) {
            return;
        }
        cmcFlush(Citadel.getInstance());
        cmcFlush(Bastion.getPlugin());
        short worldID = getWorldID(player.getWorld());
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();
        ManagedDatasource db = CivArenaManager.getInstance().getCitadelDB();
        try {
            saveStructureReinforcements(db, worldID, min, max);
            saveStructureBastions(db, player.getWorld().getName(), min, max);
            saveStructureMetaData(db, structure_name, worldID, min, max);
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Failed to save structure reinforcements and bastions. See console for errors");
        }
        player.sendMessage(ChatColor.GREEN + "Structure saved successfully");
    }

    @CommandAlias("loadcivstructure|loadcs")
    @Syntax("<name>")
    @CommandCompletion("@structure")
    @Description("Loads reinforcements from saved structure at minimum point of current selection")
    @CommandPermission("cam.admin")
    public void loadCivStructure(Player player, String structure_name) {
        CuboidRegion selection;
        try {
            selection = getCuboidRegion(player);
        } catch (IncompleteRegionException e) {
            return;
        }
        CivStructureManager csm = CivArenaManager.getInstance().getStructureManager();
        CivStructure structure = csm.getStructures().stream().filter(civStructure -> civStructure.getName()
                .equals(structure_name)).findFirst().orElse(null);
        if (structure == null) {
            player.sendMessage(ChatColor.RED + "No structure found with matching name.");
            return;
        }
        pasteStructure(structure.getWorldID(), structure.getMin_bound(), structure.getMax_bound(), selection);

        short dest_worldID = getWorldID(player.getWorld());
        String origin_worldName = CivModCorePlugin.getInstance().getWorldIdManager().getWorldByInternalID(dest_worldID).getName();
        ManagedDatasource db = CivArenaManager.getInstance().getCitadelDB();
        BlockVector3 dest_min = selection.getMinimumPoint();
        try {
            int bastionsChanged = loadStructureBastions(db, origin_worldName, structure.getMin_bound(),
                    structure.getMax_bound(), player.getWorld().getName(), dest_min);
            int reinChanged = loadStructureReinforcements(db, structure.getWorldID(), structure.getMin_bound(),
                    structure.getMax_bound(), dest_worldID, dest_min);
            player.sendMessage(ChatColor.GREEN + "Loaded all bastions (" + bastionsChanged + ")");
            player.sendMessage(ChatColor.GREEN + "Loaded all reinforcements (" + reinChanged + ")");
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Failed to load structure");
        }
    }
}