package edu.kit.kastel.formal.mimaflux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Timeline {

    private final List<Update[]> updates = new ArrayList<>();

    private List<Update> curUpdates = new ArrayList<>();

    private final String fileContent;
    private final List<Command> commands;
    private final State state;

    private int currentPosition = 0;
    private List<UpdateListener> listeners = new ArrayList<>();

    public Timeline(String fileContent, List<Command> commands, State state) {
        this.fileContent = fileContent;
        this.commands = commands;
        this.state = state;
    }

    public void set(int addr, int val) {
        int curVal = state.get(addr);
        curUpdates.add(new Update(addr, curVal, val));
        state.set(addr, val);

        for (UpdateListener listener : listeners) {
            listener.memoryChanged(addr, val);
        }
    }

    public void incIAR() {
        set(State.IAR, (state.get(State.IAR) + 1) & Constants.ADDRESS_MASK);
    }

    public void commit() {
        updates.add(curUpdates.toArray(Update[]::new));
        curUpdates.clear();
        currentPosition++;
    }

    public State exposeState() {
        return state;
    }

    public void addListener(UpdateListener listener) {
        listeners.add(listener);
    }

    public void setPosition(int position) {

        position = Math.min(updates.size(), position);

        if(currentPosition < position) {
            while(currentPosition < position) {
                incrementPosition();
            }
        } else {
            while (currentPosition > position) {
                decrementPosition();
            }
        }
    }

    private void decrementPosition() {
        currentPosition--;
        for (Update update : updates.get(currentPosition)) {
            set(update.addr(), update.oldValue());
        }
    }

    private void incrementPosition() {
        for (Update update : updates.get(currentPosition)) {
            set(update.addr(), update.newValue());
        }
        currentPosition ++;
    }

    public int getPosition() {
        return currentPosition;
    }

    public int size() {
        return updates.size();
    }
    public String getFileContent() {
        return fileContent;
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

}
