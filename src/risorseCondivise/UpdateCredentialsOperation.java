package risorseCondivise;

public class UpdateCredentialsOperation {
	private String username;
	private String oldPassword;
	private String newPassword;

	public UpdateCredentialsOperation(String usr, String oldP, String newP) {
		this.username=usr;
		this.oldPassword=oldP;
		this.newPassword=newP;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

}
