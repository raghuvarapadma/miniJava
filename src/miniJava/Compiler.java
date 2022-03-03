package miniJava;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SyntaxException;

import java.io.*;

public class Compiler {
	public static <InputReader> void main(String[] args) throws SyntaxException, FileNotFoundException {
		File folder = new File("/users/raghuvarapadma/Desktop/Tests/pa1_tests");
		try {
			new PrintStream(new BufferedOutputStream(new FileOutputStream("/users/raghuvarapadma/Desktop/text.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File[] listOfFiles = folder.listFiles();
		int numFiles = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().indexOf("pass") == 0) {

					System.out.println("(" + (++numFiles) + ") + Testing: " + listOfFiles[i].getName());

					(new ASTDisplay()).showTree(new Parser(new Scanner(new SourceFile(listOfFiles[i].getAbsolutePath()))).parse());
//					(new Parser(new Scanner(new SourceFile(listOfFiles[i].getAbsolutePath())))).parse();
					System.out.println("\tSuccess");
				}
			}
		}
	}
}