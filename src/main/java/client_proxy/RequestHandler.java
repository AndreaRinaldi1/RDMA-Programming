package client_proxy;

import java.io.*;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;

import common.Request;

public class RequestHandler implements Runnable {
    private BlockingQueue<Request> requestQueue;
	private ClientEndpoint endpoint;
	private RdmaActiveEndpointGroup<ClientEndpoint> endpointGroup;

	public RequestHandler(BlockingQueue<Request> requestQueue, ClientEndpoint endpoint, RdmaActiveEndpointGroup<ClientEndpoint> endpointGroup) {
		this.requestQueue = requestQueue;
		this.endpoint = endpoint;
		this.endpointGroup = endpointGroup;
	}


	@Override
	public void run() {
		try{
			while(true) {
				Request request = requestQueue.take();
				String[] requestParts = request.request.split(" ");
				System.out.println("Handling a new request from the client: " + requestParts[1]);
				if (requestParts.length < 3)
					continue;
					
				Matcher m = Pattern.compile("(?:https?://)?www\\.rdmawebpage\\.com(.*)").matcher(requestParts[1]);

				if (m.matches()){
					try {
						this.endpoint.send(m.group(1));
						System.out.println("Request sent to the server");
						byte[] response = this.endpoint.receiveData();
						if(response.length >0) {
							found(requestParts[2], response, request.socket.getOutputStream());
						} else {
							System.out.println("Resource not found");
							notFound(requestParts[2], request.socket.getOutputStream());
						}
					} catch(Exception e) {
						System.out.println("Gateway Timeout");
						gatewayTimeout(requestParts[2], request.socket.getOutputStream());
					}
				} else {
					System.out.println("The requested resource is not www.rdmawebpage.com");
					notFound(requestParts[2], request.socket.getOutputStream());
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				System.out.println("Closing Endpoint");
				endpointGroup.close();
			}catch(Exception e) {
				System.out.println("Could not close endpoint group");
				e.printStackTrace();
			}
		}
	}
	
	private void found(String protocol, byte[] content, OutputStream output) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(protocol + " 200 OK\n");
		sb.append("Date: " + Instant.now().toString() + "\n");
		sb.append("Content-Length: " + content.length +"\n");
		sb.append('\n');
		
		output.write(sb.toString().getBytes());
		output.write(content);
		output.flush();
	}
	
	private void notFound(String protocol, OutputStream output) throws IOException {
		String response = "Page Not Found";
		StringBuilder sb = new StringBuilder();
		sb.append(protocol + " 404 Not found\n");
		sb.append("Date: " + Instant.now().toString() + "\n");
		sb.append("Content-Length: " + response.getBytes().length +"\n");
		sb.append("\n");
		
		output.write(sb.toString().getBytes());
		output.write(response.getBytes());
		output.flush();
	}
	
	private void gatewayTimeout(String protocol, OutputStream output) throws IOException {
		String response = "Gateway Timeout";
		StringBuilder sb = new StringBuilder();
		sb.append(protocol + " 504 Gateway timeout\n");
		sb.append("Date: " + Instant.now().toString() + "\n");
		sb.append("Content-Length: " + response.getBytes().length +"\n");
		sb.append("\n");
		
		output.write(sb.toString().getBytes());
		output.write(response.getBytes());
		output.flush();
	}

}
