package crossServer;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;


import orderTypes.*;

public class ServerMain {
	// coppie username-pw su json
	// qui salvate su una concurrenthashmap contenente gli utenti. chiave:username,
	// valore:password;
	private static Gson gson = new Gson();

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream("configs/serverConfig.properties")) {
			p.load(fis);
		}

		int serverPort = Integer.parseInt(p.getProperty("porta"));
		String indirizzo = p.getProperty("indirizzo");
		String path=System.getProperty("user.dir")+"/src/crossServer/";
		String pathOrdini = path + p.getProperty("pathOrdini");
		System.out.println(pathOrdini);
		String pathUsers = path+ p.getProperty("pathUsers");
		ConcurrentHashMap<String, String> users = loadUsers(pathUsers);
		CopyOnWriteArrayList<GenericOrder> storedOrders=new CopyOnWriteArrayList<GenericOrder>();
		loadOrders(storedOrders, pathOrdini);
		System.out.println("Numero ordini memorizzati --> "+storedOrders.size());
		ConcurrentHashMap<String, UserInfo> loggedUsers = new ConcurrentHashMap<String, UserInfo>();
		ConcurrentLinkedQueue<GenericOrder> ordiniEvasi = new ConcurrentLinkedQueue<GenericOrder>();
		ScheduledExecutorService userSaver = Executors.newSingleThreadScheduledExecutor();
		userSaver.scheduleAtFixedRate(() -> {
			saveUsers(users, pathUsers);
		}, 30L, 30L, TimeUnit.SECONDS);
		ScheduledExecutorService orderSaver = Executors.newSingleThreadScheduledExecutor();
		orderSaver.scheduleWithFixedDelay(() -> {
			saveOrders(storedOrders, ordiniEvasi, pathOrdini);
		}, 30L, 30L, TimeUnit.SECONDS);
		OrderBook orders = new OrderBook(loggedUsers, storedOrders);
		try (ServerSocket socket = new ServerSocket(serverPort);) {
			ExecutorService es = Executors.newCachedThreadPool();
			while (true) {
				es.execute(new CROSSExecutor(socket.accept(), users, loggedUsers, orders, ordiniEvasi));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			saveUsers(users, pathUsers);
		}
	}

	private static ConcurrentHashMap<String, String> loadUsers(String path) {
		try (FileReader freader = new FileReader(path)) {
			Type user = new TypeToken<ConcurrentHashMap<String, String>>() {
			}.getType();
			return gson.fromJson(freader, user);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void saveUsers(ConcurrentHashMap<String, String> users, String path) {
		try (FileWriter userWriter = new FileWriter(path)) {
			String usersString = gson.toJson(users);
			userWriter.write(usersString);
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	private static void saveOrders(CopyOnWriteArrayList<GenericOrder> storedOrders, ConcurrentLinkedQueue<GenericOrder> ordiniEvasi, String path) {
		if (ordiniEvasi.size() == 0) return;
		//System.out.println(storedOrders.size());
		try (JsonWriter orderWriter = new JsonWriter(new FileWriter(path))) {
			// System.out.println(storedOrders.toString());
			synchronized (ordiniEvasi) {
				storedOrders.addAll(ordiniEvasi);
				ordiniEvasi.clear();
			}
				/*for (GenericOrder order : ordiniEvasi) {
					JsonObject o = new JsonObject();
					o.addProperty("orderId", order.getId());
					o.addProperty("type", orderTypeToString(order.getType()));
					o.addProperty("orderType", order.getOrderType());
					o.addProperty("size", order.getOriginalSize());
					o.addProperty("price", order.getactualPrice());
					o.addProperty("timestamp", order.getTimestamp());
					o.addProperty("user", order.getUserOwner());
					allOrders.add(o);
				}*/
			orderWriter.beginArray();
				for(GenericOrder order : storedOrders) {
					orderWriter.beginObject();
	                orderWriter.name("orderID").value(order.getId());
	                orderWriter.name("type").value(orderTypeToString(order.getType()));
	                orderWriter.name("orderType").value(order.getOrderType());
	                orderWriter.name("size").value(order.getOriginalSize());
	                orderWriter.name("price").value(order.getactualPrice());
	                orderWriter.name("timestamp").value(order.getTimestamp());
	                orderWriter.name("user").value(order.getUserOwner());
	                orderWriter.endObject();
				}
			orderWriter.endArray();
			
			/*
			 * JsonObject obj=new JsonObject(); obj.add("trades", allOrders);
			 */
			//gson.toJson(allOrders, orderWriter);
			// orderWriter.write(gson.toJson(allOrders), orderWriter);
			// System.out.println(storedOrders.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadOrders(CopyOnWriteArrayList<GenericOrder> storedOrders, String path) {
		try (InputStreamReader freader = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) {
			JsonElement fileElement = JsonParser.parseReader(freader);
			if (fileElement.isJsonNull()) {
				return;
			}
			JsonArray arr = fileElement.getAsJsonArray();
			for (JsonElement orderElement : arr) {
				JsonObject orderObject = orderElement.getAsJsonObject();
				int orderId = orderObject.get("orderID").getAsInt();
				OrderType type = stringToOrderType(orderObject.get("type").getAsString());
				String orderType = orderObject.get("orderType").getAsString();
				int size = orderObject.get("size").getAsInt();
				int price = orderObject.get("price").getAsInt();
				//System.out.println(price);
				long timestamp = orderObject.get("timestamp").getAsLong();
				String user = orderObject.get("user").getAsString();
				if (!(orderType.equals("market") || orderType.equals("limit") || orderType.equals("stop"))) {
					continue;
				}
				//GenericOrder ord = new GenericOrder(orderId, type, size, price, timestamp, user);
				GenericOrder ord;
				// ord.setOrderType(orderType);
				if (orderType.equals("market")) {
					//ord = new MarketOrder(type, size, orderId, price, timestamp, user);
					storedOrders.add(new MarketOrder(type, size, orderId, price, timestamp, user));
				} else if (orderType.equals("limit")) {
					//ord = new LimitOrder(type, size, orderId, price, timestamp, user);
					storedOrders.add(new LimitOrder(type, size, orderId, price, timestamp, user));
				} else if (orderType.equals("stop")) {
					//ord = new StopOrder(type, size, orderId, price, timestamp, user);
					storedOrders.add(new StopOrder(type, size, orderId, price, timestamp, user));
				}
				//System.out.println("Prezzo: "+price);;
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String orderTypeToString(OrderType t) {
		return ((t.equals(OrderType.ask)) ? "ask" : "bid");
	}

	private static OrderType stringToOrderType(String t) {
		return ((t.equals("ask")) ? OrderType.ask : OrderType.bid);
	}
}
