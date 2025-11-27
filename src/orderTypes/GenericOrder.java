package orderTypes;

public class GenericOrder {
	protected int idOrder;
	private String orderType;
	private long timestamp;
	private int originalSize;
	private String userOwner;
	private OrderType type;
	private int size;
	public GenericOrder(int id, String user, OrderType t, int s) {
		this.userOwner=user;
		this.type=t;
		this.idOrder=id;
		this.size=s;
		this.originalSize=s;
		this.timestamp=System.currentTimeMillis();
	}
	public GenericOrder(int id, OrderType t, int size, int price, long timestamp, String user) {
		this.userOwner=user;
		this.type=t;
		this.idOrder=id;
		this.timestamp=timestamp;
		this.size=size;
		this.originalSize=size;
	}
	public int getId() {
		return idOrder;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public String getUserOwner() {
		return userOwner;
	}
	public void setUserOwner(String userOwner) {
		this.userOwner = userOwner;
	}
	public OrderType getType() {
		return type;
	}
	public void setType(OrderType type) {
		this.type = type;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getOriginalSize() {
		return originalSize;
	}
	public String getOrderType() {
		return orderType;
	}
	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}
}
