package client_proxy;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPostSend;

import common.CustomEndpoint;

public class ClientEndpoint extends CustomEndpoint{

	public 	ClientEndpoint(RdmaActiveEndpointGroup<? extends CustomEndpoint> endpointGroup, RdmaCmId idPriv, boolean serverSide) throws IOException {
		super(endpointGroup, idPriv, serverSide);
	}
	
	
	public void send(String request) throws IOException, InterruptedException{
		sendBuf.putInt(request.length());
        sendBuf.asCharBuffer().put(request);
        sendBuf.clear();

        IbvSendWR sendWR = new IbvSendWR();
        sendWR.setWr_id(wrId);
        sendWR.setSg_list(sgeListSend);
        sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
        sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
        List<IbvSendWR> wrList = new LinkedList<>();
        wrList.add(sendWR);

        this.postSend(wrList).execute().free();
        this.wcEvents.take();

	}
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
        send.execute().free();
        this.wcEvents.take();

        dataBuf.clear();
        byte[] content = new byte[length];
        dataBuf.get(content);

        return content;
	}

	public byte[] receiveData() throws IOException, InterruptedException {
		receive();
		
		recvBuf.clear();
        byte status = recvBuf.get();

        if (status == RESOURCE_FOUND) {
            long addr = recvBuf.getLong();
            int length = recvBuf.getInt();
            int lkey = recvBuf.getInt();

            return read(addr, length, lkey);
        } else {
            return new byte[0];
        }
	}
}
