package miniJava;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SyntaxException;

import java.io.FileNotFoundException;

public class Compiler {
	public static void main(String[] args) {
		String filename = args[0];
		SourceFile sourceFile = null;
		try {
			sourceFile = new SourceFile(filename);
		} catch (FileNotFoundException e) {
			System.out.println("There was an error reading in the argument. Make sure to only pass in valid files!");
			System.exit(3);
		}
		Scanner scanner = new Scanner(sourceFile);
		Parser parser = new Parser(scanner);
		try {
			AST ast = parser.parse();
			ASTDisplay astDisplay = new ASTDisplay();
			astDisplay.showTree(ast);
			System.out.println("Parsed successfully!");
			System.exit(0);
		} catch (SyntaxException e) {
			System.out.println(e.getMessage());
			System.exit(4);
		}
	}
}
