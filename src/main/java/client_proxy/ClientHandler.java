package client_proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import common.Request;

public class ClientHandler implements Runnable{
	private Socket socket;
    private BufferedReader input;
    private BlockingQueue<Request> requestQueue;

    public ClientHandler(BlockingQueue<Request> requestQueue, Socket socket) throws IOException {
        super();
        this.requestQueue = requestQueue;
        this.socket = socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    
	@Override
	public void run(){
		try {
            while (true) {
                String rawRequest = input.readLine();

                if (rawRequest == null) {
                    System.out.println("Client has disconnected");
                    break;
                } else if (!rawRequest.startsWith("GET")) {
                    continue;
                }

                System.out.println("Queueing client's request");
                requestQueue.add(new Request(this.socket, rawRequest));
            }
        } catch (Exception e) {
        	System.out.println("Exception while reading input from client or while queueing request");
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing connection with client");
                socket.getOutputStream().flush();
                socket.close();
            } catch (IOException e) {
                System.out.println("Exception while closing socket");
                e.printStackTrace();
            }
        }
	}

}
