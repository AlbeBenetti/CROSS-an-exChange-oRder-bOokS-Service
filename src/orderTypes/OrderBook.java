package orderTypes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import crossServer.UserInfo;
import risorseCondivise.OrdersHistory;
import risorseCondivise.Response;

public class OrderBook {

	private int limitAskTotalSize = 0;
	private int limitBidTotalSize = 0;
	private PriorityBlockingQueue<LimitOrder> codaLimitAsk;
	private PriorityBlockingQueue<LimitOrder> codaLimitBid;
	private PriorityBlockingQueue<StopOrder> codaStopAsk;
	private PriorityBlockingQueue<StopOrder> codaStopBid;
	LimitOrderComparator lmc = new LimitOrderComparator();
	StopOrderComparator soc = new StopOrderComparator();
	ConcurrentHashMap<String, UserInfo> loggedInUsers;
	int initialSize = 10;
	CopyOnWriteArrayList<GenericOrder> storicoOrdini;

	public OrderBook(ConcurrentHashMap<String, UserInfo> loggedInUsers, CopyOnWriteArrayList<GenericOrder> storedOrders) {
		this.codaLimitAsk = new PriorityBlockingQueue<LimitOrder>(initialSize, lmc);
		this.codaLimitBid = new PriorityBlockingQueue<LimitOrder>(initialSize, lmc);
		this.codaStopAsk = new PriorityBlockingQueue<StopOrder>(initialSize, soc);
		this.codaStopBid = new PriorityBlockingQueue<StopOrder>(initialSize, soc);
		this.loggedInUsers = loggedInUsers;
		this.storicoOrdini=storedOrders;

		// fai anche per gli altri(?)

	}

	public synchronized MarketOrder marketOrderMovement(String user, OrderType t, int size,
			ArrayList<GenericOrder> ordersCompleted) {
		if ((codaLimitAsk.isEmpty() && t.equals(OrderType.bid)) || (codaLimitBid.isEmpty() && t.equals(OrderType.ask)))
			return null;
		// OrderType mrktType = mrkt.getType();
		if (t == OrderType.bid && this.limitAskTotalSize < size)
			return null;
		if (t == OrderType.ask && this.limitBidTotalSize < size)
			return null;
		LimitOrder bestOffer = ((t == OrderType.bid) ? this.codaLimitAsk.poll() : this.codaLimitBid.poll());
		// verifica race
		// condition!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		/*
		
		 */
		if (bestOffer.getUserOwner().equals(user)) {
			System.out.println("stesso proprietario");
			MarketOrder v = marketOrderMovement(user, t, size, ordersCompleted);
			if (t.equals(OrderType.bid)) {
				this.codaLimitAsk.put(bestOffer);
			} else {
				this.codaLimitBid.put(bestOffer);
			}
			return v;
		}
		if (bestOffer.getSize() > size) {
			bestOffer.setSize(bestOffer.getSize() - size);
			if (t.equals(OrderType.bid)) {
				this.limitAskTotalSize -= size;
				this.codaLimitAsk.put(bestOffer);
			} else {
				this.limitBidTotalSize -= size;
				this.codaLimitBid.put(bestOffer);
			}
			MarketOrder mrkt = new MarketOrder(user, t, size);
			mrkt.setactualPrice(size * bestOffer.getLimitPrice());
			// ordersCompleted.add(mrkt);
			System.out.println("ask total: " + this.limitAskTotalSize + ", bid total: " + this.limitBidTotalSize);
			return mrkt;
		}
		if (bestOffer.getSize() == size) {
			MarketOrder mrkt = new MarketOrder(user, t, size);
			if (t == OrderType.bid)
				this.limitAskTotalSize -= size;
			else
				this.limitBidTotalSize -= size;
			int expense = size * bestOffer.getLimitPrice();
			mrkt.setactualPrice(expense);
			bestOffer.setactualPrice(expense);
			ordersCompleted.add(bestOffer);
			// ordersCompleted.add(mrkt);
			System.out.println("ask total: " + this.limitAskTotalSize + ", bid total: " + this.limitBidTotalSize);
			return mrkt;
		}
		if (bestOffer.getSize() < size) {
			if (t.equals(OrderType.bid))
				this.limitAskTotalSize -= bestOffer.getSize();
			else
				this.limitBidTotalSize -= bestOffer.getSize();
			int expense = bestOffer.getSize() * bestOffer.getLimitPrice();
			MarketOrder mrkt = marketOrderMovement(user, t, size - bestOffer.getSize(), ordersCompleted);
			System.out.println("ask total: " + this.limitAskTotalSize + ", bid total: " + this.limitBidTotalSize);
			if (mrkt == null) {
				if (t.equals(OrderType.bid)) {
					this.codaLimitAsk.add(bestOffer);
					this.limitAskTotalSize += bestOffer.getSize();
				} else {
					this.codaLimitBid.add(bestOffer);
					limitAskTotalSize += bestOffer.getSize();
				}
				return null;
			}
			ordersCompleted.add(bestOffer);
			// ordersCompleted.add(mrkt);
			bestOffer.setactualPrice(expense);
			mrkt.setactualPrice(expense);
			return mrkt;
		}
		return null;
	}

