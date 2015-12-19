package peertopeer;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.apache.commons.collections.map.MultiValueMap;

public class PeerClient3 
{
	
	private ServerSocket serverSocket;
	private static final ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
	private static int peerId=0;
	private static String nodeId = "1003";
	private static String ipAddress = null;
	private static Socket clientSoc = null;
	private static PrintWriter output = null;
	PeerClient peerClient = null;

	static HashMap<String,String> routingInfo = new HashMap<String,String>();
	MultiValueMap chatMessages = new MultiValueMap();
	
	
	//constructor to initialize server socket / current system socket
	public PeerClient3(ServerSocket serverSocket)
	{
		this.serverSocket = serverSocket;
	}
	
	//method to accept peer connection and start the peer thread
	public void init()
	{
		try
		{
			ipAddress = InetAddress.getLocalHost().getHostAddress().toString();
			while(true)
			{	
				if(pool.getActiveCount()<pool.getMaximumPoolSize())
				{	
					initializeRoutingTable();
					System.out.println("Waiting for CLIENT : \n-------------------------------------");													
					Socket socket = serverSocket.accept();
					peerId++;
					System.out.println("CLIENT Connected\n-------------------------------------");
					peerClient = new PeerClient(socket,this,peerId);
					PeerClient3.pool.execute(peerClient);
				}
				
			}						                       
		}
		catch(Exception e)
		{
			System.out.println(e);
		}	
	}
	
	//method to initialize the intial routing table of this system
	private void initializeRoutingTable() 
	{
		routingInfo.put("9001", "134.226.58.160");
		routingInfo.put("1004", "134.226.58.160");
		routingInfo.put("9003", "134.226.58.160");
		routingInfo.put("9004", "134.226.58.160");
	}

	//method to join the peer to the network
	public void joinNetwork(Socket socket, JSONObject jsonInput)
	{
		sendRoutingInfo(socket,jsonInput);
		sendRelay(jsonInput);
		addToRouteTable(jsonInput);
	}
	
	
	//method to send routing info to a joining node
	public void sendRoutingInfo(Socket socket, JSONObject jsonInput)
	{
		try
		{
			PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
			JSONObject routingJson = new JSONObject();
			JSONArray routingInfoArray = new JSONArray();
			
			routingJson.put("type","ROUTING_INFO");
			routingJson.put("gateway_id",nodeId);
			routingJson.put("node_id",jsonInput.get("node_id"));
			routingJson.put("ip_address",ipAddress);
			
			Iterator it = routingInfo.entrySet().iterator();
            while (it.hasNext()) 
            {
            	JSONObject routingEntries = new JSONObject();
            	Map.Entry entry = (Map.Entry)it.next();
            	routingEntries.put("node_id", entry.getKey());
            	routingEntries.put("ip_address", entry.getValue());
            	routingInfoArray.add(routingEntries);
            }
            routingJson.put("route_table",routingInfoArray);
			System.out.println(routingJson);
			output.println(routingJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendRoutingInfo");
		}
	}
	
	//method to send relay messages about peer joining
	private void sendRelay(JSONObject jsonInput) 
	{
		System.out.println("Sending Relay Messages");
		System.out.println("Find out numerically closest node from the routing table");
		int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());
		int intialDiff = Math.abs(Integer.parseInt(nodeId) - nodeChecked);
		
		int nextNode = 0;
		int closest = Integer.parseInt(nodeId);
		int diff = 0;
		String closestNode = nodeId;
		String closestIp = ipAddress;
		
		Iterator it = routingInfo.entrySet().iterator();
		
		while(it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			nextNode = Integer.parseInt(entry.getKey().toString());
			diff =  Math.abs(nextNode - nodeChecked);
			if((diff<intialDiff))
			{
				closest = nextNode;
				closestIp = (String) entry.getValue();
				intialDiff = diff;
			}
		}
		
		System.out.println("CLOSEST NODE : " + closest);
		
		closestNode = Integer.toString(closest);
		if(!closestNode.equals(nodeId))
		{
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("type","JOINING_NETWORK_RELAY");
			jsonObj.put("node_id",jsonInput.get("node_id"));
			if(jsonInput.containsKey("gateway_id"))
			{
				jsonObj.put("gateway_id",jsonInput.get("gateway_id"));
			}
			else
			{
				jsonObj.put("gateway_id",nodeId);
			}
			if(jsonInput.containsKey("gateway_ip"))
			{
				jsonObj.put("gateway_ip",jsonInput.get("gateway_ip"));
			}
			else
			{
				jsonObj.put("gateway_ip",ipAddress);
			}
			sendMessage(closestNode,closestIp,9999,jsonObj);
		}
		else
		{
			System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
		}
		
		
	}
	
