package crossServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import orderTypes.*;
import risorseCondivise.*;

public class CROSSExecutor implements Runnable {

	private Socket socket;
	// hash map lega le chiavi rappresentanti gli username alla rispettiva password
	// inserita
	private ConcurrentHashMap<String, String> users;
	//map che lega usename degli utenti attualmente connessi e la porta destinata alla ricezione dei messaggi udp. In uqesto modo è possibile mandare un messaggion udp
	private ConcurrentHashMap<String, UserInfo> loggedInUsers;
	String loggedUsername = null;
	BufferedReader bufreader;
	PrintWriter writer;
	private Gson gson = new Gson();
	OrderBook orders;
	DatagramSocket socketUdp;
	private ConcurrentLinkedQueue<GenericOrder> ordiniEvasi;
	public CROSSExecutor(Socket s, ConcurrentHashMap<String, String> chm, ConcurrentHashMap<String, UserInfo> loggedInUsers, OrderBook orders, ConcurrentLinkedQueue<GenericOrder> ordinievasi) {
		this.socket = s;
		this.users = chm;
		this.loggedInUsers = loggedInUsers;
		this.orders=orders;
		this.ordiniEvasi=ordinievasi;
		try {
			this.socketUdp = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		System.out.println("Client ricevuto");
		try {
			this.bufreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (true) {
				System.out.println("Attendo richiesta. Local: " + this.socket.getLocalPort() + " remote: "+ this.socket.getPort());
				String str = bufreader.readLine();
				System.out.println("Ricevuta stringa: " + str);
				// verifica che non sia null
				convertLine(str);
				writer.flush(); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			//e.printStackTrace();
			//System.exit(1);
			System.out.println("Comunicazione terminata");
		} finally {
			try {
				this.bufreader.close();
				this.writer.close();
				System.out.println("Stream chiusi");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void convertLine(String str) {
		Richiesta r = gson.fromJson(str, Richiesta.class);
		String res = null;
		System.out.println("Operazione richiesta: " + r.getOperation());
		switch (r.getOperation().toLowerCase()) {
		case "register":
			if (this.loggedUsername != null) {
				System.out.println("Username loggato: "+this.loggedUsername);
				res = gson.toJson(new Response(103,
						"client already registered. Please logout before trying to log with another account"));
				//rewriter.println(res);
				break;
			}
			res = registration(str);
			break;
		case "updatecredentials":
			res = changeCredentials(str);
			break;
		case "login":
			res = login(str);
			break;
		case "logout":
			res = logout();
			break;
		case "insertmarketorder":
			res=sendOrder(str, "mrkt");
			break;
		case "insertlimitorder":
			res=sendOrder(str, "limit");
			break;
		case "insertstoporder":
			res=sendOrder(str, "stop");
			break;
		case "cancelorder":
			res=cancelOrder(str);
			break;
		case "getpricehistory":
			res=getPriceHistory(str);
			break;
		}
		writer.println(res);
	}

	private String registration(String str) {
		Richiesta<User> usr = gson.fromJson(str, new TypeToken<Richiesta<User>>() {
		}.getType());
		String tmpUser = usr.getValues().getUsername();
		String psw = usr.getValues().getPassword().trim();
		int port=usr.getValues().getUdpPort();
		String res = null;
		if (users.containsKey(tmpUser)) {
			res = gson.toJson(new Response(102, "username not available"));
		}
		if (!users.containsKey(tmpUser)) {
			if (psw.length() > 4 || psw == psw.replaceAll(" ", "")) {
				res = gson.toJson(new Response(100, "OK"));
				this.users.put(tmpUser, psw);
				//utenti ricevono messaggio udp
				
				this.loggedInUsers.put(tmpUser, new UserInfo(this.socket.getInetAddress(), port));
				this.loggedUsername = tmpUser;
			} else {
				res = gson.toJson(new Response(101, "invalid password"));
			}
		}
		return res;
	}

	private String changeCredentials(String str) {
		Richiesta<UpdateCredentialsOperation> newCred = gson.fromJson(str, new TypeToken<Richiesta<UpdateCredentialsOperation>>() {}.getType());
		String tmpUser = newCred.getValues().getUsername();
		String oldP = newCred.getValues().getOldPassword();
		String newP = newCred.getValues().getNewPassword().trim();
		String res = null;
		if (newP.equals(oldP)) {
			res = gson.toJson(new Response(103, "New password equal to old one"));
		} else if (newP.length() <= 4 || newP.replaceAll(" ", "") != newP) {
			res = gson.toJson(new Response(101, "Invalid new password"));
		} else if (!(users.containsKey(tmpUser)) || (users.containsKey(tmpUser) && !(users.get(tmpUser).equals(oldP)))) {
			res = gson.toJson(new Response(102, "username/old_password mismatch or non existent username"));
		} else if (loggedInUsers.containsKey(tmpUser)) {
			res = gson.toJson(new Response(104, "User already  logged in"));
		} else {
			this.users.put(tmpUser, newP);
			res = gson.toJson(new Response(100, "OK"));
		}
		// controlla se ci sono altri casi di errore
		return res;
	}

	private String login(String str) {
		Richiesta<User> usr = gson.fromJson(str, new TypeToken<Richiesta<User>>() {}.getType());
		String tmpUser = usr.getValues().getUsername();
		String psw = usr.getValues().getPassword();
		int port=usr.getValues().getUdpPort();
		String res = null;
		if (loggedInUsers.containsKey(tmpUser)) {
			res = gson.toJson(new Response(102, "User already  logged in"));
		} else if (this.loggedUsername != null) {
			res = gson.toJson(new Response(103, "Client already logged to another profile. please logout before trying to access"));
		} else if (users.containsKey(tmpUser) && users.get(tmpUser).equals(psw)) {
			res = gson.toJson(new Response(100, "OK"));
			this.loggedInUsers.put(tmpUser, new UserInfo(this.socket.getInetAddress(), port));
			this.loggedUsername=tmpUser;
		} else {
			res = gson.toJson(new Response(101, "username/old_password mismatch or non existent username"));
		}
		return res;
	}

	private String logout() {
		// String res=null;
		if (this.loggedUsername == null) {
			return gson.toJson(new Response(101, "User not logged in"));
		}
		this.loggedInUsers.remove(this.loggedUsername);
		this.loggedUsername = null;
		System.out.println("utente loggato dopo logout: "+this.loggedUsername);
		return gson.toJson(new Response(100, "OK")); 
	}

	private String sendOrder(String str, String tp) {
		System.out.println("avvio lettura ordine, tipo: "+tp);
		Richiesta<SupportOrder> r=gson.fromJson(str, new TypeToken<Richiesta<SupportOrder>>() {}.getType());
		OrderType t=((r.getValues().type.equals("ask"))? OrderType.ask :OrderType.bid);
		ArrayList<GenericOrder> ordersCompleted= new ArrayList<GenericOrder>();
		int retVal=this.orders.addOrder(this.loggedUsername, t, r.getValues().size, tp, r.getValues().price, ordersCompleted);
		for(GenericOrder o : ordersCompleted) {
			System.out.println(o.getOrderType());
		}
		System.out.println(ordersCompleted.toString());
		if(!ordersCompleted.isEmpty()) {
			completeOrder(ordersCompleted);
		}
		JsonObject res=new JsonObject();
		res.addProperty("orderId", retVal);
		String fin=gson.toJson(res);
		System.out.println(fin);
		return fin;
	}
	private String cancelOrder(String str) {
		Richiesta<JsonObject> r=gson.fromJson(str, new TypeToken<Richiesta<JsonObject>>() {}.getType());
		String idOrder=r.getValues().get("orderId").getAsString();
		return gson.toJson(this.orders.cancelOrder(Integer.parseInt(idOrder), this.loggedUsername));
	}
	
	private void completeOrder(ArrayList<GenericOrder> ordersCompleted) {
		JsonObject ob = new JsonObject();
		// veidi poi
		for (GenericOrder ord : ordersCompleted) {
			ord.setTimestamp();
			String u = ord.getUserOwner();
			/*
			 * if(!ordersForUser.containsKey(u)) { ordersForUser.put(u, new
			 * ArrayList<GenericOrder>()); ordersForUser.get(u).add(ord); }
			 */ 
			this.ordiniEvasi.add(ord);
			if (!this.loggedInUsers.containsKey(ord.getUserOwner()))
				continue;
			JsonObject ordmsg = new JsonObject();
			ordmsg.addProperty("orderId", ord.getId());
			ordmsg.addProperty("type", ((ord.getType() == OrderType.ask) ? "ask" : "bid"));
			if (ord instanceof MarketOrder)
				ordmsg.addProperty("orderType", "market");
			else if (ord instanceof LimitOrder)
				ordmsg.addProperty("orderType", "limit");
			else if (ord instanceof StopOrder)
				ordmsg.addProperty("orderType", "stop");
			ordmsg.addProperty("size", ord.getOriginalSize());
			ordmsg.addProperty("price", ord.getactualPrice());
			ordmsg.addProperty("timestamp", System.currentTimeMillis());
			ordmsg.addProperty("user", u);

			if (!(ob.has(u))) {
				ob.add(u, new JsonArray());
			}
			ob.getAsJsonArray(u).add(ordmsg);
		}
		for (String user : ob.keySet()) {
			String userOrds = ob.get(user).toString();
			byte[] buf = userOrds.getBytes();
			UserInfo userInfo = this.loggedInUsers.get(user);
			DatagramPacket dp = new DatagramPacket(buf, buf.length, userInfo.getAddr(), userInfo.getPort());
			try {
				socketUdp.send(dp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private String getPriceHistory(String str) {
		Richiesta<JsonObject> r=gson.fromJson(str, new TypeToken<Richiesta<JsonObject>>() {}.getType());
		String data=r.getValues().get("month").getAsString();
		int mese=Integer.parseInt(data.substring(0, 2));
		int anno=Integer.parseInt(data.substring(2));
		HashMap<Integer, OrdersHistory> res=this.orders.getHistory(mese, anno);
		//System.out.println("Apertura: "+res.get(5).getApertura()+" Chiusura: "+res.get(5).getChisura()+" min: "+res.get(5).getMin()+" max: "+res.get(5).getMax());
		return gson.toJson(res);
	}
}