	// vedi race condition!!!!!!!!!!!!!!!!!!!!!!!!!!!!! con poll e peek
	private boolean consumeLimitOrder(LimitOrder lo, ArrayList<GenericOrder> ordersCompleted) {
		if (lo.getType().equals(OrderType.ask)) {
			if (this.codaLimitBid.isEmpty())
				return false;
			LimitOrder bestOffer = this.codaLimitBid.poll();
			if (bestOffer.getUserOwner().equals(lo.getUserOwner())) {
				System.out.println("owner of " + bestOffer.getId() + ": " + bestOffer.getUserOwner());
				System.out.println("owner of " + lo.getId() + ": " + lo.getUserOwner());
				boolean res = consumeLimitOrder(lo, ordersCompleted);
				this.codaLimitBid.put(bestOffer);
				return res;
			}
			if (bestOffer.getLimitPrice() < lo.getLimitPrice()) {
				this.codaLimitBid.put(bestOffer);
				System.out.println("Prezzo troppo basso");
				return false;
			}
			if (bestOffer.getSize() >= lo.getSize()) {
				bestOffer.setSize(bestOffer.getSize() - lo.getSize());
				int expense = lo.getLimitPrice() * lo.getSize();
				bestOffer.setactualPrice(expense);
				lo.setactualPrice(expense);
				this.limitBidTotalSize -= lo.getSize();
				lo.setSize(0);
				ordersCompleted.add(lo);
				// notifica lo della vendita
				if (bestOffer.getSize() != 0)
					this.codaLimitBid.put(bestOffer);
				else {
					ordersCompleted.add(bestOffer);
					// notifica della vendita bestoffer
				}
				ordersCompleted.add(lo);
				return true;
			}
			if (bestOffer.getSize() < lo.getSize()) {
				this.limitBidTotalSize -= bestOffer.getSize();
				bestOffer.setSize(0);
				lo.setSize(lo.getSize() - bestOffer.getSize());
				int expense = bestOffer.getLimitPrice() * bestOffer.getSize();
				bestOffer.setactualPrice(expense);
				ordersCompleted.add(bestOffer);
				consumeLimitOrder(lo, ordersCompleted);
				/*
				 * if(v==-1) { lo.setSize(lo.getSize()+bestOffer.getSize());
				 * this.codaLimitBid.add(bestOffer); return -1; }
				 */
				
				lo.setactualPrice(expense);
				
				System.out.println("Spesa: " + expense);
				return true;
				// ordine evaso
				// notifica bestOffer

			}
		}
		if (lo.getType().equals(OrderType.bid)) {
			if (this.codaLimitAsk.isEmpty())
				return false;
			LimitOrder bestOffer = this.codaLimitAsk.poll();
			if (bestOffer.getUserOwner().equals(lo.getUserOwner())) {
				boolean res = consumeLimitOrder(lo, ordersCompleted);
				this.codaLimitAsk.put(bestOffer);
				return res;
			}
			if (bestOffer.getLimitPrice() > lo.getLimitPrice()) {
				this.codaLimitAsk.put(bestOffer);
				return false;
			}
			if (bestOffer.getSize() >= lo.getSize()) {
				bestOffer.setSize(bestOffer.getSize() - lo.getSize());
				this.limitAskTotalSize -= lo.getSize();
				int expense = bestOffer.getLimitPrice() * lo.getSize();
				bestOffer.setactualPrice(expense);
				lo.setSize(0);
				lo.setactualPrice(expense);
				ordersCompleted.add(lo);
				// notifica lo della vendita
				if (bestOffer.getSize() != 0)
					this.codaLimitAsk.put(bestOffer);
				else {
					ordersCompleted.add(bestOffer);
					// notifica della vendita bestoffer
				}
				return true;
			}
			if (bestOffer.getSize() < lo.getSize()) {
				lo.setSize(lo.getSize() - bestOffer.getSize());
				this.limitAskTotalSize -= bestOffer.getSize();
				bestOffer.setSize(0);
				int expense = bestOffer.getLimitPrice() * bestOffer.getSize();
				bestOffer.setactualPrice(expense);
				lo.setactualPrice(expense);
				ordersCompleted.add(bestOffer);
				consumeLimitOrder(lo, ordersCompleted);
				/*
				 * if(v==-1) { lo.setSize(lo.getSize()+bestOffer.getSize());
				 * this.codaLimitAsk.add(bestOffer); return -1; }
				 */
				
				
				System.out.println("Spesa: " + expense);
				return true;
				// ordine evaso
				// notifica bestOffer
			}
		}
		return false;
	}

