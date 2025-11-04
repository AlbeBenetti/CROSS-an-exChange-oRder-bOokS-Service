package crossServer;

import java.net.InetAddress;

public class UserInfo {
	
	private InetAddress addr;
	private int port;
	public UserInfo(InetAddress ia, int p) {
		this.addr=ia;
		this.port=p;
	}
	public InetAddress getAddr() {
		return addr;
	}
	public int getPort() {
		return port;
	}
	

}
