package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SourceFile {
	public static final char EOT = '\u0000';

	private final FileInputStream source;

	public SourceFile(String filename) throws FileNotFoundException {
		this.source = new FileInputStream(filename);
	}

	public char getChar() {
		try {
			int char_int = source.read();

			if (char_int == -1) {
				char_int = EOT;
			}

			return (char) char_int;

		} catch (IOException error) {
			System.out.println("ERROR: " + error);
			System.out.println("There was an issue retrieving the next character!");
			return EOT;
		}
	}
}