	public synchronized int addOrder(String user, OrderType t, int size, String type, int price,
			ArrayList<GenericOrder> ordersCompleted) {
		if (type.equals("mrkt")) {
			MarketOrder mrkt = marketOrderMovement(user, t, size, ordersCompleted);
			if (mrkt == null) {
				return -1;
			}
			System.out.println("Costo marketOrder " + mrkt.getId() + ": " + mrkt.getactualPrice());
			// this.completeOrder();
			ordersCompleted.add(mrkt);
			return mrkt.getId();
		} else if (type.equals("limit")) {
			LimitOrder lo = new LimitOrder(user, t, size, price);
			System.out.println("Sono l'ordine numero " + lo.getId() + ", di: " + lo.getUserOwner());
			consumeLimitOrder(lo, ordersCompleted);
			// this.completeOrder();
			// fulfillOrder(l);
			if (lo.getSize() == 0) {
				System.out.println("Ordine evaso subito. totale size ask: " + this.limitAskTotalSize
						+ " totale size bid: " + this.limitBidTotalSize);
				System.out.println("Spesa totale: " + lo.getactualPrice());
				return lo.getId();
			}
			if (lo.getType() == OrderType.ask) {
				// vedi se mettere synchronized
				codaLimitAsk.add(lo);
				this.limitAskTotalSize += lo.getSize();
				if (this.codaLimitAsk.peek().getLimitPrice() == lo.getLimitPrice()) {
					System.out.println("controllo bid");
					checkStopOrder(OrderType.bid, ordersCompleted);
				}
			} else if (lo.getType() == OrderType.bid) {
				codaLimitBid.add(lo);
				this.limitBidTotalSize += lo.getSize();
				if (this.codaLimitBid.peek().getLimitPrice() == lo.getLimitPrice()) {
					System.out.println("controllo ask");
					checkStopOrder(OrderType.ask, ordersCompleted);
				}
			}
			System.out.println("Valori dopo inserimento limitorder: bid-> " + this.limitBidTotalSize + ", ask-> "
					+ this.limitAskTotalSize);
			this.printOrderBook();
			return lo.getId();
		} else if ("stop".equals(type)) {
			StopOrder lo = new StopOrder(user, t, size, price);
			if (lo.getType() == OrderType.ask) {
				codaStopAsk.add(lo);
			} else if (lo.getType() == OrderType.bid) {
				codaStopBid.add(lo);
			}
			System.out.println("messo");
			checkStopOrder(lo.getType(), ordersCompleted);
			return lo.getId();
		}
		return -1;
	}

