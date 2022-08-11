package com.squareblob.cam.models;

import com.sk89q.worldedit.math.BlockVector3;

public class CivStructure {
    private final long uuid;
    private String name;
    private short worldID;
    private BlockVector3 min_bound;
    private BlockVector3 max_bound;

    public CivStructure(long uuid, String name, short worldID, BlockVector3 min_bound, BlockVector3 max_bound) {
        this.uuid = uuid;
        this.name = name;
        this.worldID = worldID;
        this.min_bound = min_bound;
        this.max_bound = max_bound;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getWorldID() {
        return worldID;
    }

    public void setWorldID(short worldID) {
        this.worldID = worldID;
    }

    public BlockVector3 getMin_bound() {
        return min_bound;
    }

    public void setMin_bound(BlockVector3 min_bound) {
        this.min_bound = min_bound;
    }

    public BlockVector3 getMax_bound() {
        return max_bound;
    }

    public void setMax_bound(BlockVector3 max_bound) {
        this.max_bound = max_bound;
    }

    public long getUuid() {
        return uuid;
    }
}
