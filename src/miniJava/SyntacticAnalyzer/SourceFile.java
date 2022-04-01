package miniJava.SyntacticAnalyzer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SourceFile {
	public static final char EOT = '\u0000';
	public static final char EOL = '\n';

	private final FileInputStream source;
	private int lineCounter;

	public SourceFile(String filename) throws FileNotFoundException {
		this.source = new FileInputStream(filename);
		lineCounter = 1;
	}

	public char getChar() {
		try {
			int char_int = source.read();

			if (char_int == -1) {
				char_int = EOT;
			} else if (char_int == EOL) {
				lineCounter++;
			}

			return (char) char_int;

		} catch (IOException error) {
			System.out.println("ERROR: " + error);
			System.out.println("There was an issue retrieving the next character!");
			return EOT;
		}
	}

	public int getLineCounter() {
		return lineCounter;
	}
}

