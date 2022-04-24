package miniJava.CodeGenerator;

public class Field extends RuntimeEntity {
	public int index;
	public Field() {
		super();
		index = 0;
	}
	public Field(int size, int index) {
		super(size);
		this.index = index;
	}
}
