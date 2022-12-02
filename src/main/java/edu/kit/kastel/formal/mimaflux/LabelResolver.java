package edu.kit.kastel.formal.mimaflux;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class LabelResolver {
    private Map<String, Integer> labelMap;

    public void resolve(List<Command> commands) {
        labelMap = new HashMap<String, Integer>();
        for (Command command : commands) {
            if (command.label() != null) {
                if (labelMap.containsKey(command.label())) {
                    throw new TokenedException(command.ctx().getStart(), "Symbol '" + command.label() + "' already defined");
                }
                labelMap.put(command.label(), command.address());
            }
        }

        ListIterator<Command> it = commands.listIterator();
        while (it.hasNext()) {
            Command command = it.next();
            if(command.labelArg() != null) {
                Integer val = labelMap.get(command.labelArg());
                if (val == null) {
                    throw new TokenedException(command.ctx().getStart(), "Unknown label " + command.labelArg());
                }
                Command newCommand = command.updateArg(val);
                it.set(newCommand);
            }
        }
    }

    public Map<String, Integer> getLabelMap() {
        return labelMap;
    }
}
