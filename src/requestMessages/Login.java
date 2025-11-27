package requestMessages;

public class Login {
	String username;
	String password;
	public Login(String u, String p) {
		username=u;
		password=p;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

}
