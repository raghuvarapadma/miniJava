package miniJava.SyntacticAnalyzer;

public class Token {
	private final TokenKind token;
	private final String spelling;
	private final SourcePosition sourcePosition;

	public Token(TokenKind token, String spelling, SourcePosition sourcePosition) {
		this.token = token;
		this.spelling = spelling;
		this.sourcePosition = sourcePosition;
	}

	public TokenKind getTokenKind() {
		return this.token;
	}

	public String getSpelling() {
		return this.spelling;
	}

	public SourcePosition getSourcePosition() {
		return this.sourcePosition;
	}
}
