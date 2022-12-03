package edu.kit.kastel.formal.mimaflux.pl;

import edu.kit.kastel.formal.mimaflux.BailOutErrorStrategy;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.FileContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.List;

public class MimaCompiler {

    public static void main(String[] args) throws IOException {
        parse(args[0]).forEach(System.out::println);
    }

    public static List<String> parse(String fileName) throws IOException {
        CharStream input = CharStreams.fromFileName(fileName);
        MimaWhileLexer lexer = new MimaWhileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MimaWhileParser parser = new MimaWhileParser(tokens);
        BailOutErrorStrategy errorStrategy = new BailOutErrorStrategy();
        parser.setErrorHandler(errorStrategy);
        FileContext content = parser.file();
        CompilerVisitor pv = new CompilerVisitor();
        content.accept(pv);
        return pv.getCompilation();
    }
}
