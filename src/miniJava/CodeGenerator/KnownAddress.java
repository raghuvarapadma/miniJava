package miniJava.CodeGenerator;

import mJAM.Machine;

public class KnownAddress extends RuntimeEntity {
	public int displacement;
	public Machine.Reg registerAddress;

	public KnownAddress(int size, int displacement, Machine.Reg registerAddress) {
		super(size);
		this.displacement = displacement;
		this.registerAddress = registerAddress;
	}
}
