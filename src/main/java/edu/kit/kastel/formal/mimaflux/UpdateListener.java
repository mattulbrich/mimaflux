package edu.kit.kastel.formal.mimaflux;

public interface UpdateListener {
    public void memoryChanged(int addr, int val);
}
