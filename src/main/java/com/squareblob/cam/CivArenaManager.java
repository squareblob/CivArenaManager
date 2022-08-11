package com.squareblob.cam;

import com.sk89q.worldedit.math.BlockVector3;
import com.squareblob.cam.commands.CAMCommandManager;
import com.squareblob.cam.models.CivStructure;
import org.bukkit.Bukkit;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CivArenaManager extends ACivMod {
    private static CivArenaManager instance;
    private CAMCommandManager commandManager;
    private CivStructureManager structureManager;

    public CivStructureManager getStructureManager() {
        return structureManager;
    }

    public static CivArenaManager getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        commandManager = new CAMCommandManager(this);
        structureManager = new CivStructureManager();

        // todo : use own ManagedDatasource, and registerMigrations
        ManagedDatasource db = getCitadelDB();
        try (Connection conn = db.getConnection();
             PreparedStatement create_table_structures = conn.prepareStatement(
                     "create table if not exists civ_structures(" +
                             "uuid bigint unsigned default(uuid_short()) primary key," +
                             "name varchar(32) not null," +
                             "world_id smallint unsigned not null," +
                             "min_x int not null," +
                             "min_y smallint not null," +
                             "min_z int not null," +
                             "max_x int not null," +
                             "max_y smallint not null," +
                             "max_z int not null);");
             PreparedStatement create_table_rein_backup = conn.prepareStatement(
                     "create table if not exists ctdl_reinforcements_backup like ctdl_reinforcements"
             );
             PreparedStatement create_table_bastion_backup = conn.prepareStatement(
                     "create table if not exists bastion.bastion_blocks_backup like bastion.bastion_blocks"
             )) {
            create_table_structures.execute();
            create_table_rein_backup.execute();
            create_table_bastion_backup.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Errors setting up database, shutting down");
            Bukkit.shutdown();
        }
        reloadCivStructures();
    }

    public void reloadCivStructures() {
        structureManager.getStructures().clear();
        ManagedDatasource db = getCitadelDB();
        try (Connection conn = db.getConnection();
             PreparedStatement loadStructures = conn.prepareStatement(
                     "select * from civ_structures")) {
            ResultSet rs = loadStructures.executeQuery();
            while (rs.next()) {
                structureManager.getStructures().add(
                        new CivStructure(rs.getLong(1), rs.getString(2), rs.getShort(3),
                                BlockVector3.at(rs.getInt(4), rs.getInt(5), rs.getInt(6)),
                                BlockVector3.at(rs.getInt(7), rs.getInt(8), rs.getInt(9)))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Error getting civ structures, shutting down");
            Bukkit.shutdown();
        }
    }

    public ManagedDatasource getCitadelDB() {
        return Citadel.getInstance().getConfigManager().getDatabase();
    }
}