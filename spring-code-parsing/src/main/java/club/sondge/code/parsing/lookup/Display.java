package club.sondge.code.parsing.lookup;

public abstract class Display {
	public abstract Car getCar();

	public void display() {
		getCar().display();
	}
}