	private synchronized boolean checkStopOrder(OrderType t, ArrayList<GenericOrder> ordersCompleted) {
		if (t.equals(OrderType.ask)) {
			while (!this.codaStopAsk.isEmpty()) {
				if(this.codaLimitBid.isEmpty()) {
					break;
				}
				StopOrder bestStopAsk = this.codaStopAsk.peek();
				if (bestStopAsk.getStopPrice() <= this.codaLimitBid.peek().getLimitPrice()) {
					this.codaStopAsk.remove(bestStopAsk);
					MarketOrder mrkt = this.marketOrderMovement(bestStopAsk.getUserOwner(), bestStopAsk.getType(),
							bestStopAsk.getSize(), ordersCompleted);
					if (mrkt != null) {
						bestStopAsk.setactualPrice(mrkt.getactualPrice());
						ordersCompleted.add(bestStopAsk);
					}
				} else{
					break;
				}
			}
		} else {
			while (!this.codaStopBid.isEmpty()) {
				if(this.codaLimitAsk.isEmpty()) {
					return false;
				}
				StopOrder bestStopBid = this.codaStopBid.peek();
				System.out.println("miglior prezzo stop: "+bestStopBid.getStopPrice());
				System.out.println("miglior prezzo limit: "+this.codaLimitAsk.peek().getLimitPrice());
				if (bestStopBid.getStopPrice() >= this.codaLimitAsk.peek().getLimitPrice()) {
					System.out.println("trovato!");
					this.codaStopBid.remove(bestStopBid);
					MarketOrder mrkt = this.marketOrderMovement(bestStopBid.getUserOwner(), bestStopBid.getType(),
							bestStopBid.getSize(), ordersCompleted);
					if (mrkt != null) {
						bestStopBid.setactualPrice(mrkt.getactualPrice());
						ordersCompleted.add(bestStopBid);
					}
				} else{
					break;
				}
			}
		}
		return false;
	}
	
	public HashMap<Integer, OrdersHistory> getHistory(int mese, int anno) {
		HashMap<Integer, OrdersHistory> history=new HashMap<Integer, OrdersHistory>();
		GenericOrder[] ordini=this.storicoOrdini.toArray(new GenericOrder[this.storicoOrdini.size()]);
		Arrays.sort(ordini, Comparator.comparingLong(GenericOrder::getTimestamp));
		System.out.println("Dimensione: "+ordini.length);
		for(GenericOrder order: ordini) {
			Instant instant=Instant.ofEpochMilli(order.getTimestamp());
			LocalDateTime date=LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
			if(date.getYear()<anno)
				continue;
			if(date.getMonthValue()>mese || date.getYear()>anno) {
				return history;
			}
			if(date.getMonthValue()<mese)
				continue;
			int giorno=date.getDayOfMonth();
			System.out.println("giorno "+giorno);
			int price=order.getactualPrice();
			if(!history.containsKey(giorno)) {
				OrdersHistory periodo=new OrdersHistory();
				periodo.setApertura(price);
				periodo.setChisura(price);
				periodo.setMin(price);
				periodo.setMax(price);
				history.put(giorno, periodo);
				continue;
			}
			OrdersHistory periodo=history.get(giorno);
			periodo.setChisura(order.getactualPrice());
			if(periodo.getMin()>price)
				periodo.setMin(price);
			if(periodo.getMax()<price)
				periodo.setMax(price);
			
		}
		//System.out.println(history.get(5).getMax());
		return history; 
	}