	private void sendMessage(String node, String ipAddress,int port,JSONObject jsonObj)
	{
		try
		{
			clientSoc = new Socket(ipAddress,port);
			
			System.out.println("SOCKET CONNECTED");
			BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
		
			output = new PrintWriter(clientSoc.getOutputStream(),true);
			System.out.println("NOW---------" + jsonObj);
			output.println(jsonObj);
			System.out.println("SENT MESSAGE");
		}
		catch(Exception e)
		{
			
		}
	}

	//method to add current joining node to the routing table
	private void addToRouteTable(JSONObject jsonInput) 
	{
		if(jsonInput.get("type").equals("JOINING_NETWORK"))
		{
			routingInfo.put((String)jsonInput.get("node_id"), (String)jsonInput.get("ip_address"));
		}
		else if(jsonInput.get("type").equals("ROUTING_INFO"))
		{
			JSONArray routingInfoArray = (JSONArray) jsonInput.get("route_table");
			Iterator<JSONObject> routingJsonIterator = routingInfoArray.iterator();
			while(routingJsonIterator.hasNext())
			{
				JSONObject routingJson = routingJsonIterator.next();
				routingInfo.put((String)routingJson.get("node_id"), (String)routingJson.get("ip_address"));
			}
		}
		
		System.out.println("UPDATED THE ROUTING TABLE\n -----------");
		System.out.println(routingInfo);
	}

	//method to remove the leaving peer from the routing table 
	public void leaveNetwork(JSONObject jsonInput)
	{
		routingInfo.remove((String)jsonInput.get("node_id"));
		System.out.println("Routing Table after removing the node\n -----------");
		System.out.println(routingInfo);
	}
	
