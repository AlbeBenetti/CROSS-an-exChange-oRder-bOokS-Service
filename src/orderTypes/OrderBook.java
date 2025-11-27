package orderTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import crossServer.UserInfo;
import requestMessages.OrdersHistory;
import requestMessages.Response;

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
	private AtomicInteger idCounter;
	int initialSize = 10;
	CopyOnWriteArrayList<OrdineEvaso> storicoOrdini;
	public OrderBook(ConcurrentHashMap<String, UserInfo> loggedInUsers, CopyOnWriteArrayList<OrdineEvaso> storedOrders, AtomicInteger counter) {
		this.codaLimitAsk = new PriorityBlockingQueue<LimitOrder>(initialSize, lmc);
		this.codaLimitBid = new PriorityBlockingQueue<LimitOrder>(initialSize, lmc);
		this.codaStopAsk = new PriorityBlockingQueue<StopOrder>(initialSize, soc);
		this.codaStopBid = new PriorityBlockingQueue<StopOrder>(initialSize, soc);
		this.loggedInUsers = loggedInUsers;
		this.storicoOrdini=storedOrders;
		this.idCounter=counter;
	}
	
	//Funzione per eseguire un market order. Restutuirà l'ordine solo se questo viene calcolato, altrimenti restituirà  null
	public synchronized MarketOrder marketOrderMovement(String user, OrderType t, int remainingSize, int originalSize, ArrayList<OrdineEvaso> ordersCompleted, StopOrder stop) {
		if ((codaLimitAsk.isEmpty() && t.equals(OrderType.bid)) || (codaLimitBid.isEmpty() && t.equals(OrderType.ask)))
			return null; 
		if (t == OrderType.bid && this.limitAskTotalSize < remainingSize) {
			if(stop!=null)
				stop.setSize(-1);
			return null;
		}
		if (t == OrderType.ask && this.limitBidTotalSize < remainingSize) {
			if(stop!=null)
				stop.setSize(-1);
			return null;
		}
		LimitOrder bestOffer = ((t == OrderType.bid) ? this.codaLimitAsk.poll() : this.codaLimitBid.poll());
		
		if (bestOffer.getUserOwner().equals(user)) {
			System.out.println("stesso proprietario");
			MarketOrder mrkt = marketOrderMovement(user, t, remainingSize, originalSize, ordersCompleted, stop);
			if (t.equals(OrderType.bid)) {
				this.codaLimitAsk.put(bestOffer);
			} else {
				this.codaLimitBid.put(bestOffer);
			}
			return mrkt;
		}
		if (bestOffer.getSize() >= remainingSize) {
			if (t.equals(OrderType.bid)) {
				this.limitAskTotalSize -= remainingSize;
			} else {
				this.limitBidTotalSize -= remainingSize;
			}
			int id=this.idCounter.addAndGet(1);
			MarketOrder mrkt;
			if(stop==null) { 
				mrkt = new MarketOrder(id, user, t, originalSize);				
			} else {
				mrkt= new MarketOrder(stop);
			}
			ordersCompleted.add(new OrdineEvaso(mrkt.getId(), remainingSize, bestOffer.getLimitPrice(), orderTypeToString(mrkt.getType()), mrkt.getOrderType(), mrkt.getUserOwner(), System.currentTimeMillis()));
			ordersCompleted.add(new OrdineEvaso(bestOffer.getId(), remainingSize, bestOffer.getLimitPrice(), orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
			bestOffer.setSize(bestOffer.getSize() - remainingSize);
			if(bestOffer.getSize()>0 && t.equals(OrderType.bid)) {
				this.codaLimitAsk.put(bestOffer);					
			}
			if(bestOffer.getSize()>0 && !t.equals(OrderType.bid)) {
				this.codaLimitBid.put(bestOffer);					
			}
			return mrkt;
		}
		if (bestOffer.getSize() < remainingSize) {
			if (t.equals(OrderType.bid))
				this.limitAskTotalSize -= bestOffer.getSize();
			else
				this.limitBidTotalSize -= bestOffer.getSize();
			MarketOrder mrkt=marketOrderMovement(user, t, remainingSize-bestOffer.getSize(), originalSize, ordersCompleted, stop);
			if(mrkt==null) {
				if (t.equals(OrderType.bid)) {
					this.limitAskTotalSize -= bestOffer.getSize();
					this.codaLimitAsk.put(bestOffer);
				}
				else {
					this.limitBidTotalSize -= bestOffer.getSize();
					this.codaLimitBid.put(bestOffer);
				}
				return mrkt;
			}
			
			ordersCompleted.add(new OrdineEvaso(bestOffer.getId(), bestOffer.getSize(), bestOffer.getLimitPrice(),orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
			
			ordersCompleted.add(new OrdineEvaso(mrkt.getId(), bestOffer.getSize(), bestOffer.getLimitPrice(), orderTypeToString(mrkt.getType()), mrkt.getOrderType(), mrkt.getUserOwner(), System.currentTimeMillis()));
			System.out.println("ask total: " + this.limitAskTotalSize + ", bid total: " + this.limitBidTotalSize);
			return mrkt;
		}
		return null;
	}

	//Funzione necessaria per verificare la possibilità di evadere in manierà totale o parziale un limit order appena ricevuto
	private ArrayList<OrdineEvaso> consumeLimitOrder(LimitOrder lo) {
		if (lo.getType().equals(OrderType.ask)) {
			if (this.codaLimitBid.isEmpty())
				return null;
			LimitOrder bestOffer = this.codaLimitBid.poll();
			if (bestOffer.getUserOwner().equals(lo.getUserOwner())) {
				System.out.println("owner of " + bestOffer.getId() + ": " + bestOffer.getUserOwner());
				System.out.println("owner of " + lo.getId() + ": " + lo.getUserOwner());
				ArrayList<OrdineEvaso> res = consumeLimitOrder(lo);
				this.codaLimitBid.put(bestOffer);
				return res;
			}
			if (bestOffer.getLimitPrice() < lo.getLimitPrice()) {
				this.codaLimitBid.put(bestOffer);
				System.out.println("Prezzo troppo basso");
				return new ArrayList<OrdineEvaso>();
			}
			if (bestOffer.getSize() >= lo.getSize()) {
				ArrayList<OrdineEvaso> res = new ArrayList<OrdineEvaso>();
				res.add(new OrdineEvaso(lo.getId(), lo.getSize(), lo.getLimitPrice(), orderTypeToString(lo.getType()), lo.getOrderType(), lo.getUserOwner(), System.currentTimeMillis()));
				res.add(new OrdineEvaso(bestOffer.getId(), lo.getSize(),bestOffer.getLimitPrice(), orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
				this.limitBidTotalSize -= lo.getSize();
				bestOffer.setSize(bestOffer.getSize() - lo.getSize());
				lo.setSize(0);
				if (bestOffer.getSize() != 0)
					this.codaLimitBid.put(bestOffer);
				return res;
			}
			if (bestOffer.getSize() < lo.getSize()) {
				this.limitBidTotalSize -= bestOffer.getSize();
				ArrayList<OrdineEvaso> res = consumeLimitOrder(lo);
				if(res==null) {
					res = new ArrayList<OrdineEvaso>();
				}
				res.add(new OrdineEvaso(lo.getId(), bestOffer.getSize(), bestOffer.getLimitPrice(), orderTypeToString(lo.getType()), lo.getOrderType(), lo.getUserOwner(), System.currentTimeMillis()));
				res.add(new OrdineEvaso(bestOffer.getId(), bestOffer.getSize(),bestOffer.getLimitPrice(), orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
				lo.setSize(lo.getSize()-bestOffer.getSize());
				bestOffer.setSize(0);
				return res;

			}
		}
		if (lo.getType().equals(OrderType.bid)) {
			if (this.codaLimitAsk.isEmpty())
				return new ArrayList<OrdineEvaso>();
			LimitOrder bestOffer = this.codaLimitAsk.poll();
			if (bestOffer.getUserOwner().equals(lo.getUserOwner())) {
				ArrayList<OrdineEvaso> res = consumeLimitOrder(lo);
				this.codaLimitAsk.put(bestOffer);
				return res;
			}
			if (bestOffer.getLimitPrice() > lo.getLimitPrice()) {
				this.codaLimitAsk.put(bestOffer);
				return new ArrayList<OrdineEvaso>();
			}
			if (bestOffer.getSize() >= lo.getSize()) {
				ArrayList<OrdineEvaso> res = new ArrayList<OrdineEvaso>();
				res.add(new OrdineEvaso(lo.getId(), lo.getSize(), lo.getLimitPrice(), orderTypeToString(lo.getType()), lo.getOrderType(), lo.getUserOwner(), System.currentTimeMillis()));
				res.add(new OrdineEvaso(bestOffer.getId(), lo.getSize(),bestOffer.getLimitPrice(), orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
				this.limitAskTotalSize -= lo.getSize();
				bestOffer.setSize(bestOffer.getSize() - lo.getSize());
				lo.setSize(0);
				if (bestOffer.getSize() != 0)
					this.codaLimitAsk.put(bestOffer);
				return res;
			}
			if (bestOffer.getSize() < lo.getSize()) {
				this.limitAskTotalSize -= bestOffer.getSize();
				lo.setSize(lo.getSize() - bestOffer.getSize());
				ArrayList<OrdineEvaso> res = new ArrayList<OrdineEvaso>();
				res.add(new OrdineEvaso(lo.getId(), bestOffer.getSize(), bestOffer.getLimitPrice(), orderTypeToString(lo.getType()), lo.getOrderType(), lo.getUserOwner(), System.currentTimeMillis()));
				res.add(new OrdineEvaso(bestOffer.getId(), bestOffer.getSize(),bestOffer.getLimitPrice(), orderTypeToString(bestOffer.getType()), bestOffer.getOrderType(), bestOffer.getUserOwner(), System.currentTimeMillis()));
				lo.setSize(lo.getSize()-bestOffer.getSize());
				bestOffer.setSize(0);
				ArrayList<OrdineEvaso> other=consumeLimitOrder(lo);
				if(other!=null) {
					res.addAll(other);
					
				}
				return res;
				
			}
		}
		return new ArrayList<OrdineEvaso>();
	}

	//Metodo necessario per processare un nuovo tentativo di ordine ricevuto in base al tipo richiesto.
	public synchronized int addOrder(String user, OrderType t, int size, String type, int price, ArrayList<OrdineEvaso> ordersCompleted) {
		if (type.equals("mrkt")) {
			MarketOrder mrkt = marketOrderMovement(user, t, size, size, ordersCompleted, null);
			
			ordersCompleted.addAll(checkStopOrder(t));
			this.printOrderBookLimit();
			if(mrkt==null)
				return -1;
			return mrkt.getId();
		} else if (type.equals("limit")) {
			int id=this.idCounter.addAndGet(1);
			LimitOrder lo = new LimitOrder(id, user, t, size, price);
			ArrayList<OrdineEvaso> ordini=consumeLimitOrder(lo);
			if(ordini!=null) {
				ordersCompleted.addAll(ordini);				
			}
			if (lo.getSize() == 0) {
				System.out.println("Ordine evaso subito. totale size ask: " + this.limitAskTotalSize
						+ " totale size bid: " + this.limitBidTotalSize);
				return lo.getId();
			}
			if (lo.getType() == OrderType.ask) {
				codaLimitAsk.add(lo);
				this.limitAskTotalSize += lo.getSize();
					ordersCompleted.addAll(checkStopOrder(OrderType.bid));
			} else if (lo.getType() == OrderType.bid) {
				codaLimitBid.add(lo);
				this.limitBidTotalSize += lo.getSize();
					ordersCompleted.addAll(checkStopOrder(OrderType.ask));
			}
			System.out.println("Valori dopo inserimento limitorder: bid-> " + this.limitBidTotalSize + ", ask-> "
					+ this.limitAskTotalSize);
			this.printOrderBookLimit();
			return lo.getId();
		} else if ("stop".equals(type)) {
			int id=this.idCounter.addAndGet(1);
			StopOrder lo = new StopOrder(id, user, t, size, price);
			if (lo.getType() == OrderType.ask) {
				codaStopAsk.add(lo);
			} else if (lo.getType() == OrderType.bid) {
				codaStopBid.add(lo);
			}
			ordersCompleted.addAll(checkStopOrder(lo.getType()));				
			return lo.getId();
		}
		return -1;
	}

	//Funzione necessaria per verificare se è sata raggiunta la condizione per evadere qualche stop order
	private synchronized ArrayList<OrdineEvaso> checkStopOrder(OrderType t) {
		ArrayList<OrdineEvaso> ordersCompleted = new ArrayList<OrdineEvaso>(); 
		StopOrder bestStop=null;
		MarketOrder mrkt=null;
		if (t.equals(OrderType.ask)) {
			while (!this.codaStopAsk.isEmpty()) {
				if(this.codaLimitBid.isEmpty()) {
					break;
				}
				bestStop = this.codaStopAsk.poll();
				
				if (bestStop.getStopPrice() >= this.codaLimitBid.peek().getLimitPrice()) {
					System.out.println("Controllo... "+bestStop.getId());
					mrkt=this.marketOrderMovement(bestStop.getUserOwner(), bestStop.getType(), bestStop.getSize(), bestStop.getSize(), ordersCompleted, bestStop);
					if(bestStop.getSize()==-1) {
						ordersCompleted.add(new OrdineEvaso(bestStop.getId(), bestStop.getSize(), bestStop.getStopPrice(), orderTypeToString(bestStop.getType()), "stop", bestStop.getUserOwner(), bestStop.getTimestamp()));
						System.out.println("Trovato e rimosso");
					}
					
				} else{
					this.codaStopAsk.add(bestStop);
					break;
				}
			}
		} else {
			while (!this.codaStopBid.isEmpty()) {
				if(this.codaLimitAsk.isEmpty()) {
					break;
				}
				bestStop = this.codaStopBid.poll();
				if (bestStop.getStopPrice() <= this.codaLimitAsk.peek().getLimitPrice()) {
					System.out.println("trovato!");
					this.codaStopBid.remove(bestStop);
					mrkt=this.marketOrderMovement(bestStop.getUserOwner(), bestStop.getType(), bestStop.getSize(),
							bestStop.getSize(), ordersCompleted, bestStop);
					if(bestStop.getSize()==-1) { 
						ordersCompleted.add(new OrdineEvaso(bestStop.getId(), bestStop.getSize(), bestStop.getStopPrice(), orderTypeToString(bestStop.getType()), "stop", bestStop.getUserOwner(), bestStop.getTimestamp()));
						System.out.println("Trovato e rimosso");
					}
				} else{
					break;
				}
			}
		}
		return ordersCompleted;
	}
	
	public LinkedHashMap<String, OrdersHistory> getHistory(int mese, int anno) {
		LinkedHashMap<String, OrdersHistory> history=new LinkedHashMap<String, OrdersHistory>();
		OrdineEvaso[] ordini=this.storicoOrdini.toArray(new OrdineEvaso[this.storicoOrdini.size()]);
		Arrays.sort(ordini, Comparator.comparingLong(OrdineEvaso::getTimestamp));
		System.out.println("Dimensione: "+ordini.length);
		for(OrdineEvaso order: ordini) { 
			Instant instant=Instant.ofEpochMilli(order.getTimestamp());
			LocalDateTime date=LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
			if(date.getYear()<anno)
				continue;
			if(date.getMonthValue()>mese || date.getYear()>anno) {
				return history;
			}
			if(date.getMonthValue()<mese)
				continue;
			String day=date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
			int price=order.getPrice();
			if(!history.containsKey(day)) {
				OrdersHistory periodo=new OrdersHistory();
				periodo.setApertura(price);
				periodo.setChisura(price);
				periodo.setMin(price);
				periodo.setMax(price);
				history.put(day, periodo);
				continue;
			}
			OrdersHistory periodo=history.get(day);
			periodo.setChisura(order.getPrice());
			if(periodo.getMin()>price)
				periodo.setMin(price);
			if(periodo.getMax()<price)
				periodo.setMax(price);
		}
		return history; 
	}

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
		for(StopOrder s :this.codaStopAsk) {
			if(s.getId()==orderId) {
				if (!s.getUserOwner().equals(username)) {
					return new Response(100, "order belongs to a different user");
				}
				this.codaLimitAsk.remove(s);
				return new Response(100, "OK");
			}
		}
		for(StopOrder s: this.codaStopBid) {
			if(s.getId()==orderId) {
				if (!s.getUserOwner().equals(username)) {
					return new Response(100, "order belongs to a different user");
				}
				this.codaLimitBid.remove(s);
				return new Response(100, "OK");
			}
		}
		return new Response(101, "order does not exists or has already been finalized");
	}


	private class LimitOrderComparator implements Comparator<LimitOrder> {
		@Override
		public int compare(LimitOrder l1, LimitOrder l2) {
			int diff = Integer.compare(l1.getLimitPrice(), l2.getLimitPrice());
			if (diff == 0)
				return Long.compare(l1.getTimestamp(), l2.getTimestamp());
			if (l1.getType().equals(OrderType.bid))
				return -diff;

			return diff;
		}
	}

	private class StopOrderComparator implements Comparator<StopOrder> {
		@Override
		public int compare(StopOrder l1, StopOrder l2) {
			int diff = Integer.compare(l1.getStopPrice(), l2.getStopPrice());
			if (diff == 0)
				return Long.compare(l1.getTimestamp(), l2.getTimestamp());
			if (l1.getType().equals(OrderType.bid))
				return -diff;
			return diff;
		}
	}

	private void printOrderBook() {
		this.printOrderBookLimit();
		System.out.println("------------------------\nOrdini di tipo Stop Ask:");
		for (StopOrder l : this.codaStopAsk) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getStopPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
		System.out.println("------------------------\nOrdini di tipo Stop Bid:");
		for (StopOrder l : this.codaStopBid) {
			System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getStopPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
		}
		System.out.println("------------------------");
	}
	private void printOrderBookLimit() {
		if(!this.codaLimitAsk.isEmpty()) {
			System.out.println("------------------------\nOrder Book:\nOrdini di tipo Limit Ask:");
			for (LimitOrder l : this.codaLimitAsk) {
				System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getLimitPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
			}	
		}
		if(!this.codaLimitAsk.isEmpty()) {
			System.out.println("------------------------\nOrdini di tipo Limit Bid:");
			for (LimitOrder l : this.codaLimitBid) {
				System.out.println("Ordine nr: " + l.getId() + " prezzo: " + l.getLimitPrice()+" proprietario: "+l.getUserOwner()+" dimensione: "+l.getSize());
			}
			System.out.println("------------------------");
		}
	}
	private static String orderTypeToString(OrderType t) {
		return ((t.equals(OrderType.ask)) ? "ask" : "bid");
	}

	
}
