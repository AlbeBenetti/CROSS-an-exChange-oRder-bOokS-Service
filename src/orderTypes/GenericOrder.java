package orderTypes;


import java.util.concurrent.atomic.AtomicInteger;

public class GenericOrder {
	private int idOrder;
	private static AtomicInteger counter=new AtomicInteger(0);
	private String orderType;
	private long timestamp;
	private int actualPrice=0;
	private int originalSize;
	private String userOwner;
	private OrderType type;
	private int size;
	public GenericOrder(String user, OrderType t, int s) {
		this.userOwner=user;
		this.type=t;
		this.idOrder=counter.addAndGet(1);
		this.size=s;
		this.originalSize=s;
	}
	public GenericOrder(int id, OrderType t, int size, int price, long timestamp, String user) {
		this.userOwner=user;
		this.type=t;
		this.idOrder=id;
		counter.addAndGet(1);
		this.timestamp=timestamp;
		this.actualPrice=price;
		this.size=size;
		this.originalSize=size;
	}
	public int getId() {
		return idOrder;
	}
	public void setTimestamp() {
		this.timestamp=System.currentTimeMillis();
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
	public int getactualPrice() {
		return actualPrice;
	}
	public void setactualPrice(int expense) {
		this.actualPrice += expense;
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
