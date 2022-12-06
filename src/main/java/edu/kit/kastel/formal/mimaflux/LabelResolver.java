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

import edu.kit.kastel.formal.mimaflux.MimaAsmParser.CommandContext;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class LabelResolver {
    private Map<String, Integer> labelMap;

    public void resolve(List<Command> commands) {
        labelMap = new HashMap<String, Integer>();
        ListIterator<Command> it = commands.listIterator();
        while (it.hasNext()) {
            Command command = it.next();
            if (command.label() != null) {
                if (labelMap.containsKey(command.label())) {
                    CommandContext ctx = command.ctx();
                    throw new TokenedException(ctx != null ? ctx.getStart() : null, "Symbol '" + command.label() + "' already defined");
                }
                labelMap.put(command.label(), command.address());
            }
            if (command.instruction() == null) {
                // This was a label definition only.
                it.remove();
            }
        }

        it = commands.listIterator();
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
