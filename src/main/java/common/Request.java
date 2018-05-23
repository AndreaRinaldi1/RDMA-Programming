package common;

import java.net.Socket;

/**
 * 
 * Class representing a request.
 *
 */
public class Request {
	public Socket socket;
	public String request;
	
	public Request(Socket socket, String request) {
		this.socket = socket;
		this.request = request;
	}
	
	
	
}
