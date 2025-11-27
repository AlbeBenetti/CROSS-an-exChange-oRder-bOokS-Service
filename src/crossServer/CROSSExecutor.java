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
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import orderTypes.*;
import requestMessages.*;

public class CROSSExecutor implements Runnable {

	private Socket socket;
	private ConcurrentHashMap<String, String> users;
	private ConcurrentHashMap<String, UserInfo> loggedInUsers;
	String loggedUsername = null;
	BufferedReader bufreader;
	PrintWriter writer;
	private Gson gson = new Gson();
	OrderBook orders;
	DatagramSocket socketUdp;
	int timeout;
	private ConcurrentLinkedQueue<OrdineEvaso> ordiniEvasi;
	public CROSSExecutor(Socket s, ConcurrentHashMap<String, String> users, ConcurrentHashMap<String, UserInfo> loggedInUsers, OrderBook orders, ConcurrentLinkedQueue<OrdineEvaso> ordinievasi, int timeout) {
		this.socket = s;
		this.users = users;
		this.loggedInUsers = loggedInUsers;
		this.orders=orders;
		this.ordiniEvasi=ordinievasi;
		this.timeout=timeout;
		try {
			this.socketUdp = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			this.socket.setSoTimeout(timeout);
			this.bufreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (true) {
				String str = bufreader.readLine();
				convertLine(str);
				writer.flush(); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NullPointerException e) {
			System.out.println("Comunicazione terminata"+e.getMessage());
		} finally {
			try {
				if(this.loggedUsername!=null) {
					this.loggedInUsers.remove(this.loggedUsername);
					this.loggedUsername=null;					
				}
				this.bufreader.close();
				this.writer.close();
				System.out.println("Stream chiusi");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void convertLine(String str) {
		Richiesta r = gson.fromJson(str, Richiesta.class);
		String res = null;
		switch (r.getOperation().toLowerCase()) {
		case "register":
			if (this.loggedUsername != null) {
				System.out.println("Username loggato: "+this.loggedUsername);
				res = gson.toJson(new Response(103,
						"client already registered. Please logout before trying to log with another account"));
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
			res = gson.toJson(new Response(101, "username/password mismatch or non existent username"));
		}
		return res;
	}

	private String logout() {
		if (this.loggedUsername == null) {
			return gson.toJson(new Response(101, "User not logged in"));
		}
		this.loggedInUsers.remove(this.loggedUsername);
		this.loggedUsername = null;
		System.out.println("utente loggato dopo logout: "+this.loggedUsername);
		return gson.toJson(new Response(100, "OK")); 
	}

	private String sendOrder(String str, String tp) {
		Richiesta<SupportOrder> r=gson.fromJson(str, new TypeToken<Richiesta<SupportOrder>>() {}.getType());
		SupportOrder ordine=r.getValues();
		String type=ordine.type;
		if(!(type.equals("ask") || type.equals("bid"))) {
			System.out.println("Tipo errato");
			return null;
		}
		OrderType t=((r.getValues().type.equals("ask"))? OrderType.ask :OrderType.bid);
		ArrayList<OrdineEvaso> ordersCompleted= new ArrayList<OrdineEvaso>();
		int retVal=this.orders.addOrder(this.loggedUsername, t, r.getValues().size, tp, r.getValues().price, ordersCompleted);
		for(OrdineEvaso o : ordersCompleted) {
			System.out.println(o.getOrderType());
		}
		if(!ordersCompleted.isEmpty()) {
			completeOrder(ordersCompleted);
		}
		JsonObject res=new JsonObject();
		res.addProperty("orderId", retVal);
		String fin=gson.toJson(res);
		return fin;
	}
	private String cancelOrder(String str) {
		Richiesta<JsonObject> r=gson.fromJson(str, new TypeToken<Richiesta<JsonObject>>() {}.getType());
		String idOrder=r.getValues().get("orderId").getAsString();
		return gson.toJson(this.orders.cancelOrder(Integer.parseInt(idOrder), this.loggedUsername));
	}
	
	private void completeOrder(ArrayList<OrdineEvaso> ordersCompleted) {
		HashMap<String, ArrayList<OrdineEvaso>> map= new HashMap<String, ArrayList<OrdineEvaso>>();
		for (OrdineEvaso ord : ordersCompleted) {
			String u = ord.getUserOwner();
			if(!(ord.getOrderId()==-1)) {
				this.ordiniEvasi.add(ord);				
			}
			if (!(map.containsKey(u))) {
				map.put(u, new ArrayList<OrdineEvaso>());
			}
			map.get(u).add(ord);
		}
		for (String user : map.keySet()) {
			UserInfo userInfo = this.loggedInUsers.get(user);
			if(userInfo==null)
				continue;
			MessaggioOrdineEvaso mess = new MessaggioOrdineEvaso("closedTrades", map.get(user));
			String res = gson.toJson(mess);
			byte[] buf = res.getBytes();
			DatagramPacket dp = new DatagramPacket(buf, buf.length, userInfo.getAddr(), userInfo.getPort());
			try {
				socketUdp.send(dp);
				System.out.println("Messaggio inviato!");
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
		LinkedHashMap<String, OrdersHistory> res=this.orders.getHistory(mese, anno);
		return gson.toJson(res);
	}
}
