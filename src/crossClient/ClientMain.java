package crossClient;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import risorseCondivise.*;

public class ClientMain {
	
	private static Gson gson= new Gson();

	public static void main(String[] args) throws IOException {
		Properties p=new Properties();
		String path=System.getProperty("user.dir") +"/ClientProgettoFinale/clientConfig.properties";
		System.out.println("Path:" +path);
		try(FileInputStream fis=new FileInputStream(path)){
			p.load(fis);
		}
		int numPorta=Integer.parseInt(p.getProperty("portaTCP"));
		String indirizzo=p.getProperty("indirizzo");
		int portaUdp=Integer.parseInt(p.getProperty("portaUDP"));
		
		boolean registered=false;
		Scanner sc=new Scanner(System.in);
		ClientCROSS c=new ClientCROSS(sc, gson, portaUdp);
		System.out.printf("porta: %d, indirizzo: %s, portaUdp: %d\n", numPorta, indirizzo, portaUdp);
		try(Socket socket=new Socket(indirizzo, numPorta);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));PrintWriter out=new PrintWriter(socket.getOutputStream());DatagramSocket udpSocket=new DatagramSocket()){
			String actions="Possibili azioni:\n1-Register\n2-UpdateCredentials\n3-Login\n4-Logout\n5-InsertLimitOrder\n6-InsertMarketOrder\n7-InsertStopOrder\n8-CancelOrder\n9-GetPriceHistory\n10-Exit";
			//Thread messageHandler=new Thread(new MessageHandler(udpSocket));
			//messageHandler.start();
			while(true) {
				if(registered) {
					System.out.println(actions);
				}else {
					System.out.println("Possibili azioni:\n1-Register\n2-UpdateCredentials\n3-Login\n4-Exit");
				}
				
				String scelta=sc.nextLine();
				scelta=scelta.toLowerCase();
				if(scelta.equals("exit")) break;
				if(!(scelta.equals("register")||scelta.equals("login")|| scelta.equals("updatecredentials")) && !registered) {
					System.out.println("Impossibile utilizzare operazioni diverse da resigter o login se no si è ancora registrati");
					continue;
				}
				String jsObj=c.getJsonRequest(scelta);
				if(jsObj==null) continue;
				System.out.println(jsObj.toString());
				out.println(jsObj);
				out.flush();
				String res=in.readLine();
				if((scelta.equals("register") || scelta.equals("login") || scelta.equals("updatecredentials")) && !registered) {
					Response r=gson.fromJson(res, Response.class);
					System.out.println("Response: "+r.getResponse()+", error message: "+r.getErrorMessage());
					if(r.getResponse()==100) registered=true;
				}
				if(scelta.endsWith("order") && !(scelta.equals("cancelorder"))) {
					readResponse(res, true, false);
				}else if(scelta.equals("getpricehistory")) {
					HashMap<Integer, OrdersHistory> history=gson.fromJson(res, new TypeToken<HashMap<Integer, OrdersHistory>>() {}.getType());
					for(Integer day :history.keySet()) {
						//System.out.println(day);
						System.out.println("- Giorno: "+day +" --> "+ history.get(day).toString());
					}
				}else{ 
					Response r=gson.fromJson(res, Response.class);
					System.out.println("Response: "+r.getResponse()+", error message: "+r.getErrorMessage());
				}
				if(scelta.equals("logout")) {
					if(registered) {
						registered=false;
						continue;
					}
				}
			}
		} catch(IOException e) {
			//System.out.println("Errore dal server");
			e.printStackTrace();
		}
		
		
	}
	private static void readResponse(String res, boolean order, boolean priceHistory) {
		if(order) {
			OrderResponse r=gson.fromJson(res, OrderResponse.class);
			System.out.println("Order identifier: "+r.orderId);
		}
	}
}
