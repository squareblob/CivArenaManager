package com.squareblob.cam;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.BlockBasedChunkMeta;

import java.sql.*;

public class CivDatabaseUtility {
    public static void saveStructureReinforcements(ManagedDatasource db, short worldID, BlockVector3 min, BlockVector3 max) throws SQLException {
        try (
                Connection conn = db.getConnection();
                PreparedStatement deleteReinInRegion = conn.prepareStatement(
                        "delete from ctdl_reinforcements_backup where world_id = ? and " +
                                "(chunk_x*16 + cast(x_offset as int)) between ? and ? and " +
                                "(chunk_z*16 + cast(z_offset as int)) between ? and ? and " +
                                "y between ? and ?;");
                PreparedStatement insertReinInRegion = conn.prepareStatement(
                        "insert into ctdl_reinforcements_backup select * from ctdl_reinforcements where world_id = ? and " +
                                "(chunk_x*16 + cast(x_offset as int)) between ? and ? and " +
                                "(chunk_z*16 + cast(z_offset as int)) between ? and ? and " +
                                "y between ? and ?;")) {
            setRegionBoundsStatement(deleteReinInRegion, worldID, min, max);
            deleteReinInRegion.execute();
            setRegionBoundsStatement(insertReinInRegion, worldID, min, max);
            insertReinInRegion.execute();
            CivArenaManager.getInstance().reloadCivStructures();
        }
    }

    public static void saveStructureBastions(ManagedDatasource db, String world_name, BlockVector3 min, BlockVector3 max) throws SQLException {
        try (
                Connection conn = db.getConnection();
                PreparedStatement deleteBastionInRegion = conn.prepareStatement(
                        "delete from bastion.bastion_blocks_backup where loc_world = ? and " +
                                "loc_x between ? and ? and " +
                                "loc_z between ? and ? and " +
                                "loc_y between ? and ?;");
                PreparedStatement insertBastionInRegion = conn.prepareStatement(
                        "insert into bastion.bastion_blocks_backup select * from bastion.bastion_blocks where loc_world = ? and " +
                                "loc_x between ? and ? and " +
                                "loc_z between ? and ? and " +
                                "loc_y between ? and ?;")) {
            setRegionBoundsStatement(deleteBastionInRegion, world_name, min, max);
            deleteBastionInRegion.execute();
            setRegionBoundsStatement(insertBastionInRegion, world_name, min, max);
            insertBastionInRegion.execute();
            CivArenaManager.getInstance().reloadCivStructures();
        }
    }

    public static void saveStructureMetaData(ManagedDatasource db, String structure_name, short worldID, BlockVector3 min, BlockVector3 max) throws SQLException {
        try (
                Connection conn = db.getConnection();
                PreparedStatement saveStructure = conn.prepareStatement(
                        "insert into civ_structures (name, world_id, min_x, min_y, min_z, max_x, max_y, max_z) " +
                                "values (?,?,?,?,?,?,?,?)")) {
            setCivStructuresStatement(saveStructure, structure_name, worldID, min, max);
            saveStructure.execute();
            CivArenaManager.getInstance().reloadCivStructures();
        }
    }

    public static int loadStructureReinforcements(ManagedDatasource db, short origin_worldID, BlockVector3 min, BlockVector3 max, short dest_worldID, BlockVector3 dest_min) throws SQLException {
        try (
                Connection conn = db.getConnection();
                PreparedStatement deleteReinInRegion = conn.prepareStatement(
                        "delete from ctdl_reinforcements where world_id = ? and " +
                                "(chunk_x*16 + cast(x_offset as int)) between ? and ? and " +
                                "(chunk_z*16 + cast(z_offset as int)) between ? and ? and " +
                                "y between ? and ?");
                PreparedStatement extractStructure = conn.prepareStatement("select * from " +
                        "ctdl_reinforcements_backup where world_id = ? and (chunk_x*16 + cast(x_offset as int)) between ? and ? and " +
                        "(chunk_z*16 + cast(z_offset as int)) between ? and ? and y between ? and ?"
                );
                PreparedStatement insertRein = conn.prepareStatement("insert into ctdl_reinforcements (chunk_x, " +
                        "chunk_z, world_id, x_offset, y, z_offset, type_id, health, group_id, insecure, creation_time) values(?,?,?, ?,?,?, ?,?,?,?,?);")) {
            BlockVector3 transformation = dest_min.subtract(min);
            setRegionBoundsStatement(deleteReinInRegion, origin_worldID, min.add(transformation), max.add(transformation));
            deleteReinInRegion.execute();
            setRegionBoundsStatement(extractStructure, origin_worldID, min, max);
            ResultSet rs = extractStructure.executeQuery();
            int i = 0;
            while (rs.next()) {
                int chunk_x = rs.getInt(1);
                int chunk_z = rs.getInt(2);
                byte x_offset = rs.getByte(4);
                short y = rs.getShort(5);
                byte z_offset = rs.getByte(6);
                short type = rs.getShort(7);
                float health = rs.getFloat(8);
                int group_id = rs.getInt(9);
                boolean insecure = rs.getBoolean(10);
                Timestamp creation_time = rs.getTimestamp(11);
                insertRein.setInt(1, BlockBasedChunkMeta.toChunkCoord((chunk_x * 16 + x_offset + transformation.getX())));
                insertRein.setInt(2, BlockBasedChunkMeta.toChunkCoord((chunk_z * 16 + z_offset + transformation.getZ())));
                insertRein.setShort(3, dest_worldID);
                insertRein.setByte(4, (byte) BlockBasedChunkMeta.modulo(chunk_x * 16 + x_offset + transformation.getX()));
                insertRein.setShort(5, (short) ((y) + transformation.getY()));
                insertRein.setByte(6, (byte) BlockBasedChunkMeta.modulo(chunk_z * 16 + z_offset + transformation.getZ()));
                insertRein.setShort(7, type);
                insertRein.setFloat(8, health);
                insertRein.setInt(9, group_id);
                insertRein.setBoolean(10, insecure);
                insertRein.setTimestamp(11, creation_time);
                // Not batched because I ran into problems. Fix later
                insertRein.execute();
                i++;
            }
            return i;
        }
    }

