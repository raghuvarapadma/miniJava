package miniJava;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.CodeGenerator;
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
//			new ASTDisplay().showTree(ast);
			try {
				Identification identification = new Identification(ast);
				TypeChecking typechecking = new TypeChecking(ast);
				if (TypeChecking.throwError) {
					System.exit(4);
				}
				CodeGenerator codeGenerator = new CodeGenerator(ast);
				String objectCodeFileName = filename.replace(".java", ".mJAM");
				ObjectFile objF = new ObjectFile(objectCodeFileName);
				System.out.print("Writing object code file " + objectCodeFileName + " ... ");
				if (objF.write()) {
					System.out.println("FAILED!");
					System.exit(4);;
				}
				else {
					System.out.println("SUCCEEDED");
				}
//				Interpreter.interpret(objectCodeFileName);
				String asmCodeFileName = objectCodeFileName.replace(".mJAM",".asm");
				System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
				Disassembler d = new Disassembler(objectCodeFileName);
				if (d.disassemble()) {
					System.out.println("FAILED!");
					System.exit(4);;
				}
				else {
					System.out.println("SUCCEEDED");
				}
				System.out.println("Running code in debugger ... ");
				Interpreter.debug(objectCodeFileName, asmCodeFileName);

				System.out.println("*** mJAM execution completed");
			} catch (ContextualAnalysisException e) {
				System.exit(4);;
			}
		} catch (SyntaxException e) {
			System.out.println(e.getMessage());
			System.exit(4);
		}
	}
}