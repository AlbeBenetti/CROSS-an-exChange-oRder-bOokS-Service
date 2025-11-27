package crossServer;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
	private static Gson gson = new Gson();
	private static AtomicInteger counter=new AtomicInteger(0);
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		Properties p = new Properties();
		try (FileReader is = new FileReader("src/crossServer/resources/serverConfig.properties");) {
			p.load(is);
		}
		int serverPort = Integer.parseInt(p.getProperty("porta"));
		String path=System.getProperty("user.dir")+"/src/crossServer/";
		String pathOrdini = path + p.getProperty("pathOrdini");
		String pathUsers = path+ p.getProperty("pathUsers");
		int timeout=Integer.parseInt(p.getProperty("timeoutTime"));
		ConcurrentHashMap<String, String> users = loadUsers(pathUsers);
		CopyOnWriteArrayList<OrdineEvaso> storedOrders=new CopyOnWriteArrayList<OrdineEvaso>();
		loadOrders(storedOrders, pathOrdini);
		System.out.println("Numero ordini memorizzati --> "+storedOrders.size());
		ConcurrentHashMap<String, UserInfo> loggedUsers = new ConcurrentHashMap<String, UserInfo>();
		ConcurrentLinkedQueue<OrdineEvaso> ordiniEvasi = new ConcurrentLinkedQueue<OrdineEvaso>();
		ScheduledExecutorService userSaver = Executors.newSingleThreadScheduledExecutor();
		userSaver.scheduleWithFixedDelay(() -> {
			saveUsers(users, pathUsers);
		}, 30L, 30L, TimeUnit.SECONDS);
		ScheduledExecutorService orderSaver = Executors.newSingleThreadScheduledExecutor();
		orderSaver.scheduleWithFixedDelay(() -> {
			saveOrders(storedOrders, ordiniEvasi, pathOrdini);
		}, 30L, 30L, TimeUnit.SECONDS);
		OrderBook orders = new OrderBook(loggedUsers, storedOrders, counter);
		try (ServerSocket socket = new ServerSocket(serverPort);) {
			
			ExecutorService es = Executors.newCachedThreadPool();
			while (true) {
				es.execute(new CROSSExecutor(socket.accept(), users, loggedUsers, orders, ordiniEvasi, timeout));
			}
		} catch (IOException e) {
			if(e instanceof SocketException) {
				System.out.println("Socket chiusa");
			}else {
				e.printStackTrace();
				
			}
		} finally {
			saveUsers(users, pathUsers);
			saveOrders(storedOrders, ordiniEvasi, pathOrdini);
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

	private static void saveOrders(CopyOnWriteArrayList<OrdineEvaso> storedOrders, ConcurrentLinkedQueue<OrdineEvaso> ordiniEvasi, String path) {
		if (ordiniEvasi.size() == 0) {
			return;
		}
		try (JsonWriter orderWriter = new JsonWriter(new FileWriter(path))) {
			synchronized (ordiniEvasi) {
				storedOrders.addAll(ordiniEvasi);
				ordiniEvasi.clear();
			}
			orderWriter.beginArray();
				for(OrdineEvaso order : storedOrders) {
					orderWriter.beginObject();
	                orderWriter.name("orderID").value(order.getOrderId());
	                orderWriter.name("type").value(order.getType());
	                orderWriter.name("orderType").value(order.getOrderType());
	                orderWriter.name("size").value(order.getSize());
	                orderWriter.name("price").value(order.getPrice());
	                orderWriter.name("timestamp").value(order.getTimestamp());
	                orderWriter.name("user").value(order.getUserOwner());
	                orderWriter.endObject();
				}
			orderWriter.endArray();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadOrders(CopyOnWriteArrayList<OrdineEvaso> storedOrders, String path) {
		try (FileReader freader = new FileReader(path)) {
			JsonElement fileElement = JsonParser.parseReader(freader);
			if (fileElement.isJsonNull()) {
				return;
			}
			JsonArray arr = fileElement.getAsJsonArray();
			for (JsonElement orderElement : arr) {
				counter.addAndGet(1);
				JsonObject orderObject = orderElement.getAsJsonObject();
				int orderId = orderObject.get("orderID").getAsInt();
				String type = orderObject.get("type").getAsString();
				String orderType = orderObject.get("orderType").getAsString();
				int size = orderObject.get("size").getAsInt();
				int price = orderObject.get("price").getAsInt();
				long timestamp = orderObject.get("timestamp").getAsLong();
				String user = orderObject.get("user").getAsString();
				storedOrders.add(new OrdineEvaso(orderId, size, price, type, orderType, user, timestamp));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
