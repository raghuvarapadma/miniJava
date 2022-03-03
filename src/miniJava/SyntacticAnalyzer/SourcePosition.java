package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	public int start;
	public int finish;

	public SourcePosition() {
		start = 0;
		finish = 0;
	}

	public SourcePosition(int start, int finish) {
		this.start = start;
		this.finish = finish;
	}

	public String toString() {
		return "start - " + this.start + "; finish - " + this.finish;
	}
}
