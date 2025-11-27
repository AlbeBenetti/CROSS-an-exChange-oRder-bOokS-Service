package orderTypes;

public class StopOrder extends GenericOrder {
	private int stopPrice;

	public StopOrder(int id, String username, OrderType t, int size, int stop) {
		super(id, username, t, size);
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
	public void setOrderId(int id) {
		this.idOrder=id;
	}
}
