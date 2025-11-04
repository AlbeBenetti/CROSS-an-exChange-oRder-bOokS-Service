package orderTypes;


public class MarketOrder extends GenericOrder{
	public MarketOrder(String user, OrderType t, int s) { 
		super(user, t, s);
		super.setOrderType("market");
	}
	
	public MarketOrder(OrderType t, int s, int id, int price, long timestamp, String username) { 
		super(id, t, s, price, timestamp, username);
		super.setOrderType("market");
	}
}
