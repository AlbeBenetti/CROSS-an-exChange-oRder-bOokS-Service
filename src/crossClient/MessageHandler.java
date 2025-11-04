package crossClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MessageHandler implements Runnable {
	DatagramSocket udpSocket;
	private DatagramPacket dp=new DatagramPacket(new byte[1024], 1024);

	public MessageHandler(DatagramSocket s) {
		this.udpSocket = s;
	}

	@Override
	public void run() {
		try {
			while (!this.udpSocket.isClosed()) {
				this.udpSocket.receive(dp);
				String jsonString=new String(dp.getData(), "UTF-8");
				System.out.println("Ordine evaso: " +jsonString);
			}
		} catch (IOException e) {
			System.out.println("Errore: "+e.getMessage());
			e.printStackTrace();
		}

	}

}