	/*
	 * private synchronized boolean checkStopOrder(OrderType t,
	 * ArrayList<GenericOrder> ordersCompleted) { if (t == OrderType.bid) { if
	 * (this.codaStopBid.isEmpty() || this.codaLimitAsk.isEmpty()) return false;
	 * synchronized (this.codaStopBid) { LimitOrder bestAsk =
	 * this.codaLimitAsk.poll(); StopOrder so = this.codaStopBid.poll(); if
	 * (so.getUserOwner().equals(bestAsk.getUserOwner())) { return checkStopOrder(t,
	 * ordersCompleted); } if (bestAsk.getLimitPrice() < so.getStopPrice()) {
	 * this.codaLimitAsk.put(bestAsk); this.codaStopBid.put(so); return false; } if
	 * (so.getSize() <= bestAsk.getSize()) { // this.codaStopBid.poll(); // salva
	 * che hai evaso l'ordine: sia quello stop che non so.setSize(0);
	 * bestAsk.setSize(bestAsk.getSize() - so.getSize()); int expense = so.getSize()
	 * * bestAsk.getLimitPrice(); so.setactualPrice(expense);
	 * bestAsk.setactualPrice(expense); ordersCompleted.add(so);
	 * this.limitAskTotalSize -= so.getSize(); if (bestAsk.getSize() == 0) {
	 * ordersCompleted.add(bestAsk); // this.codaLimitAsk.poll(); return true;
	 * 
	 * } this.codaLimitAsk.put(bestAsk); return true; }
	 * if(so.getSize()>bestAsk.getSize()) {
	 * so.setSize(so.getSize()-bestAsk.getSize());
	 * this.limitAskTotalSize-=bestAsk.getSize(); this.codaStopBid.add(so);
	 * if(checkStopOrder(t, ordersCompleted)) { this.codaStopBid.remove(so); int
	 * expense=bestAsk.getSize()*bestAsk.getLimitPrice();
	 * bestAsk.setactualPrice(expense); so.setactualPrice(expense);
	 * bestAsk.setSize(0); ordersCompleted.add(bestAsk); ordersCompleted.add(so);
	 * return true; } so.setSize(so.getSize()+bestAsk.getSize());
	 * this.codaLimitAsk.add(bestAsk); this.codaStopBid.add(so);
	 * this.limitAskTotalSize+=bestAsk.getSize();
	 * this.limitAskTotalSize+=bestAsk.getSize(); } /*if (checkStopOrder(t,
	 * ordersCompleted)) { this.codaStopBid.poll(); return true; }
	 * this.codaLimitAsk.put(bestAsk); return false; } } else { if
	 * (this.codaStopAsk.isEmpty() || this.codaLimitBid.isEmpty()) return false;
	 * synchronized (this.codaLimitBid) { LimitOrder bestBid =
	 * this.codaLimitBid.poll(); StopOrder so = this.codaStopAsk.poll(); if
	 * (so.getUserOwner().equals(bestBid.getUserOwner())) { return checkStopOrder(t,
	 * ordersCompleted); } if (bestBid.getLimitPrice() > so.getStopPrice()) {
	 * this.codaLimitBid.put(bestBid); this.codaStopAsk.put(so); return false; } if
	 * (so.getSize() <= bestBid.getSize()) { // this.codaStopBid.poll(); // salva
	 * che hai evaso l'ordine: sia quello stop che non so.setSize(0);
	 * bestBid.setSize(bestBid.getSize() - so.getSize());
	 * so.setactualPrice(so.getSize() * bestBid.getLimitPrice());
	 * bestBid.setactualPrice(so.getSize() * bestBid.getLimitPrice());
	 * this.limitBidTotalSize -= so.getSize(); ordersCompleted.add(so); if
	 * (bestBid.getSize() == 0) { ordersCompleted.add(bestBid); return true; }
	 * this.codaLimitBid.put(bestBid); return true; } if (so.getSize() >
	 * bestBid.getSize()) { so.setSize(so.getSize() - bestBid.getSize()); boolean r
	 * = this.checkStopOrder(t, ordersCompleted); if (!r) { so.setSize(so.getSize()
	 * + bestBid.getSize()); this.codaLimitBid.add(bestBid);
	 * this.codaStopAsk.add(so); return false; }
	 * 
	 * int expense = bestBid.getSize() * bestBid.getSize();
	 * so.setactualPrice(expense); bestBid.setactualPrice(expense);
	 * ordersCompleted.add(bestBid); ordersCompleted.add(so); return true; } if
	 * (checkStopOrder(t, ordersCompleted)) { this.codaStopAsk.poll(); return true;
	 * } this.codaLimitBid.put(bestBid); return false; } }
	 * 
	 * }
	 */

