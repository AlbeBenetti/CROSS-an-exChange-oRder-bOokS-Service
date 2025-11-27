package requestMessages;



public class User{
	private String username;
	private String password;
	private int udpPort;
	public User(String username, String password, int port) {
		this.setUsername(username);
		this.setPassword(password);
		this.udpPort=port;
	}
	public int getUdpPort() {
		return udpPort;
	}
	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
}
