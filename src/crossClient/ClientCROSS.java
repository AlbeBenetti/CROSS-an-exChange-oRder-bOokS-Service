package crossClient;


import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import risorseCondivise.*;

public class ClientCROSS {
	
	String scelta;
	Scanner sc;
	Gson gson;
	private String username;
	private String password;
	private int udpPort;
	public ClientCROSS(Scanner sc, Gson gson, int udp) {
		this.sc=sc;
		this.gson=gson;
		this.udpPort=udp;
	}
	public String getJsonRequest(String scelta) {
		this.scelta=scelta;
		return handler();
	}
	
	private String handler() {
		switch (scelta.toLowerCase()) {
		case "register":
			System.out.println("Scegliere username");
			username=sc.nextLine();
			System.out.println("Scegliere password");
			password=sc.nextLine();
			return gson.toJson(new Richiesta<User>("register", new User(username, password, this.udpPort)));
		case "updatecredentials":
			System.out.println("Indicare il proprio username");
			username=sc.nextLine();
			System.out.println("Indicare la vecchia password");
			String oldPassword=sc.nextLine();
			System.out.println("Indicare la nuova password");
			password=sc.nextLine();
			return gson.toJson(new Richiesta<UpdateCredentialsOperation>("updateCredentials", new UpdateCredentialsOperation(username, oldPassword, password)));
		case "login":
			System.out.println("Scegliere username");
			username=sc.nextLine();
			System.out.println("Scegliere password");
			password=sc.nextLine();
			return gson.toJson(new Richiesta<User>("login", new User(username, password, this.udpPort)));
		case "logout":
			JsonObject lgout=new JsonObject();
			return gson.toJson(new Richiesta<JsonObject>("logout", lgout));
		case "insertlimitorder":
			try {
				return order("limit");
			}catch(NumberFormatException e) {System.out.println("Formato inserito on ammissibile"); return null; }
		case "insertstoporder":
			try {
				return order("stop");
			}catch(NumberFormatException e) {System.out.println("Formato inserito on ammissibile"); return null; }
		case "insertmarketorder":
			try {
				return order("mrkt");	
			}catch(NumberFormatException e) {System.out.println("Formato inserito on ammissibile"); return null; }
		case "cancelorder":
			System.out.println("Inserire il numero dell'ordine che si intende cancellare");
			int id;
			try {
				id=Integer.parseInt(sc.nextLine());
				JsonObject jo=new JsonObject();
				jo.addProperty("orderId", id);
				return gson.toJson(new Richiesta<JsonObject>("cancelOrder",jo));
			}catch(NumberFormatException v) {System.out.println("Formato identificatore ordine errato");}
		case "getpricehistory":
			System.out.println("Inserire mese richiesto in formato numerico ");
			try {
				Integer month=Integer.parseInt(sc.nextLine());
				System.out.println("Inserire l'anno richiesto");
				Integer year=Integer.parseInt(sc.nextLine());
				if(month<0 || month>12 || year>2025) throw new NumberFormatException();
				String m=month.toString();
				String y=year.toString();
				if(y.length()==2) y="20"+y;
				if(m.length()==1) m="0"+m;
				JsonObject data=new JsonObject();
				data.addProperty("month", m+y);
				return gson.toJson(new Richiesta<JsonObject>("getPriceHistory", data)); 
			}catch(NumberFormatException v) {System.out.println("Formato del mese o dell'anno inseriti errati");}
			
		default:
			System.out.println("Richiesta errata. Riprovare");
			return null;
			
		}
	}
	
	
	private String order(String type) {
		System.out.println("Selezionare tipo di ordine (ask/bid)");
		String t=sc.nextLine();
		t=t.toLowerCase();
		if(!(t.equals("ask")||t.equals("bid"))) {
			System.out.println("Il tipo di ordine selezionato non è corretto, riprovare");
			return null;
		}
		/*if(choice.toLowerCase().equals("ask")) t=OrderType.ask;
		else t=OrderType.bid;*/
		System.out.println("Selezionare quantità da acquistare o vendere");
		int size=Integer.parseInt(sc.nextLine());
		if(size<=0) {
			System.out.println("Impossibile selezionare una quantità minore o uguale a zero, riprovare");
			return null;
		}
		int price=0;
		if(!type.equals("mrkt")) {
			System.out.println("Inserire il prezzo deisderato");
			price=Integer.parseInt(sc.nextLine());
			if(price<=0) {
				System.out.println("Prezzo inserito non valido");
				return null;
			}
		}
		if(type.equals("mrkt")) {
			JsonObject operation=new JsonObject();
			JsonObject values=new JsonObject();
			values.addProperty("type", t);
			values.addProperty("size", size);
			operation.addProperty("operation", "insertMarketOrder");
			operation.add("values", values);
			return gson.toJson(operation);
			
		}else {
			JsonObject operation=new JsonObject();
			JsonObject values=new JsonObject();
			values.addProperty("type", t);
			values.addProperty("size", size);
			values.addProperty("price", price);
			if(type.equals("limit")) operation.addProperty("operation", "insertLimitOrder");
			else operation.addProperty("operation", "insertStopOrder");
			operation.add("values", values);
			return gson.toJson(operation);
		}
	}
}