	public Response cancelOrder(int orderId, String username) {
		for (LimitOrder l : this.codaLimitAsk) {
			if (l.getId() == orderId) {
				if (!l.getUserOwner().equals(username)) {
					return new Response(100, "order belongs to a different user");
				}
				this.limitAskTotalSize -= l.getSize();
				this.codaLimitAsk.remove(l);
				return new Response(100, "OK");
			}
		}
		for (LimitOrder l : this.codaLimitBid) {
			if (l.getId() == orderId) {
				if (!l.getUserOwner().equals(username)) {
					return new Response(100, "order belongs to a different user");
				}
				this.limitBidTotalSize -= l.getSize();
				this.codaLimitBid.remove(l);
				return new Response(100, "OK");
			}
		}

		return new Response(101, "order does not exists or has already been finalized");
	}

	/*
	 * private void fulfillOrder(ArrayList<GenericOrder> ordersCompleted) {
	 * JsonObject ob = new JsonObject(); // veidi poi for (GenericOrder ord :
	 * orders) {
	 * 
	 * String u = ord.getUserOwner(); // // if(!ordersForUser.containsKey(u)) {
	 * ordersForUser.put(u, new // ArrayList<GenericOrder>());
	 * ordersForUser.get(u).add(ord); } this.ordiniEvasi.add(ord); if
	 * (!this.loggedInUsers.containsKey(ord.getUserOwner())) continue; JsonObject
	 * ordmsg = new JsonObject(); ordmsg.addProperty("orderId", ord.getId());
	 * ordmsg.addProperty("type", ((ord.getType() == OrderType.ask) ? "ask" :
	 * "bid")); if (ord instanceof MarketOrder) ordmsg.addProperty("orderType",
	 * "market"); else if (ord instanceof LimitOrder)
	 * ordmsg.addProperty("orderType", "limit"); else if (ord instanceof StopOrder)
	 * ordmsg.addProperty("orderType", "stop"); ordmsg.addProperty("size",
	 * ord.getOriginalSize()); ordmsg.addProperty("price", ord.getactualPrice());
	 * ordmsg.addProperty("timestamp", System.currentTimeMillis());
	 * ordmsg.addProperty("user", u);
	 * 
	 * if (!(ob.has(u))) { ob.add(u, new JsonArray()); }
	 * ob.getAsJsonArray(u).add(ordmsg); } for (String user : ob.keySet()) { String
	 * userOrds = ob.get(user).toString(); byte[] buf = userOrds.getBytes();
	 * UserInfo userInfo = this.loggedInUsers.get(user); DatagramPacket dp = new
	 * DatagramPacket(buf, buf.length, userInfo.getAddr(), userInfo.getPort()); try
	 * { socketUdp.send(dp); } catch (IOException e) { e.printStackTrace(); } } }
	 */

