package server;

import java.io.IOException;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.verbs.RdmaCmId;

public class ServerEndpointFactory implements RdmaEndpointFactory<ServerEndpoint>{
	private RdmaActiveEndpointGroup<ServerEndpoint> endpointGroup;

    public ServerEndpointFactory(RdmaActiveEndpointGroup<ServerEndpoint> endpointGroup) throws IOException {
        this.endpointGroup = endpointGroup;
    }

    @Override
    public ServerEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
        return new ServerEndpoint(endpointGroup, idPriv, serverSide);
    }
}