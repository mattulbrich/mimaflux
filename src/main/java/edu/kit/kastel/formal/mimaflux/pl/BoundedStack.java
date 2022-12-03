package edu.kit.kastel.formal.mimaflux.pl;

public class BoundedStack {
    private final int stackBound;
    private int pointer;

    public BoundedStack(int stackBound) {

        this.stackBound = stackBound;
    }

    public String current() {
        return current(0);
    }

    public String inc() {
        pointer ++;
        return current();
    }

    public void dec() {
        pointer --;
        assert pointer >= 0;
    }

    public String current(int offset) {
        if (pointer + offset >= stackBound) {
            throw new RuntimeException("Overflowing the bounded stack. Decompose your expressions!");
        }
        return "X" + (pointer + offset);
    }
}