	//method to store the chat message from the peer
	public void chat(JSONObject jsonInput)
	{
		try
		{
		if(jsonInput.get("target_id").equals(nodeId))
		{
			String tag = (String)jsonInput.get("tag");
			String text = (String)jsonInput.get("text");
			chatMessages.put(tag, text);
			
			Iterator it = chatMessages.entrySet().iterator();
			while(it.hasNext()) 
			{
				Map.Entry entry = (Map.Entry)it.next();
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());	
			}
			sendACK(tag,jsonInput);
		}
		else
		{
			System.out.println(jsonInput);
			int nodeChecked = Integer.parseInt(jsonInput.get("target_id").toString());
			
			if(routingInfo.containsKey(jsonInput.get("target_id")))
			{
				System.out.println("I have the node to be messaged in my routing table");
				String target_ip = routingInfo.get(jsonInput.get("target_id"));
				sendMessage(jsonInput.get("target_id").toString(),target_ip,5555,jsonInput);
			}
			else
			{
				int intialDiff = Math.abs(Integer.parseInt(nodeId) - nodeChecked);
				int nextNode = 0;
				int closest = Integer.parseInt(nodeId);
				int diff = 0;
				String closestNode = nodeId;
				String closestIp = ipAddress;
			
				Iterator it = routingInfo.entrySet().iterator();
			
				while(it.hasNext())
				{
					Map.Entry entry = (Map.Entry)it.next();
					nextNode = Integer.parseInt(entry.getKey().toString());
					diff =  Math.abs(nextNode - nodeChecked);
					if((diff<intialDiff))
					{
						closest = nextNode;
						closestIp = (String) entry.getValue();
						intialDiff = diff;
					}
				}
			
				System.out.println("CLOSEST NODE : " + closest);
				
				closestNode = Integer.toString(closest);
				if(!closestNode.equals(nodeId))
				{
					sendMessage(closestNode,closestIp,9999,jsonInput);
				}
				else
				{
					System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
				}
			/*BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
			output = new PrintWriter(clientSoc.getOutputStream(),true);
			output.println(jsonObj);
			String messagefromserver = bd.readLine();
			
			JSONParser jsonInputParser = new JSONParser();
			JSONObject jsonInputFromServer = (JSONObject) jsonInputParser.parse(messagefromserver);
	
			System.out.println("Message from Server to you for CHAT : "+jsonInputFromServer+" \n");*/
			}
		
		}
		}
		catch(Exception e)
		{
			
		}
	}
	
	//method to send ACK for chat Message index
	private void sendACK(String tag,JSONObject jsonInput) 
	{
		try
		{
			//PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
			JSONObject ackJson = new JSONObject();
			ackJson.put("type","ACK_CHAT");
			ackJson.put("node_id",jsonInput.get("node_id"));
			ackJson.put("tag",tag);
			sendMessage(jsonInput.get("sender_id").toString(),peerClient.socket.getLocalAddress().toString(),9999,ackJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendACK");
		}
	}

	//method to retrieve the chat message from a peer and send chat response to a peer
	public void chatRetrieve(Socket socket,JSONObject jsonInput)
	{
		try
		{
			String tag = (String) jsonInput.get("tag");
			PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
			String matchedTag = "";
			String textString = "";
			JSONArray matchedTextsArray = new JSONArray();
			
			Iterator it = chatMessages.entrySet().iterator();
			while(it.hasNext()) 
			{
				Map.Entry entry = (Map.Entry)it.next();
				if(entry.getKey().equals(tag))
				{
					matchedTag = (String) entry.getKey();
					String tempString = entry.getValue().toString();
					textString = tempString.substring(1, tempString.length()-1);
				}
			}
			
			String[] matchedTexts = textString.split(",");
			JSONObject chatResponseJson = new JSONObject();
			chatResponseJson.put("type","CHAT_RESPONSE");
			chatResponseJson.put("tag",tag);
			chatResponseJson.put("node_id",jsonInput.get("node_id"));
			chatResponseJson.put("sender_id",nodeId);
			for(int i=0;i<matchedTexts.length;i++)
			{
				JSONObject tempJson = new JSONObject();
				tempJson.put("text", matchedTexts[i]);
				matchedTextsArray.add(tempJson);
			}
			chatResponseJson.put("response",matchedTextsArray);
			System.out.println(chatResponseJson);
			output.println(chatResponseJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in chatRetrieve");
		}
	}
	
	//method to acknowledge for ping message
	public void ping(Socket socket,JSONObject jsonInput)
	{
		try
		{
			PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
			JSONObject pinAckJson = new JSONObject();
			pinAckJson.put("type","ACK");
			pinAckJson.put("node_id",jsonInput.get("node_id"));
			pinAckJson.put("ip_address",ipAddress);
			System.out.println(pinAckJson);
			output.println(pinAckJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendACK");
		}
	}
	
	//method to send routing info to gateway node
	public void sendRoutingInfoToGateway(JSONObject jsonObj) 
	{
		try
		{
			System.out.println("I AM SENDING MY ROUTING INFO TO GATEWAY IN RETURN OF RELAY MESSAGE");
			String gateway_ip = jsonObj.get("gateway_ip").toString();
			
			clientSoc = new Socket(gateway_ip,8767);
			PrintWriter output = new PrintWriter(clientSoc.getOutputStream(),true);
		
			JSONObject routingJson = new JSONObject();
			JSONArray routingInfoArray = new JSONArray();
		
			routingJson.put("type","ROUTING_INFO");
			routingJson.put("gateway_id",jsonObj.get("gateway_id"));
			routingJson.put("node_id",jsonObj.get("node_id"));
			routingJson.put("ip_address",ipAddress);
		
			Iterator it = routingInfo.entrySet().iterator();
			while (it.hasNext()) 
			{
				JSONObject routingEntries = new JSONObject();
				Map.Entry entry = (Map.Entry)it.next();
				routingEntries.put("node_id", entry.getKey());
				routingEntries.put("ip_address", entry.getValue());
				routingInfoArray.add(routingEntries);
			}
			routingJson.put("route_table",routingInfoArray);
			System.out.println(routingJson);
			output.println(routingJson);
		}
		catch(Exception e)
		{
			
		}
	}	
	
	//method to display routing info received
		public void displayRoutingInfo(JSONObject jsonObj) 
		{
			try
			{
				System.out.println("I AM DSIPLAYING THE ROUTING INFO RECEIVED FROM A PEER NODE : I AM GATEWAY");
				System.out.println(jsonObj);
			}
			catch(Exception e)
			{
				
			}
		}
	
	//method to send routing info received from a relay node back to joining node
	public void sendRoutingInfoToJoinNode(JSONObject jsonObj)
	{
		try
		{
			System.out.println("I AM GATEWAY. I AM SENDING THE ROUTING INFO TO JOINING NODE");
			if(routingInfo.containsKey(jsonObj.get("node_id")))
			{
				System.out.println("JOINING NODE IS AVAILABLE IN MY ROUTING TABLE");
				Iterator it = routingInfo.entrySet().iterator();
				while (it.hasNext()) 
				{
					JSONObject routingEntries = new JSONObject();
					Map.Entry entry = (Map.Entry)it.next();
					//routingEntries.put("node_id", entry.getKey());
					//routingEntries.put("ip_address", entry.getValue());
					//routingInfoArray.add(routingEntries);
					if(entry.getKey().equals(jsonObj.get("node_id")))
					{
						sendMessage(entry.getKey().toString(),entry.getValue().toString(),7777,jsonObj);
					}
				}
				
			}
			
		}
		catch(Exception e)
		{
			
		}
	}
	
	//hashcode method
	public int hashCode(String str) 
	{
		  int hash = 0;
		  for (int i = 0; i < str.length(); i++)
		  {
		    hash = hash * 31 + str.charAt(i);
		  }
		  return Math.abs(hash);
		}


	//******************* - PEER CLIENT - ********************
	public class PeerClient implements Runnable
	{
		private Socket socket;
		private PeerClient3 peerModule;
		private int clientId;
		private boolean kill;
	
		PeerClient(Socket socket,PeerClient3 peerModule,int clientId)
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
		
        public void leaveNetwork(JSONObject jsonInput)
        {
        	peerModule.leaveNetwork(jsonInput);
        }
        
        public void chat(JSONObject jsonInput)
        {
        	peerModule.chat(jsonInput);
        }
        
        public void chatRetrieve(JSONObject jsonInput)
        {
        	peerModule.chatRetrieve(socket,jsonInput);
        }
        
        public void ping(JSONObject jsonInput) 
        {
        	peerModule.ping(socket,jsonInput);
        }
        
        public void sendRoutingInfoToGateway(JSONObject jsonInput) 
        {
        	peerModule.sendRoutingInfoToGateway(jsonInput);
			
		}
        
        public void displayRoutingInfo(JSONObject jsonInput) 
        {
        	peerModule.displayRoutingInfo(jsonInput);
			
		}
        
        public void sendRoutingInfoToJoinNode(JSONObject jsonInput)
        {
        	peerModule.sendRoutingInfoToJoinNode(jsonInput);
        }
        
        public void addToRouteTable(JSONObject jsonInput)
        {
        	peerModule.addToRouteTable(jsonInput);
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
					String input = bd.readLine();
					JSONParser jsonInputParser = new JSONParser();
					JSONObject jsonInput  = (JSONObject) jsonInputParser.parse(input);
					String jsonString = jsonInput.toJSONString();
					
					if((jsonString.indexOf("JOINING_NETWORK")!=-1)&&((jsonString.indexOf("RELAY")==-1)))
					{
						//System.out.println(csocket.getRemoteSocketAddress().toString());
						joinNetwork(jsonInput);
					}
					else if(jsonString.indexOf("JOINING_NETWORK_RELAY")!=-1)
					{	
						System.out.println("I GOT YOUR RELAY MESSAGE MY SERVER .. THANK you!");
						sendRoutingInfoToGateway(jsonInput);
						//sendToNextSystemInTheLoop
						sendRelay(jsonInput);
					}
					else if(jsonString.indexOf("LEAVING_NETWORK")!=-1)
					{	
						leaveNetwork(jsonInput);
										                   
					}
					else if((jsonString.indexOf("CHAT")!=-1)&&((jsonString.indexOf("RETRIEVE")==-1)))
					{
						chat(jsonInput);
					}
					else if(jsonString.indexOf("CHAT_RETRIEVE")!=-1)
					{
						chatRetrieve(jsonInput);
					}
					else if(jsonString.indexOf("PING")!=-1)
					{
						ping(jsonInput);
					}
					else if(jsonString.indexOf("ROUTING_INFO")!=-1)
					{
						if(nodeId.equals(jsonInput.get("gateway_id")))
						{
							displayRoutingInfo(jsonInput);
							sendRoutingInfoToJoinNode(jsonInput);
						}
						else if(nodeId.equals(jsonInput.get("node_id")))
						{
							System.out.println("I am TARGET . I AM GOING TO UPDATE MY ROUTING TABLE WITH THE INFO");
							addToRouteTable(jsonInput);
						}
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
	//******************* - PEER CLIENT - END ********************
	
	//######################## - THIS NODE - ########################
	public static class ThisPeer implements Runnable
	{
		static PeerClient3 thisPeerClient3;
				
		ThisPeer(PeerClient3 peerModule)
		{
			this.thisPeerClient3 = peerModule;
		}
		
		private void joinNetwork(JSONObject jsonObj,String boot_ip) 
		{
			System.out.println(jsonObj);
			try
			{
				clientSoc = new Socket(boot_ip,8767);
				BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
				output = new PrintWriter(clientSoc.getOutputStream(),true);
				output.println(jsonObj);
				String messagefromserver = bd.readLine();
				JSONParser jsonInputParser = new JSONParser();
				JSONObject jsonInputFromServer = (JSONObject) jsonInputParser.parse(messagefromserver);
		
				System.out.println("Message from Server to you : "+jsonInputFromServer+" \n");
				
				JSONArray routingInfoArray = (JSONArray) jsonInputFromServer.get("route_table");
				addToRouteTable(routingInfoArray);
			}
			catch(Exception e)
			{
				
			}
		}
		
		private void addToRouteTable(JSONArray routingInfoArray) 
		{
			
			Iterator<JSONObject> routingJsonIterator = routingInfoArray.iterator();
			while(routingJsonIterator.hasNext())
			{
				JSONObject routingJson = routingJsonIterator.next();
				routingInfo.put((String)routingJson.get("node_id"), (String)routingJson.get("ip_address"));
			}
			System.out.println("Current Routing Table in client\n -----------");
			System.out.println(routingInfo);
		}
		
		private void leaveNetwork(JSONObject jsonObj) 
		{
			try
			{
	            System.out.println("In Leave Network of client");
				output = new PrintWriter(clientSoc.getOutputStream(),true);
				output.println(jsonObj);
			}
			catch(Exception e)
			{
				
			}
		}
		
		public void sendMessage(String node, String ipAddress,int port,JSONObject jsonObj)
		{
			thisPeerClient3.sendMessage(node, ipAddress, port, jsonObj);
		}
		private void chat(JSONObject jsonObj) 
		{
			thisPeerClient3.chat(jsonObj);
			
		}
		
		private void chatRetrieve(JSONObject jsonObj) 
		{
			try
			{
				System.out.println("In Chat Retrieve of client");
				BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
				output = new PrintWriter(clientSoc.getOutputStream(),true);
				output.println(jsonObj);
				
				String messagefromserver = bd.readLine();
				
				JSONParser jsonInputParser = new JSONParser();
				JSONObject jsonInputFromServer = (JSONObject) jsonInputParser.parse(messagefromserver);
		
				System.out.println("Message from Server to you for CHAT RETRIEVE: "+jsonInputFromServer+" \n");
			}
			catch(Exception e)
			{
				
			}
		}
		
		private void ping(JSONObject jsonObj) 
		{
			try
			{
				System.out.println("In ping of client");
				BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
				
				output = new PrintWriter(clientSoc.getOutputStream(),true);
				output.println(jsonObj);
				
				String messagefromserver = bd.readLine();
				
				JSONParser jsonInputParser = new JSONParser();
				JSONObject jsonInputFromServer = (JSONObject) jsonInputParser.parse(messagefromserver);
		
				System.out.println("Message from Server to you for PING: "+jsonInputFromServer+" \n");
			}
			catch(Exception e)
			{
				
			}
		}	
					
		public void run()
		{
			try
			{
				String tag = null;
				String text = null;
				String boot_ip = null;
				String targetNodeId = null;
				ipAddress = InetAddress.getLocalHost().getHostAddress().toString();
				
				//BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
				while(true)
				{
					
					System.out.println("Enter your message to the server : ");
					System.out.println("1. JOIN NETWORK");
					System.out.println("2. CHAT");
					System.out.println("3. CHAT RETRIEVAL");
					System.out.println("4. PING");
					System.out.println("5. LEAVE NETWORK");
		        	InputStreamReader rd = new InputStreamReader(System.in);
					BufferedReader br = new BufferedReader(rd);
					
					int temp = Integer.parseInt(br.readLine());
					System.out.println(temp);
					JSONObject jsonobj = new JSONObject();
					switch(temp)
					{
						case 1:
							System.out.println("JOIN NETWORK\n---------");
							System.out.println("Enter IP of Bootstrap");
							boot_ip = br.readLine();
							jsonobj.put("type","JOINING_NETWORK");
							jsonobj.put("node_id",nodeId);
							jsonobj.put("ip_address", ipAddress);
							joinNetwork(jsonobj,boot_ip);
							break;
						case 2:
							System.out.println("CHAT\n---------");
							System.out.println("Enter TAG : ");
							tag = br.readLine();
							System.out.println("Enter TEXT : ");
							text = br.readLine();
							System.out.println("Enter TARGET NODE ID : ");
							targetNodeId = br.readLine();
							jsonobj.put("type","CHAT");
							jsonobj.put("tag",tag);
							jsonobj.put("text",text);
							jsonobj.put("target_id",targetNodeId);
							jsonobj.put("sender_id",nodeId);
							chat(jsonobj);
							break;
						case 3:
							System.out.println("CHAT RETRIEVAL\n---------");
							jsonobj.put("type","CHAT_RETRIEVE");
							System.out.println("Enter TAG to be retrieved: ");
							tag = br.readLine();
							jsonobj.put("tag",tag);
							jsonobj.put("node_id",nodeId);
							jsonobj.put("sender_id",nodeId);
							chatRetrieve(jsonobj);
							break;
						case 4:
							System.out.println("PING\n---------");
							jsonobj.put("type","PING");
							jsonobj.put("target_id",nodeId);
							jsonobj.put("sender_id",nodeId);
							jsonobj.put("ip_address",ipAddress);
							ping(jsonobj);
							break;
						case 5:			
							System.out.println("LEAVE NETWORK\n---------");
							jsonobj.put("type","LEAVING_NETWORK");
							jsonobj.put("node_id",nodeId);
							leaveNetwork(jsonobj);
							break;
						default:
							System.out.println("Enter from 1 - 5\n---------");
							break;
					}
				}
			}
			catch(Exception e)
			{
				System.out.println(e + "Exception in client 1 message" );
			}
		}
	}
	//######################## - THIS NODE - END ########################
	
	public static void main(String[] args) throws Exception
	{	
		//This node
		PeerClient3 pm = new PeerClient3(new ServerSocket(9999));	
		ThisPeer tp = new ThisPeer(pm);
		(new Thread(tp)).start();
		pm.init();		   
	}

	

}
