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

import edu.kit.kastel.formal.mimaflux.MimaFluxArgs.Range;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class State {

    public static final int IAR = -1;
    public static final int ACCU = -2;

    private static Map<String, Integer> OPCODES = Map. ofEntries(
            Map.entry("LDC", 0x000000),
            Map.entry("DS",  0x0),
            Map.entry("LDV", 0x100000),
            Map.entry("STV", 0x200000),
            Map.entry("ADD", 0x300000),
            Map.entry("AND", 0x400000),
            Map.entry("OR",  0x500000),
            Map.entry("XOR", 0x600000),
            Map.entry("EQL", 0x700000),
            Map.entry("JMP", 0x800000),
            Map.entry("JMN", 0x900000),
            Map.entry("LDIV",0xa00000),
            Map.entry("STIV",0xb00000),
            Map.entry("JMS", 0xc00000),
            Map.entry("JIND",0xd00000),
            Map.entry("HALT",0xf00000),
            Map.entry("NOT", 0xf10000),
            Map.entry("RAR", 0xf20000)
    );

    private static Map<Integer, String> INV_OPCODES = Map. ofEntries(
            Map.entry(0x000000, "LDC"),
            Map.entry(0x100000, "LDV"),
            Map.entry(0x200000, "STV"),
            Map.entry(0x300000, "ADD"),
            Map.entry(0x400000, "AND"),
            Map.entry(0x500000, "OR"),
            Map.entry(0x600000, "XOR"),
            Map.entry(0x700000, "EQL"),
            Map.entry(0x800000, "JMP"),
            Map.entry(0x900000, "JMN"),
            Map.entry(0xa00000, "LDIV"),
            Map.entry(0xb00000, "STIV"),
            Map.entry(0xc00000, "JMS"),
            Map.entry(0xd00000, "JIND"),
            Map.entry(0xf00000, "HALT"),
            Map.entry(0xf10000, "NOT"),
            Map.entry(0xf20000, "RAR")
    );

    private int[] mem = new int[Constants.ADDRESS_RANGE];

    private int iar;
    private int accu;

    public State(List<Command> commands) {
        populateMemoryFromProgram(commands);
    }

    private void populateMemoryFromProgram(List<Command> commands) {
        for (Command command : commands) {
            int adr = command.address();
            int opcode = OPCODES.getOrDefault(command.instruction(), -1);
            int arg = command.valueArg();
            mem[adr] = opcode;
            if((opcode & 0xf0_0000) != 0xf0_0000) {
                mem[adr] |= arg;
            }
        }
    }

    public void set(int addr,  int value) {
        switch(addr) {
            case IAR: iar = value; break;
            case ACCU: accu = value; break;
            default: mem[addr] = value;
        }
    }

    public int get(int addr) {
        switch (addr) {
            case IAR: return iar;
            case ACCU: return accu;
            default: return mem[addr];
        }
    }

    public void printToConsole(Map<String, Integer> labelMap) {
        System.out.printf("        IAR  = 0x%06x = %8d\t\t(instruction there: %s)%n", iar, iar, toInstruction(mem[iar]));
        System.out.printf("        ACCU = 0x%06x = %8d%n", accu, accu);
        for (Entry<String, Integer> entry : labelMap.entrySet()) {
            int val = entry.getValue();
            System.out.printf("Label '%s' at mem[0x%05x]  =  0x%06x = %8d = %s%n", entry.getKey(), val, mem[val], mem[val], toInstruction(mem[val]));
        }
        if (MimaFlux.mmargs.printRanges != null) {
            for (Range range : MimaFlux.mmargs.printRanges) {
                for (int i = range.from(); i <= range.to(); i++) {
                    System.out.printf("mem[0x%05x] = 0x%06x = %8d = %s%n", i, mem[i], mem[i], toInstruction(mem[i]));
                }
            }
        }
    }

    public static String toInstruction(int instruction) {
        if(instruction >> 20 == 0xf) {
            return INV_OPCODES.getOrDefault(instruction, Constants.UNKNOWN_OPCODE);
        } else {
            String opcode = INV_OPCODES.getOrDefault(instruction & 0xf00000, Constants.UNKNOWN_OPCODE);
            return opcode + " " + (instruction & Constants.ADDRESS_MASK);
        }
    }
}
