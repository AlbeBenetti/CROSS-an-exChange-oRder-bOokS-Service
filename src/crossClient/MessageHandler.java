package crossClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.google.gson.Gson;

import orderTypes.OrdineEvaso;
import requestMessages.MessaggioOrdineEvaso;

public class MessageHandler implements Runnable {
	DatagramSocket udpSocket;
	Gson gson=new Gson();
	private DatagramPacket dp=new DatagramPacket(new byte[1024*3], 1024*3);

	public MessageHandler(DatagramSocket s) {
		this.udpSocket = s;
	}

	@Override
	public void run() {
		try {
			while (!this.udpSocket.isClosed()) {
				this.udpSocket.receive(dp);
				String jsonString=new String(dp.getData(), 0, dp.getLength(), "UTF-8");
				MessaggioOrdineEvaso mess=gson.fromJson(jsonString, MessaggioOrdineEvaso.class);
				System.out.println("Notifica: "+mess.getNotification());
				for(OrdineEvaso trade : mess.getTrades()) {
					System.out.println("È stata effettuata la seguente transazione relativa all'orderId "+trade.getOrderId());
					System.out.println("Size: "+trade.getSize()+", price:"+trade.getPrice()+", type:"+trade.getType()+", order type: "+trade.getOrderType());
				}
			}
		}catch(SocketException e) {
			if(this.udpSocket.isClosed()) {
				System.out.println("Socket udp chiusa");
			}else {
				e.printStackTrace();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

}
