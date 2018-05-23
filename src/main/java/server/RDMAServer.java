package server;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaServerEndpoint;

import java.io.IOException;
import java.net.URI;

/**
 * 
 * This class models the Server.
 * Parts of it has been written following DiSNI's examples at https://github.com/zrlio/disni
 *
 */
public class RDMAServer {

	private RdmaActiveEndpointGroup<ServerEndpoint> endpointGroup;
	private RdmaServerEndpoint<ServerEndpoint> serverEndpoint;
	public static String resource_path;
	
	/**
	 * @param addr: the address to bind the server with
	 * @param resource_path: path to the resource
	 * @throws Exception
	 * 
	 * Creates an endpoint group and an endpoint server and binds it to the address past by argument
	 * to the command line
	 */
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
	
	
	/**
	 * 
	 * Waits for a client to connect. When the connection has been established,
	 * the server gets the name of the requested resource, fetches it and sends it back
	 * to the client.
	 */
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

