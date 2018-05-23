package client_proxy;

import java.io.IOException;

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.verbs.RdmaCmId;

/**
*
* This class has been written following DiSNI's examples at https://github.com/zrlio/disni
* 
*/
public class ClientEndpointFactory implements RdmaEndpointFactory<ClientEndpoint>{
	
	private RdmaActiveEndpointGroup<ClientEndpoint> endpointGroup;

    public ClientEndpointFactory(RdmaActiveEndpointGroup<ClientEndpoint> endpointGroup) throws IOException {
        this.endpointGroup = endpointGroup;
    }

    @Override
    public ClientEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
        return new ClientEndpoint(endpointGroup, idPriv, serverSide);
    }

}
