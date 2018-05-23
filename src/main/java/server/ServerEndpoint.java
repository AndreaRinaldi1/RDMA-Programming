package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.RdmaCmId;

import common.CustomEndpoint;

/**
 * 
 * This class represents the server endpoint.
 * Parts of it has been written following DiSNI's examples at https://github.com/zrlio/disni
 *
 */
public class ServerEndpoint extends CustomEndpoint {

	private static final String IMAGE_PATH = RDMAServer.resource_path + "/network.png";
	private static final String HTML_PATH = RDMAServer.resource_path + "/index.html";

	public ServerEndpoint(RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> group, RdmaCmId idPriv,
			boolean serverSide) throws IOException {
		super(group, idPriv, serverSide);
	}
	
	/**
	 * @return the name of the requested resource
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * Receives the name of the requested resource and returns it as a String
	 */
	public String receiveRequest() throws IOException, InterruptedException {
		
		this.receive();

		recvBuf.clear();
		int length = recvBuf.getInt();
		char[] name = new char[length];
		recvBuf.asCharBuffer().get(name);
		return String.valueOf(name);
		
	}
	
	/**
	 * @param requestedResource: the resource requested by the client
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * Fetches the requested resource and sends it back to the proxy
	 */
	public void send(String requestedResource) throws IOException, InterruptedException {
		this.dataBuf.clear();
		this.sendBuf.clear();

		switch (requestedResource) {
		case "":
		case "/":
		case "/index.html":
	        this.dataBuf.put(Files.readAllBytes(Paths.get(HTML_PATH)));

	        sendBufferSetup(true);
	        descriptorSetup();
	        this.wcEvents.take();
	        
			break;
			
		case "/network.png":
			this.dataBuf.put(Files.readAllBytes(Paths.get(IMAGE_PATH)));
			
			sendBufferSetup(true);
	        descriptorSetup();
	        this.wcEvents.take();
	        
			break;
			
		default:
			this.sendBuf.clear();
			sendBufferSetup(false);
			descriptorSetup();
			this.wcEvents.take();
			break;
		}
		
	}

	/**
	 * @throws IOException
	 * 
	 * Sets up the descriptor in order to send the response back to the proxy.
	 */
	private void descriptorSetup() throws IOException {
		IbvSendWR sendWRFound = new IbvSendWR();
		sendWRFound.setWr_id(this.wrId);
		sendWRFound.setSg_list(this.sgeListSend);
		sendWRFound.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWRFound.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);

		List<IbvSendWR> wrListFound = new LinkedList<>();
		wrListFound.add(sendWRFound);

		this.postSend(wrListFound).execute().free();
	}
	
	/**
	 * @param resourceFound: boolean signaling whether the resource has been found or not
	 * 
	 * Sets up the send buffer depending on whether the requested resource has been found or not
	 */
	private void sendBufferSetup(boolean resourceFound) {
		
		if (resourceFound) {
			this.sendBuf.put(CustomEndpoint.RESOURCE_FOUND);
			this.sendBuf.putLong(this.dataMr.getAddr());
			this.sendBuf.putInt(this.dataBuf.position());
			this.sendBuf.putInt(this.dataMr.getLkey());
			this.sendBuf.clear();
		} else {
			sendBuf.put(CustomEndpoint.RESOURCE_NOT_FOUND);
			sendBuf.clear();
			
		}
		
	}
	
}

