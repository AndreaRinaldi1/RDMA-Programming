# Assignment 8: Introduction to RDMA Programming

"In this assignment you need to build an HTTP Server which serves static content and an HTTP
client-side proxy [for multiple clients]."

## Implementation

In our implementation, we build an `RDMAServer` instance passing the rdma_address and the file-system path where the static content is located.
In its constructor, an endpointGroup (using `RdmaActiveEndpointGroup`) is created and a serverEndpoint from it. The latter is bound to the rdma_address, and then it waits for the client connection request to come.

After having launched the RDMAServer, we run the client proxy, passing again the rdma_address and the port it has to listen to for incoming client connections. Also in the `RDMAClient` an endpointGroup is created and the clientEndpoint from it. At this point, the proxy enters in a loop accepting connection requests from clients and starting a new Thread for each of them (`ClientHandler`). These runnables are responsible for handling the requests from the clients. We can distinguish three cases:
* if the request is null, it means that the client disconnected, so the connection is closed.
* if the request is not a "GET" method, we ignore it.
* otherwise, the request is put into a queue in an object that also stores the information related to the client that made the request.

During the proxy initializaion, also another thread is started (`RequestHanlder`), that is responsible for handling the requests stored in the queue we just introduced. This thread checks whether the polled `Request` object encapsules a GET / HTTP request to the web server at `www.rdmawebpage.com`. In such case, it uses the clientEndpoint created in the `RDMAClient` initialization, to SEND the request to the server, otherwise it reponds to the client with a 404 Not Found HTTP Response. In the former case, "the server returns the HTML content size and other necessary parameters to the client proxy along with the HTTP response code. The client proxy then fetches the HTML content from the server’s memory using an RDMA READ operation. After fetching, the content is forwarded to the browser for rendering along with the HTTP response code.
The browser starts parsing the HTML content and encounters a URL to an image. The browser issues request for the image which is intercepted by the client proxy. The client-side proxy fetches the image from the server in the way it fetched HTML content and forwards it to the browser along with the received HTTP response code. The browser loads the HTML content along with the image."
In case the Server application is shut down, the client Proxy replies to the client requests for www.rdmawebpage.com with a HTTP 504 (Gateway Time-out), and it replies to requests for other domains again with a 404 Not Found HTTP Response.

## How to Run

1. Compile the source code with the command: `mvn clean package`

2. Run the server with the command: `java -Djava.library.path=/usr/local/lib -cp target/rdma-1.0-SNAPSHOT.jar:lib/disni-1.0-jar-with-dependencies.jar:lib/disni-1.0-jar-with-dependencies.jar server.RDMAServer rdma://10.0.2.15:8080 src/main/resources`

3. Run the Client Proxy with the command: `java -Djava.library.path=/usrocal/lib -cp target/rdma-1.0-SNAPSHOT.jar:lib/disni-1.0-jar-with-dependencies.jar:lib/disni-1.0-jar-with-dependencies.jar client_proxy.RDMAClient rdma://10.0.2.15:8080 8070`

4. Submit requests from one or more clients, e.g.:
* with curl, run the command: `curl -x 127.0.0.1:8070 www.rdmawebpage.com`
* with mozilla firefox, go to Edit > Preferences > Advanced > Connection Settings and select Manual proxy configuration with HTTP Proxy: <127.0.0.1> and Port: <8070>. Then search for www.rdmawebpage.com

The IP address 10.0.2.15 is the one of the VM. You might have to change it.
