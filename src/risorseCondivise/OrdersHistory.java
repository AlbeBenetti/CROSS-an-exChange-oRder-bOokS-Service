package risorseCondivise;

import java.util.ArrayList;

public class OrdersHistory {
	
	int apertura;
	int chisura;
	int max;
	int min;
	public OrdersHistory() {
	}
	public int getApertura() {
		return apertura;
	}
	public void setApertura(int apertura) {
		this.apertura = apertura;
	}
	public int getChisura() {
		return chisura;
	}
	public void setChisura(int chisura) {
		this.chisura = chisura;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int getMin() {
		return min;
	}
	public void setMin(int min) {
		this.min = min;
	}
	public String toString() {
		return "Apertura: "+this.apertura+" Chiusura: "+this.chisura+" min: "+this.min+" max: "+this.max;
	}

}
