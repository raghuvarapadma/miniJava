package miniJava;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.ContextualAnalysisException;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;
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
			Package ast = parser.parse();
			new ASTDisplay().showTree(ast);
			try {
				Identification identification = new Identification(ast);
				TypeChecking typechecking = new TypeChecking(ast);
				if (TypeChecking.throwError) {
					System.exit(4);
				} else {
					System.exit(0);
				}
			} catch (ContextualAnalysisException e) {
				System.exit(4);;
			}
		} catch (SyntaxException e) {
			System.out.println(e.getMessage());
			System.exit(4);
		}
	}
}