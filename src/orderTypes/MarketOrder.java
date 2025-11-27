package orderTypes;


public class MarketOrder extends GenericOrder{
	public MarketOrder(int id, String user, OrderType t, int s) { 
		super(id, user, t, s);
		super.setOrderType("market");
	}
	
	public MarketOrder(OrderType t, int s, int id, int price, long timestamp, String username) { 
		super(id, t, s, price, timestamp, username);
		super.setOrderType("market");
	}
	public MarketOrder(StopOrder s) {
		super(s.getId(), s.getUserOwner(), s.getType(), s.getSize());
		super.setOrderType("market");
	}
}
