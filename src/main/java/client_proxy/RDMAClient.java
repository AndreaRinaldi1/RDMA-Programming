package client_proxy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;

import common.Request;

public class RDMAClient{
	
	RdmaActiveEndpointGroup<ClientEndpoint> endpointGroup;
	private ClientEndpoint endpoint;
	private ServerSocket serverSocket;
	private BlockingQueue<Request> requestQueue;
	
	private RDMAClient(String rdmaAddress, int portInput) throws Exception{
		requestQueue = new LinkedBlockingQueue<Request>();
		
		endpointGroup = new RdmaActiveEndpointGroup<ClientEndpoint>(1000, false, 128, 4, 128);

		ClientEndpointFactory factory = new ClientEndpointFactory(endpointGroup);
		endpointGroup.init(factory);
		
		endpoint = endpointGroup.createEndpoint();
		endpoint.connect(URI.create(rdmaAddress));
		System.out.println("Proxy has been set up");
		
		this.serverSocket = new ServerSocket(portInput);
		
		new Thread(new RequestHandler(requestQueue, endpoint, endpointGroup)).start();
	}

	
	private void run() throws Exception {		

		while(true) {
			Socket socket = serverSocket.accept();
			System.out.println("Connection Estabilished!");
			new Thread(new ClientHandler(requestQueue, socket)).start();	
		}
		
	}

	public static void main(String[] args) throws Exception {
		
		RDMAClient RDMAclient = new RDMAClient(args[0], Integer.parseInt(args[1]));
		RDMAclient.run();
		
	}

	
	






	
}
