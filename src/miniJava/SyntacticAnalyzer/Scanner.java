package miniJava.SyntacticAnalyzer;

import miniJava.SourceFile;

public class Scanner {
	private final SourceFile sourceFile;
	private char currentChar;
	private StringBuffer currentSpelling;

	public Scanner(SourceFile sourceFile) {
		this.sourceFile = sourceFile;
		this.currentChar = this.sourceFile.getChar();
	}

	private void takeIt() {
		if (this.currentSpelling == null) {
			this.currentSpelling = new StringBuffer();
		}
		currentSpelling.append(currentChar);
		currentChar = this.sourceFile.getChar();
	}

	private TokenKind scanToken() throws SyntaxException {
		switch (currentChar) {
			case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K':
				case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V':
					case 'W': case 'X': case 'Y': case 'Z': case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g':
						case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q':
							case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
				takeIt();
				while (Character.isLetter(currentChar) || Character.isDigit(currentChar) || currentChar == '_') {
					takeIt();
				}
				switch (currentSpelling.toString()) {
					case "public":
						return TokenKind.PUBLIC;
					case "private":
						return TokenKind.PRIVATE;
					case "class":
						return TokenKind.CLASS;
					case "void":
						return TokenKind.VOID;
					case "static":
						return TokenKind.STATIC;
					case "this":
						return TokenKind.THIS;
					case "return":
						return TokenKind.RETURN;
					case "if":
						return TokenKind.IF;
					case "else":
						return TokenKind.ELSE;
					case "while":
						return TokenKind.WHILE;
					case "true":
						return TokenKind.TRUE;
					case "false":
						return TokenKind.FALSE;
					case "new":
						return TokenKind.NEW;
					case "int":
						return TokenKind.INT;
					case "boolean":
						return TokenKind.BOOLEAN;
					default:
						return TokenKind.IDENTIFIER;
				}
			case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': case '0':
				takeIt();
				while (Character.isDigit(currentChar)) {
					takeIt();
				}
				return TokenKind.NUM;
			case '<':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return TokenKind.LESS_THAN_EQUAL;
				} else {
					return TokenKind.LESS_THAN;
				}
			case '>':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return TokenKind.GREATER_THAN_EQUAL;
				} else {
					return TokenKind.GREATER_THAN;
				}
			case '=':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return TokenKind.EQUIVALENCE_COMPARISON;
				} else {
					return TokenKind.ASSIGNMENT_OPERATOR;
				}
			case '!':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return TokenKind.NOT_EQUAL;
				} else {
					return TokenKind.NOT;
				}
			case '&':
				takeIt();
				if (currentChar == '&') {
					takeIt();
					return TokenKind.AND;
				} else {
					throw new SyntaxException("\"&&\" is the only allowed input. \"&\" is not a valid input and cannot be " +
							"followed by any other character than \"&\".");
				}
			case '|':
				takeIt();
				if (currentChar == '|') {
					takeIt();
					return TokenKind.OR;
				} else {
					throw new SyntaxException("\"||\" is the only allowed input. \"|\" is not a valid input and cannot be " +
							"followed by any other character than \"|\".");
				}
			case '+':
				takeIt();
				return TokenKind.ADD;
			case '-':
				takeIt();
				return TokenKind.MINUS;
			case '*':
				takeIt();
				return TokenKind.MULTIPLY;
			case '/':
				takeIt();
				if (currentChar == '/') {
					takeIt();
					return TokenKind.SINGLE_COMMENT;
				} else if (currentChar == '*') {
					takeIt();
					return TokenKind.MULTI_LINE_COMMENT;
				} else {
					return TokenKind.DIVIDE;
				}
			case '(':
				takeIt();
				return TokenKind.L_PAREN;
			case ')':
				takeIt();
				return TokenKind.R_PAREN;
			case ',':
				takeIt();
				return TokenKind.COMMA;
			case '.':
				takeIt();
				return TokenKind.PERIOD;
			case ';':
				takeIt();
				return TokenKind.SEMICOLON;
			case '{':
				takeIt();
				return TokenKind.L_CURLY_BRACKET;
			case '}':
				takeIt();
				return TokenKind.R_CURLY_BRACKET;
			case '[':
				takeIt();
				return TokenKind.L_SQUARE_BRACKET;
			case ']':
				takeIt();
				return TokenKind.R_SQUARE_BRACKET;
			case SourceFile.EOT:
				return TokenKind.EOT;
			default:
				throw new SyntaxException("The input \"" + currentSpelling + "\" did not match any of the tokens.");
		}
	}

	public void scanSeparator() {
		switch (currentChar) {
			case '\n': case ' ': case '\r': case '\t':
				takeIt();
				break;
		}
	}

	public Token scan() throws SyntaxException {
		while (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r') {
			scanSeparator();
		}
		currentSpelling = new StringBuffer();
		TokenKind currentToken = scanToken();
		if (currentToken == TokenKind.SINGLE_COMMENT) {
			while (currentChar != '\n' && currentChar != '\r' && currentChar != SourceFile.EOT) {
				takeIt();
			}
//			System.out.println(currentSpelling);
			return scan();
		} else if (currentToken == TokenKind.MULTI_LINE_COMMENT) {
			while (true) {
				boolean take = false;
				if (currentChar == '*') {
					takeIt();
					take = true;
					if (currentChar == '/') {
						takeIt();
						break;
					}
				} else if (currentChar == SourceFile.EOT) {
					throw new SyntaxException("Multline comment should end with \"*/\"; however, file was terminated before " +
							"reaching expected character!");
				}
				if (!take) {
					takeIt();
				}
			}
//			System.out.println(currentSpelling);
			return scan();
		} else {
//			System.out.println(currentSpelling);
			return new Token(currentToken, currentSpelling.toString(), null);
		}
	}
}