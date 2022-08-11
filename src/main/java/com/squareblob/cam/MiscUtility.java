package com.squareblob.cam;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;

// todo : this is a temporary class
public class MiscUtility {
    // temporary, inefficient WorldEdit pasting
    public static void pasteStructure(short origin_worldID, BlockVector3 min, BlockVector3 max, CuboidRegion dest_selection) {
        //copy
        World world = CivModCorePlugin.getInstance().getWorldIdManager().getWorldByInternalID(origin_worldID);
        CuboidRegion origin_selection = new CuboidRegion(BukkitAdapter.adapt(world), min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(origin_selection);
        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(origin_selection.getWorld(), -1);
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, origin_selection, clipboard, origin_selection.getMinimumPoint());
        forwardExtentCopy.setCopyingEntities(true);
        try {
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
        editSession.flushSession();

        //paste
        EditSession editSession2 = WorldEdit.getInstance().getEditSessionFactory().getEditSession(dest_selection.getWorld(), -1);
        BlockVector3 min_dest = dest_selection.getMinimumPoint();
        Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession2)
                .to(BlockVector3.at(min_dest.getX(), min_dest.getY(), min_dest.getZ()))
                .ignoreAirBlocks(false)
                .build();
        try {
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
        editSession2.flushSession();
    }

    public static short getWorldID(World world) {
        return CivModCorePlugin.getInstance().getWorldIdManager().getInternalWorldId(world);
    }

    public static CuboidRegion getCuboidRegion(Player player) throws IncompleteRegionException {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        try {
            return (CuboidRegion) session.getSelection(session.getSelectionWorld());
        } catch (IncompleteRegionException | ClassCastException e) {
            player.sendMessage(ChatColor.RED + "You must select a valid cuboid region");
            throw e;
        }
    }
}
