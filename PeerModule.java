package peertopeer;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

public class PeerModule {
	
	private ServerSocket serverSocket;
	private static final ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
	private int clientId=0;
	private int chatRoomNo=1000;
	private int nodeId = 1001;
	private String ip_address = "134.226.58.115";

	HashMap<String,String> routingInfo = new HashMap<String,String>();
	
	//constructor to initialize server socket
	public PeerModule(ServerSocket serverSocket)
	{
		this.serverSocket = serverSocket;
	}
	
	//method to accept client connection
	public void init()
	{
		try
		{
			while(true)
			{	
				if(pool.getActiveCount()<pool.getMaximumPoolSize())
				{										
					System.out.println("Waiting for a client to get connected \n-------------------------------------");													
					Socket socket = serverSocket.accept();
					clientId++;
					System.out.println("Connected to a client\n-------------------------------------");
					PeerClient peerClient = new PeerClient(socket,this,clientId);
					PeerModule.pool.execute(peerClient);
				}
				
			}						                       
		}
		catch(Exception e)
		{
			System.out.println(e);
		}	
	}
	
	
	//method to join the network
	public void joinNetwork(Socket socket, JSONObject jsonInput)
	{
		sendRoutingInfo(socket);
		sendRelay();
		addToRouteTable(jsonInput);
	}
	
	private void sendRelay() {
		// TODO Auto-generated method stub
		System.out.println("Sending Relay Messages");
	}

	private void addToRouteTable(JSONObject jsonInput) {
		// TODO Auto-generated method stub
		routingInfo.put((String)jsonInput.get("node_id"), (String)jsonInput.get("ip_address"));
		System.out.println("Current Routing Table \n -----------");
		System.out.println(routingInfo);
	}

	//method for the client to leave a network
	public void leaveNetwork(JSONObject jsonInput)
	{
		routingInfo.remove((String)jsonInput.get("node_id"));
		System.out.println("Routing Table after removing the node\n -----------");
		System.out.println(routingInfo);
	}
	
	//method to chat/store message in a node
	public void chat()
	{
	
	}
	
	//method to get messages for set of tags
	public void chatRetrieve()
	{
	
	}
	
	//method to ping
	public void ping()
	{
	
	}
	
	//hashcode method
	public int hashCode(String str) {
		  int hash = 0;
		  for (int i = 0; i < str.length(); i++) {
		    hash = hash * 31 + str.charAt(i);
		  }
		  return Math.abs(hash);
		}
	
	//method to ping
	public void sendRoutingInfo(Socket socket)
	{
		try
		{
			PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
		
			System.out.println("In send routing info method");
			
			routingInfo.put("1002", "134.226.58.160");
			routingInfo.put("1003", "134.226.58.161");
			routingInfo.put("1004", "134.226.58.162");
			
			
			JSONObject routingJson = new JSONObject();
			routingJson.put("type","ROUTING_INFO");
			routingJson.put("gateway_id",this.nodeId);
			routingJson.put("node_id","1002");
			routingJson.put("ip_address",this.ip_address);
			
			JSONArray routingInfoArray = new JSONArray();
			
			Iterator it = routingInfo.entrySet().iterator();
		    while (it.hasNext()) {
		    	JSONObject routingEntries = new JSONObject();
		        Map.Entry entry = (Map.Entry)it.next();
		        routingEntries.put("node_id", entry.getKey());
		        routingEntries.put("ip_address", entry.getValue());
		        routingInfoArray.add(routingEntries);
		    }
			
		    routingJson.put("route_table",routingInfoArray);
			
			System.out.println(routingJson);
			
			output.println(routingJson);
			
			System.out.println("Hashcode : "+hashCode("abba"));
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendRoutingInfo");
		}
	}

	//peer clients
	public class PeerClient implements Runnable
	{
		private Socket socket;
		private PeerModule peerModule;
		private int clientId;
		private boolean kill;
	
		PeerClient(Socket socket,PeerModule peerModule,int clientId)
		{
			this.socket=socket;
			this.peerModule=peerModule;
			this.clientId=clientId;
			this.kill = false;
		}

		//method to join the network
		public void joinNetwork(JSONObject jsonInput)
		{
			peerModule.joinNetwork(socket,jsonInput);
		}
		
	
		@Override			
		public void run()
		{
			try
			{
				System.out.println("In Peer Client run method :)");
				BufferedReader bd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
				while(!kill)
				{
					System.out.println("JSON Object");

					String input = bd.readLine();
					
					JSONParser jsonInputParser = new JSONParser();
					JSONObject jsonInput  = (JSONObject) jsonInputParser.parse(input);
					
					String jsonString = jsonInput.toJSONString();
					
					if(jsonString.indexOf("JOINING_NETWORK")!=-1)
					{
						joinNetwork(jsonInput);
					}
					else if(jsonString.startsWith("LEAVING_NETWORK")==true)
					{	
						leaveNetwork(jsonInput);
										                   
					}
					else if(jsonString.startsWith("CHAT")==true)
					{
						
					}
					else if(jsonString.startsWith("CHAT_RETRIEVE")==true)
					{
						
					}
					else if(jsonString.startsWith("PING")==true)
					{
						
					}
					else if(jsonString.equals("PINK"))
					{

					}
					else
					{
						//do nothing		
					}
				}
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
	}
	
	public static class ThisPeer implements Runnable
	{
		PeerModule thisPeerModule;
		
		
		ThisPeer(PeerModule peerModule)
		{
			this.thisPeerModule = peerModule;
		}
		
		//method to join the network
		public void joinNetwork()
		{
			JSONObject joinJson = new JSONObject();
			joinJson.put("type","JOINING-NETWORK");
			System.out.println(joinJson);
		}
	
		//method for the client to leave a network
		public void leaveNetwork()
		{
	
		}
	
		//method to chat/store message in a node
		public void chat()
		{
	
		}
	
		//method to get messages for set of tags
		public void chatRetrieval()
		{
	
		}
	
		//method to ping
		public void ping()
		{
		
		}

					
		public void run()
		{
			try
			{
				int userInput;
				do
				{
					System.out.println("Your peer menu");
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
					System.out.println("1. JOIN NETWORK");
					System.out.println("2. CHAT");
					System.out.println("3. CHAT RETRIEVAL");
					System.out.println("4. PING");
					System.out.println("5. LEAVE NETWORK");
					userInput = Integer.parseInt(reader.readLine());
					System.out.println("Your User Input : " + userInput);
					switch(userInput)
					{
						case 1:
							System.out.println("JOIN NETWORK\n---------");
							joinNetwork();
							break;
						case 2:
							System.out.println("CHAT\n---------");
							chat();
							break;
						case 3:
							System.out.println("CHAT RETRIEVAL\n---------");
							chatRetrieval();
							break;
						case 4:
							System.out.println("PING\n---------");
							ping();
							break;
						case 5:			
							System.out.println("LEAVE NETWORK\n---------");
							leaveNetwork();
							break;
						default:
							System.out.println("Default\n---------");
							break;
					}
				}while(userInput!=6);
			}
			catch(Exception e)
			{
				System.out.println("Exception in ThisPeer Run method" + e);
			}
			
		}
		
	}
	
	public static void main(String[] args) throws Exception
	{	
		PeerModule pm = new PeerModule(new ServerSocket(8767));	
		ThisPeer tp = new ThisPeer(pm);
		System.out.println("Starting TP Thread");
		(new Thread(tp)).start();
		System.out.println("Starting PM function");
		pm.init();		   
	}	

}
