package requestMessages;



public class Response{
	private int response;
	private String errorMessage;

	public Response(int response, String errorMessage) {
		this.setResponse(response);
		this.setErrorMessage(errorMessage);
	}

	public int getResponse() {
		return response;
	}

	public void setResponse(int response) {
		this.response = response;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
