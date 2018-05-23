package client_proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPostSend;

import common.CustomEndpoint;

/**
 * 
 * Class modeling the client endpoint.
 * This class has been written following DiSNI's examples at https://github.com/zrlio/disni
 *
 */
public class ClientEndpoint extends CustomEndpoint{

	public 	ClientEndpoint(RdmaActiveEndpointGroup<? extends CustomEndpoint> endpointGroup, RdmaCmId idPriv, boolean serverSide) throws IOException {
		super(endpointGroup, idPriv, serverSide);
	}
	
	
	/**
	 * @param request: request to be sent to the server
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * Send the client's request to the server
	 */
	public void send(String request) throws IOException, InterruptedException{
		this.getSendBuf().putInt(request.length());
		this.getSendBuf().asCharBuffer().put(request);
        sendBuf.clear();

        SVCPostSend postSend = this.postSend(this.getWrList_send());
        postSend.getWrMod(0).setWr_id(4444);
        postSend.execute().free();
        this.getWcEvents().take();

	}
	
	/**
	 * @param addr
	 * @param length
	 * @param lkey
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * Read the response coming from the server
	 */
	private byte[] read(long addr, int length, int lkey) throws IOException, InterruptedException{

		IbvSendWR sendWR = new IbvSendWR();
        sendWR.setWr_id(wrId++);
        sendWR.setSg_list(this.sgeListData);
        sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
        sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
        sendWR.getRdma().setRemote_addr(addr);
        sendWR.getRdma().setRkey(lkey);

        LinkedList<IbvSendWR> wrList = new LinkedList<>();
        wrList.add(sendWR);
        SVCPostSend send = postSend(wrList);
        send.execute();
        this.getWcEvents().take();

        ByteBuffer dataBuf = this.getDataBuf();
        dataBuf.clear();
        byte[] content = new byte[length];
        dataBuf.get(content);

        return content;
	}

	/**
	 * @return: the response from the server
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * Return the response from the client if any, or an empty response if
	 * the resource has not been found.
	 */
	public byte[] receive() throws IOException, InterruptedException {
		this.postRecv(this.getWrList_recv()).execute().free();
        this.getWcEvents().take();
        
        recvBuf = this.getRecvBuf();

		recvBuf.clear();
        byte status = recvBuf.get();

        if (status == RESOURCE_FOUND) {
            long addr = recvBuf.getLong();
            int length = recvBuf.getInt();
            int lkey = recvBuf.getInt();
            recvBuf.clear();
            return read(addr, length, lkey);
        } else {
            return new byte[0];
        }
	}
}

