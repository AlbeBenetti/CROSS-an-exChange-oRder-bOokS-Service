package orderTypes;

public class StopOrder extends GenericOrder {
	private int stopPrice;

	public StopOrder(String username, OrderType t, int size, int stop) {
		super(username, t, size);
		super.setOrderType("stop");
		this.stopPrice=stop;
	}
	public StopOrder(OrderType t, int s, int id, int price, long timestamp, String username) { 
		super(id, t, s, price, timestamp, username);
		super.setOrderType("stop");
	}

	public int getStopPrice() {
		return stopPrice;
	}

	public void setStopPrice(int stopPrice) {
		this.stopPrice = stopPrice;
	}

}
