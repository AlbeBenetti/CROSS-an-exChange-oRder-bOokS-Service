package risorseCondivise;

public class Richiesta<T> {
	
	private String operation;
	private T values;
	
	public Richiesta(String op, T val) {
		operation=op;
		values=val;
	}
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public T getValues() {
		return values;
	}

	public void setValues(T values) {
		this.values = values;
	}
}
