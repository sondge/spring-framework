package club.sondge.code.parsing;

public class Courier {
	private String info;
	private String addr;

	public Courier(String addr, String info) {
		this.addr = addr;
		this.info = info;
	}

	@Override
	public String toString() {
		return "Courier{" +
				"info='" + info + '\'' +
				", addr='" + addr + '\'' +
				'}';
	}
}
