package common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;

public class CustomEndpoint extends RdmaActiveEndpoint {
	
	protected static final byte RESOURCE_FOUND = 1;
	protected static final byte RESOURCE_NOT_FOUND = 0;
	
	protected int wrId = 1000;
	protected int buffersize = 4096;

	protected ByteBuffer dataBuf;
	protected IbvMr dataMr;
	protected IbvSge sgeData;
	protected LinkedList<IbvSge> sgeListData;
	
	protected ByteBuffer sendBuf;
	protected IbvMr sendMr;
	protected IbvSendWR sendWR;
	protected LinkedList<IbvSendWR> wrListSend;
	protected IbvSge sgeSend;
	protected LinkedList<IbvSge> sgeListSend;
	
	protected ByteBuffer recvBuf;
	protected IbvMr recvMr;
	protected IbvRecvWR recvWR;
	protected LinkedList<IbvRecvWR> wrListRecv;	
	protected IbvSge sgeRecv;
	protected LinkedList<IbvSge> sgeListRecv;

	protected ArrayBlockingQueue<IbvWC> wcEvents;	
	
	public CustomEndpoint(RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> group, RdmaCmId idPriv,
			boolean serverSide) throws IOException {
		super(group, idPriv, serverSide);

		this.sgeData = new IbvSge();
		this.sgeListData = new LinkedList<>();

		this.sgeSend = new IbvSge();
		this.sgeListSend = new LinkedList<IbvSge>();
		this.sendWR = new IbvSendWR();
		this.wrListSend = new LinkedList<IbvSendWR>();		
		
		this.sgeRecv = new IbvSge();
		this.sgeListRecv = new LinkedList<IbvSge>();
		this.recvWR = new IbvRecvWR();
		this.wrListRecv = new LinkedList<IbvRecvWR>();
				
		this.wcEvents = new ArrayBlockingQueue<>(10);	

	}
	
	@Override
	public void init() throws IOException {
		super.init();
		
		this.dataBuf = ByteBuffer.allocateDirect(buffersize);
		this.sendBuf = ByteBuffer.allocateDirect(buffersize);
		this.recvBuf = ByteBuffer.allocateDirect(buffersize);
		
		this.dataMr = registerMemory(dataBuf).execute().free().getMr();
		this.sendMr = registerMemory(sendBuf).execute().free().getMr();
		this.recvMr = registerMemory(recvBuf).execute().free().getMr();

		sgeSend = setUp_ibvSge(sgeSend, sendMr);
		sgeListSend.add(sgeSend);
		sgeRecv = setUp_ibvSge(sgeRecv, recvMr);
		sgeListRecv.add(sgeRecv);
		sgeData = setUp_ibvSge(sgeData, dataMr);
		sgeListData.add(sgeData);
	
		recvWR = new IbvRecvWR();
        recvWR.setWr_id(wrId++);
        recvWR.setSg_list(sgeListRecv);
        wrListRecv.add(recvWR);
		
		this.postRecv(wrListRecv).execute().free();
	}
	

	protected IbvSge setUp_ibvSge(IbvSge ibvSge, IbvMr ibvMr) {
		ibvSge.setAddr(ibvMr.getAddr());
		ibvSge.setLength(ibvMr.getLength());
		ibvSge.setLkey(ibvMr.getLkey());
		return ibvSge;
	}
		
	public void receive() throws IOException, InterruptedException{
		
		IbvRecvWR recvWR = new IbvRecvWR();
        recvWR.setWr_id(wrId++);
        recvWR.setSg_list(sgeListRecv);

        LinkedList<IbvRecvWR> wrList = new LinkedList<>();
        wrList.add(recvWR);

        this.postRecv(wrList).execute().free();
        this.wcEvents.take();
	}
	
		@Override
	public void dispatchCqEvent(IbvWC ibvWC) throws IOException {
		wcEvents.add(ibvWC);
	}


	public ArrayBlockingQueue<IbvWC> getWcEvents() {
		return wcEvents;
	}

	public LinkedList<IbvSendWR> getWrList_send() {
		return wrListSend;
	}

	public LinkedList<IbvRecvWR> getWrList_recv() {
		return wrListRecv;
	}

	public ByteBuffer getDataBuf() {
		return dataBuf;
	}

	public ByteBuffer getSendBuf() {
		return sendBuf;
	}

	public ByteBuffer getRecvBuf() {
		return recvBuf;
	}

	public IbvSendWR getSendWR() {
		return sendWR;
	}

	public IbvRecvWR getRecvWR() {
		return recvWR;
	}
	public IbvMr getDataMr() {
		return dataMr;
	}
	
	public void close() throws IOException, InterruptedException {
		super.close();
		deregisterMemory(this.sendMr);
		deregisterMemory(this.recvMr);
		deregisterMemory(this.dataMr);
	}

}