	/*
	 * private void completeOrder() { JsonObject ob = new JsonObject(); // veidi poi
	 * for (GenericOrder ord : this.ordersCompleted) { String u =
	 * ord.getUserOwner();
	 * 
	 * this.ordiniEvasi.add(ord); if
	 * (!this.loggedInUsers.containsKey(ord.getUserOwner())) continue; JsonObject
	 * ordmsg = new JsonObject(); ordmsg.addProperty("orderId", ord.getId());
	 * ordmsg.addProperty("type", ((ord.getType() == OrderType.ask) ? "ask" :
	 * "bid")); if (ord instanceof MarketOrder) ordmsg.addProperty("orderType",
	 * "market"); else if (ord instanceof LimitOrder)
	 * ordmsg.addProperty("orderType", "limit"); else if (ord instanceof StopOrder)
	 * ordmsg.addProperty("orderType", "stop"); ordmsg.addProperty("size",
	 * ord.getOriginalSize()); ordmsg.addProperty("price", ord.getactualPrice());
	 * ordmsg.addProperty("timestamp", System.currentTimeMillis());
	 * ordmsg.addProperty("user", u);
	 * 
	 * if (!(ob.has(u))) { ob.add(u, new JsonArray()); }
	 * ob.getAsJsonArray(u).add(ordmsg); } for (String user : ob.keySet()) { String
	 * userOrds = ob.get(user).toString(); byte[] buf = userOrds.getBytes();
	 * UserInfo userInfo = this.loggedInUsers.get(user); DatagramPacket dp = new
	 * DatagramPacket(buf, buf.length, userInfo.getAddr(), userInfo.getPort()); try
	 * { socketUdp.send(dp); } catch (IOException e) { e.printStackTrace(); } } }
	 */

	private class OrderComparator implements Comparator<GenericOrder> {
		@Override
		public int compare(GenericOrder l1, GenericOrder l2) {
			int diff = 0;
			if (l1 instanceof LimitOrder && l2 instanceof LimitOrder) {
				LimitOrder lo1 = (LimitOrder) l1;
				LimitOrder lo2 = (LimitOrder) l2;
				diff = Integer.compare(lo1.getLimitPrice(), lo2.getLimitPrice());
				if (lo1.getType() == OrderType.bid) {
					return -diff;
				}
				if (diff == 0)
					return Long.compare(l1.getTimestamp(), l2.getTimestamp());
			}
			if (l1 instanceof StopOrder && l2 instanceof StopOrder) {
				StopOrder so1 = (StopOrder) l1;
				StopOrder so2 = (StopOrder) l2;
				diff = Integer.compare(so1.getStopPrice(), so2.getStopPrice());
				if (l1.getType() == OrderType.bid) {
					return diff = -diff;
				}
				if (diff == 0)
					return Long.compare(l1.getTimestamp(), l2.getTimestamp());
			}
			return diff;
		}
	}

	private class LimitOrderComparator implements Comparator<LimitOrder> {
		@Override
		public int compare(LimitOrder l1, LimitOrder l2) {
			int diff = Integer.compare(l1.getLimitPrice(), l2.getLimitPrice());
			if (l1.getType().equals(OrderType.bid))
				return -diff;
			if (diff == 0)
				return Long.compare(l1.getTimestamp(), l2.getTimestamp());

			return diff;
		}
	}

	private class StopOrderComparator implements Comparator<StopOrder> {
		@Override
		public int compare(StopOrder l1, StopOrder l2) {
			int diff = Integer.compare(l1.getStopPrice(), l2.getStopPrice());
			if (l1.getType().equals(OrderType.bid))
				return -diff;
			if (diff == 0)
				return Long.compare(l1.getTimestamp(), l2.getTimestamp());

			return diff;
		}
	}

	private void printOrderBook() {
		for (LimitOrder l : this.codaLimitAsk) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getLimitPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
		System.out.println("Ordini vendita (ask) terminati. inizio bid----------------");
		for (LimitOrder l : this.codaLimitBid) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getLimitPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
		System.out.println("Ordini acquisto (bid) terminati. inizio stop----------------");
		for (StopOrder l : this.codaStopAsk) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getStopPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
		System.out.println("Ordini acquisto (Ask) terminati. inizio stop bid----------------");
		for (StopOrder l : this.codaStopBid) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getStopPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
	}
}
