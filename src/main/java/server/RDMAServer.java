package server;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaServerEndpoint;

import java.io.IOException;
import java.net.URI;

public class RDMAServer {

	private RdmaActiveEndpointGroup<ServerEndpoint> endpointGroup;
	private RdmaServerEndpoint<ServerEndpoint> serverEndpoint;
	public static String resource_path;
	
	public RDMAServer(String addr, String resource_path) throws Exception {
		RDMAServer.resource_path = resource_path;
		this.endpointGroup = new RdmaActiveEndpointGroup<ServerEndpoint>(1000, false, 128, 4, 128);
		
		ServerEndpointFactory factory = new ServerEndpointFactory(endpointGroup);
		endpointGroup.init(factory);
		
		serverEndpoint = endpointGroup.createServerEndpoint();

		URI uri = URI.create(addr);
		this.serverEndpoint.bind(uri);
		System.out.println("Server bound to address " + uri.toString());
	}
	
	
	public void run() {
		ServerEndpoint endpoint;
		try {
			endpoint = serverEndpoint.accept();		
			System.out.println("Client connection accepted");
			while(true) {
				System.out.println("Server ready to receive");
				String requestedResource = endpoint.receiveRequest();
				endpoint.send(requestedResource);
				System.out.println("Response sent back to client proxy");
			}
		} catch(IOException | InterruptedException e) {
			System.out.println("Exception while accepting client connection request or while sending the response back to client proxy");
		} finally {
			try {
				serverEndpoint.close();
			}catch(IOException |InterruptedException e) {
				System.out.println("Exception while closing server endpoint");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		RDMAServer rdmaServer = new RDMAServer(args[0], args[1]);
		rdmaServer.run();
	}

}

