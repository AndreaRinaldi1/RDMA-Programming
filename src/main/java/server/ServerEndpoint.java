package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.RdmaCmId;

import common.CustomEndpoint;

public class ServerEndpoint extends CustomEndpoint {

	private static final String IMAGE_PATH = RDMAServer.resource_path + "/network.png";
	private static final String HTML_PATH = RDMAServer.resource_path + "/index.html";

	public ServerEndpoint(RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> group, RdmaCmId idPriv,
			boolean serverSide) throws IOException {
		super(group, idPriv, serverSide);
	}
	
	public String receiveRequest() throws IOException, InterruptedException {
		
		this.postRecv(this.getWrList_recv()).execute().free();
        this.getWcEvents().take();
        
        ByteBuffer recvBuf = this.getRecvBuf();
		recvBuf.clear();
		
		int length = recvBuf.getInt();
		char[] name = new char[length];
		recvBuf.asCharBuffer().get(name);
		return String.valueOf(name);
		
	}
	
	public void send(String requestedResource) throws IOException, InterruptedException {
		this.getDataBuf().clear();
		this.getSendBuf().clear();
		
		switch (requestedResource) {
		case "":
		case "/":
		case "/index.html":
	        this.getDataBuf().put(Files.readAllBytes(Paths.get(HTML_PATH)));

	        sendBufferSetup(true);
			this.postSend(this.getWrList_send()).execute().free();
	        this.getWcEvents().take();
	        
			break;
			
		case "/network.png":
			this.getDataBuf().put(Files.readAllBytes(Paths.get(IMAGE_PATH)));
			
			sendBufferSetup(true);
			this.postSend(this.getWrList_send()).execute().free();
	        this.getWcEvents().take();
	        
			break;
			
		default:

			this.getSendBuf().clear();
			sendBufferSetup(false);
			this.postSend(this.getWrList_send()).execute().free();
	        this.getWcEvents().take();
			break;
		}
		
	}

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