    public static int loadStructureBastions(ManagedDatasource db, String origin_world_name, BlockVector3 min, BlockVector3 max, String dest_worldName, BlockVector3 dest_min) throws SQLException {
        try (
                Connection conn = db.getConnection();
                PreparedStatement deleteBastionInRegion = conn.prepareStatement(
                        "delete from bastion.bastion_blocks where loc_world = ? and " +
                                "loc_x between ? and ? and loc_z between ? and ? and loc_y between ? and ?;");
                PreparedStatement extractStructure = conn.prepareStatement("select * from " +
                        "bastion.bastion_blocks_backup where loc_world = ? and loc_x between ? and ? and " +
                        "loc_z between ? and ? and loc_y between ? and ?;");
                PreparedStatement insertBastion = conn.prepareStatement("insert into bastion.bastion_blocks (bastion_type, " +
                        "loc_x, loc_y, loc_z, loc_world, placed, fraction, dead) values(?,?,?,?,?,?,?,?);")) {
            BlockVector3 transformation = dest_min.subtract(min);
            setRegionBoundsStatement(deleteBastionInRegion, origin_world_name, min.add(transformation), max.add(transformation));
            deleteBastionInRegion.execute();
            setRegionBoundsStatement(extractStructure, origin_world_name, min, max);
            ResultSet rs = extractStructure.executeQuery();
            int i = 0;
            while (rs.next()) {
                String bastion_type = rs.getString(2);
                int loc_x = rs.getInt(3);
                int loc_y = rs.getInt(4);
                int loc_z = rs.getInt(5);
                String loc_world = rs.getString(6);
                long placed = rs.getLong(7);
                float fraction = rs.getFloat(8);
                boolean dead = rs.getBoolean(9);
                insertBastion.setString(1, bastion_type);
                insertBastion.setInt(2, loc_x + transformation.getX());
                insertBastion.setInt(3, loc_y + transformation.getY());
                insertBastion.setInt(4, loc_z + transformation.getZ());
                insertBastion.setString(5, loc_world);
                insertBastion.setLong(6, placed);
                insertBastion.setFloat(7, fraction);
                insertBastion.setBoolean(8, dead);
                // Not batched because I ran into problems. Fix later
                insertBastion.execute();
                i++;
            }
            return i;
        }
    }

    public static void cmcFlush(JavaPlugin plugin) {
        short pluginID = CivModCorePlugin.getInstance().getChunkMetaManager().getChunkDAO()
                .getOrCreatePluginID(plugin);
        CivModCorePlugin.getInstance().getChunkMetaManager().flushPlugin(pluginID);
    }

    private static void setRegionBoundsStatement(PreparedStatement statement, String world_name, BlockVector3 min, BlockVector3 max) throws SQLException {
        statement.setString(1, world_name);
        setRegionBoundsStatement(statement, min, max);
    }

    private static void setRegionBoundsStatement(PreparedStatement statement, short worldID, BlockVector3 min, BlockVector3 max) throws SQLException {
        statement.setShort(1, worldID);
        setRegionBoundsStatement(statement, min, max);
    }

    private static void setRegionBoundsStatement(PreparedStatement statement, BlockVector3 min, BlockVector3 max) throws SQLException {
        statement.setInt(2, min.getX());
        statement.setInt(3, max.getX());
        statement.setInt(4, min.getZ());
        statement.setInt(5, max.getZ());
        statement.setInt(6, min.getY());
        statement.setInt(7, max.getY());
    }

    private static void setCivStructuresStatement(PreparedStatement statement, String structure_name, short worldID, BlockVector3 min, BlockVector3 max) throws SQLException {
        statement.setString(1, structure_name);
        statement.setShort(2, worldID);
        statement.setInt(3, min.getX());
        statement.setInt(4, min.getY());
        statement.setInt(5, min.getZ());
        statement.setInt(6, max.getX());
        statement.setInt(7, max.getY());
        statement.setInt(8, max.getZ());
    }
}
