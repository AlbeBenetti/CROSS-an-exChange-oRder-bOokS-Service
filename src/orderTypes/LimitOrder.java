package orderTypes;

public class LimitOrder extends GenericOrder {
	private int limitPrice;

	public LimitOrder(int id, String username, OrderType t, int size, int lim) {
		super(id, username, t, size);
		super.setOrderType("limit");
		this.limitPrice=lim;
	}
	public LimitOrder( OrderType t, int s, int id, int price, long timestamp, String username) { 
		super(id, t, s, price, timestamp, username);
		super.setOrderType("limit");
	}

	public int getLimitPrice() {
		return limitPrice;
	}

	public void setLimitPrice(int limitPrice) {
		this.limitPrice = limitPrice;
	}

}
