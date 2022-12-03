/*
 * This file is part of the tool MimaFlux.
 * https://github.com/mattulbrich/mimaflux
 *
 * MimaFlux is a time travel debugger for the Minimal Machine
 * used in Informatics teaching at a number of schools.
 *
 * The system is protected by the GNU General Public License Version 3.
 * See the file LICENSE in the main directory of the project.
 *
 * (c) 2016-2022 Karlsruhe Institute of Technology
 *
 * Adapted for Mima by Mattias Ulbrich
 */
package edu.kit.kastel.formal.mimaflux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimelineBuilder {

    private final List<Update[]> updates = new ArrayList<>();

    private List<Update> curUpdates = new ArrayList<>();

    private final String fileContent;
    private final Map<String, Integer> labelMap;
    private final List<Command> commands;
    private final State state;

    public TimelineBuilder(String fileContent, Map<String, Integer> labelMap, List<Command> commands) {
        this.fileContent = fileContent;
        this.labelMap = labelMap;
        this.commands = commands;
        this.state = new State(commands);
        int start = labelMap.getOrDefault(Constants.START_LABEL, 0);
        state.set(State.IAR, start);
    }

    public void set(int addr, int val) {
        int curVal = state.get(addr);
        curUpdates.add(new Update(addr, curVal, val));
        state.set(addr, val);
    }

    public void commit() {
        updates.add(curUpdates.toArray(Update[]::new));
        curUpdates.clear();
    }

    public void incIAR() {
        set(State.IAR, (state.get(State.IAR) + 1) & Constants.ADDRESS_MASK);
    }

    public int size() {
        return updates.size();
    }

    public State exposeState() {
        return state;
    }

    public Timeline build() {
        Update[][] array = updates.toArray(Update[][]::new);
        return new Timeline(array, fileContent, labelMap, commands);
    }
}
