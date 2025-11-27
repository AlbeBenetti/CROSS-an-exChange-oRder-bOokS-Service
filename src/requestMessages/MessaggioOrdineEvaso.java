package requestMessages;

import java.util.ArrayList;

import orderTypes.OrdineEvaso;

public class MessaggioOrdineEvaso {
	
	private String notification;
	private ArrayList<OrdineEvaso> trades;
	public MessaggioOrdineEvaso(String notification, ArrayList<OrdineEvaso> trades) {
		this.trades=trades;
		this.notification=notification;
	}
	public String getNotification() {
		return notification;
	}
	public ArrayList<OrdineEvaso> getTrades() {
		return trades;
	}

}
