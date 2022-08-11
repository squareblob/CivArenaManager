package com.squareblob.cam;

import com.squareblob.cam.models.CivStructure;

import java.util.HashSet;
import java.util.Set;

public class CivStructureManager {
    private Set<CivStructure> structures;

    public CivStructureManager() {
        this.structures = new HashSet<>();
    }

    public Set<CivStructure> getStructures() {
        return structures;
    }
}
