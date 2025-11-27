package orderTypes;

public class OrdineEvaso {
	private int orderId;
	private String type;
	private String orderType;
	private int size;
	private int price;
	private long timestamp;
	private String userOwner;
	public OrdineEvaso(int id, int size, int price, String type, String orderType, String owner, long timestamp) {
		this.orderId=id;
		this.size=size;
		this.price=price;
		this.type=type;
		this.orderType=orderType;
		this.timestamp=timestamp;
		this.userOwner=owner;
	}
	public int getOrderId() {
		return orderId;
	}
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	public String getType() {
		return type;
	}
	public String getOrderType() {
		return orderType;
	}
	public int getSize() {
		return size;
	}
	public int getPrice() {
		return price;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public String getUserOwner() {
		return userOwner;
	}

}
