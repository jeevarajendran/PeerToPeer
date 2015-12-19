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

public class PeerModule 
{
	
	private ServerSocket serverSocket;
	private static final ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
	private static int peerId=0;
	private static String nodeId = "448";
	private static String ipAddress = null;
	private static Socket clientSoc = null;
	private static PrintWriter output = null;
	PeerClient peerClient = null;

	static HashMap<String,String> routingInfo = new HashMap<String,String>();
	MultiValueMap chatMessages = new MultiValueMap();
	
	
	//constructor to initialize server socket / current system socket
	public PeerModule(ServerSocket serverSocket)
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
					PeerModule.pool.execute(peerClient);
				}
				
			}						                       
		}
		catch(Exception e)
		{
			System.out.println("Exception in init method"+e);
		}	
	}
	
	//method to find the closest node
	private String findClosestNode(int nodeChecked) 
	{
		int intialDiff = Math.abs(Integer.parseInt(nodeId) - nodeChecked);
		int nextNode = 0;
		int closest = Integer.parseInt(nodeId);
		int diff = 0;
		String closestNode = nodeId;
		String closesIp = ipAddress;
		Iterator it = routingInfo.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			nextNode = Integer.parseInt(entry.getKey().toString());
			diff =  Math.abs(nextNode - nodeChecked);
			if((diff<intialDiff))
			{
				closest = nextNode;
				intialDiff = diff;
			}
		}
		System.out.println("CLOSEST NODE : " + closest);
		closestNode = Integer.toString(closest);
		return closestNode;
	}
	
	//method to initialize the initial routing table of this node
	private void initializeRoutingTable() 
	{
		routingInfo.put("448", "192.168.0.16");
		routingInfo.put("8003", "999.999.999.999");
		routingInfo.put("8004", "888.888.888.888");
		routingInfo.put("8005", "777.777.777.777");
	}

	//method to join a peer node to the network
	public void handleJoinRequest(JSONObject jsonInput)
	{
		if(routingInfo.containsKey(jsonInput.get("node_id")))
		{
			System.out.println("YOU ARE ALREADY IN THE NETWORK");
			sendRoutingInfo(jsonInput);
		}
		else
		{
			sendRoutingInfo(jsonInput);
			sendRelay(jsonInput);
			addToRouteTable(jsonInput);
		}
	}
        
    //method to join this node to the network
	public void joinNetwork(JSONObject jsonInput,String boot_ip)
	{
        	sendMessage(boot_ip,jsonInput);
	}
	
	//method to send routing info to a joining node
	public void sendRoutingInfo(JSONObject jsonInput)
	{
		try
		{
			System.out.println("SENDING ROUTINGINFO");
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
			System.out.println("IP ADDRESS of Peer"+peerClient.socket.getInetAddress().toString().substring(1));
            sendMessage(peerClient.socket.getInetAddress().toString().substring(1),routingJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendRoutingInfo" + e);
		}
	}
	
	//method to send relay messages about a peer joining to a close node
	private void sendRelay(JSONObject jsonInput) 
	{
		System.out.println("Sending Relay Messages");
		int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());
		String closestNode = findClosestNode(nodeChecked);
		String closestIp = ipAddress;
		if(!closestNode.equals(nodeId))
		{
			closestIp = routingInfo.get(closestNode).toString();
		}
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
			sendMessage(closestIp,jsonObj);
		}
		else
		{
			System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
		}
	}
	
	//method to pass / send the message (GENERIC METHOD)
	private void sendMessage(String ipAddress,JSONObject jsonObj)
	{
		try
		{
			System.out.println("Trying to send messages"+ipAddress);
			clientSoc = new Socket(ipAddress,8767);
			System.out.println("SOCKET CONNECTED");
			BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
			output = new PrintWriter(clientSoc.getOutputStream(),true);
			System.out.println("NOW---------" + jsonObj);
			output.println(jsonObj);
			System.out.println("SENT MESSAGE");
		}
		catch(Exception e)
		{
			System.out.println("Exception in send message" + e);
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

	//method to send leave message to nodes in the routing table 
	public void leaveNetwork(JSONObject jsonInput)
	{
		Iterator it = routingInfo.entrySet().iterator();
		while (it.hasNext()) 
		{
			JSONObject routingEntries = new JSONObject();
	       	Map.Entry entry = (Map.Entry)it.next();
	       	String target_ip = entry.getValue().toString();
	       	if(!target_ip.equals(ipAddress))
	       	{
	       		sendMessage(target_ip,jsonInput);
	       	}
	    }
	}
	
	//method to handle leave network request from a peer node
	public void handleLeaveNetwork(JSONObject jsonInput)
	{
		routingInfo.remove(jsonInput.get("node_id"));
		System.out.println("Routing table after removing the node");
		System.out.println(routingInfo);
	}
	
	//method to index the chat message from the peer
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
				JSONObject chatAckJson = new JSONObject();
				chatAckJson.put("tag", tag);
				chatAckJson.put("node_id", jsonInput.get("sender_id"));
				chatAckJson.put("type","ACK_CHAT");
				sendChatACK(chatAckJson);
			}
			else
			{
				System.out.println(jsonInput);
				int nodeChecked = Integer.parseInt(jsonInput.get("target_id").toString());
				if(routingInfo.containsKey(jsonInput.get("target_id")))
				{
					System.out.println("I have the node to be messaged in my routing table");
					String target_ip = routingInfo.get(jsonInput.get("target_id"));
					sendMessage(target_ip,jsonInput);
				}
				else
				{	
					String closestNode = findClosestNode(nodeChecked);
					if(!closestNode.equals(nodeId))
					{
						String closestIp = routingInfo.get(closestNode).toString();
						sendMessage(closestIp,jsonInput);
					}
					else
					{
						System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in chat function" + e);
		}
	}
	
	//method to send ACK for chat Message index request
	private void sendChatACK(JSONObject jsonInput) 
	{
		System.out.println("CHAT ACKNWLOEDGEMENT");
		int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());
		if(routingInfo.containsKey(jsonInput.get("node_id")))
		{
			System.out.println("I have the node to be ACKED in my routing table");
			String target_ip = routingInfo.get(jsonInput.get("node_id"));
			jsonInput.put("ip_address", ipAddress);
			sendMessage(target_ip,jsonInput);
		}
		else
		{
			System.out.println("Checking for closest Node");
			String closestNode = findClosestNode(nodeChecked);
			if(!closestNode.equals(nodeId))
			{
				String closestIp = routingInfo.get(closestNode).toString();
				jsonInput.put("ip_address", ipAddress);
				sendMessage(closestIp,jsonInput);
			}
			else
			{
				System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
			}
		}
	}
	
	//method to handle ACK message for chat message index
	private void handleChatAck(JSONObject jsonInput) 
	{
		System.out.println("HANDLING CHAT ACK");
		try
		{
			if(jsonInput.get("node_id").equals(nodeId))
			{
				System.out.println("HURRAY .. I GOT THE ACK FOR MY CHAT MESSAGE");
			}
			else
			{
				System.out.println(jsonInput);
				int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());			
				if(routingInfo.containsKey(jsonInput.get("node_id")))
				{
					System.out.println("I have the node to be messaged in my routing table");
					String target_ip = routingInfo.get(jsonInput.get("node_id"));
					sendMessage(target_ip,jsonInput);
				}
				else
				{	
					String closestNode = findClosestNode(nodeChecked);				
					if(!closestNode.equals(nodeId))
					{
						String closestIp = routingInfo.get(closestNode).toString();
						sendMessage(closestIp,jsonInput);
					}
					else
					{
						System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in handleChatAck" + e);
		}
	}

	//method to retrieve the chat message from a peer
	public void chatRetrieve(JSONObject jsonInput)
	{
		try
		{
			if(jsonInput.get("node_id").equals(nodeId))
			{
				formChatResponse(jsonInput);
			}
			else
			{
				System.out.println(jsonInput);
				int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());
				if(routingInfo.containsKey(jsonInput.get("node_id")))
				{
					System.out.println("I have the node to be messaged in my routing table");
					String target_ip = routingInfo.get(jsonInput.get("node_id"));
					sendMessage(target_ip,jsonInput);
				}
				else
				{			
					String closestNode = findClosestNode(nodeChecked);
					if(!closestNode.equals(nodeId))
					{
						String closestIp = routingInfo.get(closestNode).toString();
						sendMessage(closestIp,jsonInput);
					}
					else
					{
						System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in chat retrieve" + e);
		}
	}
	
	//method to form and send chat response
	public void formChatResponse(JSONObject jsonInput)
	{
		try
		{
			String tag = (String) jsonInput.get("tag");
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
			chatResponseJson.put("node_id",jsonInput.get("sender_id"));
			chatResponseJson.put("sender_id",jsonInput.get("node_id"));
			for(int i=0;i<matchedTexts.length;i++)
			{
				JSONObject tempJson = new JSONObject();
				tempJson.put("text", matchedTexts[i]);
				matchedTextsArray.add(tempJson);
			}
			chatResponseJson.put("response",matchedTextsArray);
			System.out.println(chatResponseJson);
			handleChatResponse(chatResponseJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in form chat response function" + e);
		}
	}
	
	//method to handle chat response
	public void handleChatResponse(JSONObject jsonInput)
	{
			try
			{
				if(jsonInput.get("node_id").equals(nodeId))
				{
					System.out.println("HURRAH .. I GOT THE RESPONSE BACK");
					JSONArray textResults = (JSONArray) jsonInput.get("response");
					Iterator textResultsIT = textResults.iterator();
					System.out.println("TEXTS RECEIVED \n-------------");
					while(textResultsIT.hasNext())
					{
						JSONObject textResultJson = (JSONObject) textResultsIT.next();
						System.out.println(textResultJson.get("text"));
					}
				}
				else
				{
					System.out.println(jsonInput);
					int nodeChecked = Integer.parseInt(jsonInput.get("node_id").toString());
					if(routingInfo.containsKey(jsonInput.get("node_id")))
					{
						System.out.println("I have the node to be messaged in my routing table");
						String target_ip = routingInfo.get(jsonInput.get("node_id"));
						sendMessage(target_ip,jsonInput);
					}
					else
					{	
						String closestNode = findClosestNode(nodeChecked);
						if(!closestNode.equals(nodeId))
						{
							String closestIp = routingInfo.get(closestNode).toString();
							sendMessage(closestIp,jsonInput);
						}
						else
						{
							System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
						}
					}
				}
			}
			catch(Exception e)
			{
				System.out.println("Exception in handle chat response"+e);
			}
	}
	
	//method to send routing info to gateway node as a result of relay
	public void sendRoutingInfoToGateway(JSONObject jsonObj) 
	{
		try
		{
			System.out.println("I AM SENDING MY ROUTING INFO TO GATEWAY IN RETURN OF RELAY MESSAGE");
			String gateway_ip = jsonObj.get("gateway_ip").toString();
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
			sendMessage(gateway_ip,routingJson);
		}
		catch(Exception e)
		{
			System.out.println("Exception in sendRoutingInfotoGateway"+e);
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
			System.out.println("Exception"+e);
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
					if(entry.getKey().equals(jsonObj.get("node_id")))
					{
						sendMessage(entry.getValue().toString(),jsonObj);
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception :"+e);
		}
	}
	
	//method to send ping message
	public void ping(JSONObject jsonInput)
	{
		System.out.println(jsonInput);
		String ackIpAddress = jsonInput.get("ip_address").toString();
		String ackJoinNode = jsonInput.get("target_id").toString();
		if(jsonInput.get("target_id").equals(nodeId))
		{
			System.out.println("HURRAY .. I GOT THE PING MESSAGE");
			if(!ackIpAddress.equals(ipAddress))
			{
				sendPingAck(ackIpAddress,ackJoinNode);
			}
		}
		else
		{
			int nodeChecked = Integer.parseInt(jsonInput.get("target_id").toString());					
			if(routingInfo.containsKey(jsonInput.get("target_id")))
			{
				System.out.println("I have the node to be PINGED in my routing table");
				String target_ip = routingInfo.get(jsonInput.get("target_id"));
				jsonInput.put("ip_address", ipAddress);
				sendMessage(target_ip,jsonInput);
				if(!ackIpAddress.equals(ipAddress))
				{
					sendPingAck(ackIpAddress,ackJoinNode);
				}
			}
			else
			{	
				String closestNode = findClosestNode(nodeChecked);				
				if(!closestNode.equals(nodeId))
				{
					String closestIp = routingInfo.get(closestNode).toString();
					jsonInput.put("ip_address", ipAddress);
					sendMessage(closestIp,jsonInput);
					if(!ackIpAddress.equals(ipAddress))
					{
						sendPingAck(ackIpAddress,ackJoinNode);
					}
				}
				else
				{
					System.out.println("I DONT HAVE ANY OTHER CLOSER NODE TO THE TARGET . DONE!!!!");
				}
			}					
		}
	}
	
	//method to send ACK for ping message
	public void sendPingAck(String ackIpAddress,String ackJoinNode) 
	{
		  JSONObject pingAckJson = new JSONObject();
		  pingAckJson.put("type","ACK");
		  pingAckJson.put("node_id",ackJoinNode);
		  pingAckJson.put("ip_address",ackIpAddress);
		  sendMessage(ackIpAddress,pingAckJson);
	}
	
	//hashcode method
	public static int hashCode(String str) 
	{
		  int hash = 0;
		  for (int i = 0; i < str.length(); i++)
		  {
		    hash = hash * 1 + str.charAt(i);
		  }
		  return Math.abs(hash);
	}


	//******************* - PEER CLIENT - ********************
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

		public void handleJoinRequest(JSONObject jsonInput)
		{
			peerModule.handleJoinRequest(jsonInput);
		}
		
        public void leaveNetwork(JSONObject jsonInput)
        {
        	peerModule.leaveNetwork(jsonInput);
        }
        
        public void handleLeaveNetwork(JSONObject jsonInput)
        {
        	peerModule.handleLeaveNetwork(jsonInput);
        }
        
        public void chat(JSONObject jsonInput)
        {
        	peerModule.chat(jsonInput);
        }
        
        public void handleChatAck(JSONObject jsonInput)
        {
        	peerModule.handleChatAck(jsonInput);
        }
        
        
        public void chatRetrieve(JSONObject jsonInput)
        {
        	peerModule.chatRetrieve(jsonInput);
        }
        
        public void ping(JSONObject jsonInput) 
        {
        	peerModule.ping(jsonInput);
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
						handleJoinRequest(jsonInput);
					}
					else if(jsonString.indexOf("JOINING_NETWORK_RELAY")!=-1)
					{	
						System.out.println("I GOT YOUR RELAY MESSAGE MY SERVER .. THANK you!");
						sendRoutingInfoToGateway(jsonInput);
						sendRelay(jsonInput);
					}
					else if(jsonString.indexOf("LEAVING_NETWORK")!=-1)
					{	
						handleLeaveNetwork(jsonInput);
										                   
					}
					else if((jsonString.indexOf("CHAT")!=-1)&&((jsonString.indexOf("RETRIEVE")==-1))&&((jsonString.indexOf("RESPONSE")==-1))&&((jsonString.indexOf("ACK")==-1)))
					{
						chat(jsonInput);
					}
					else if(jsonString.indexOf("ACK_CHAT")!=-1)
					{
						System.out.println("InSIDE Handle CHAT ACK");
						handleChatAck(jsonInput);
					}
					else if(jsonString.indexOf("CHAT_RETRIEVE")!=-1)
					{
						chatRetrieve(jsonInput);
					}
					else if(jsonString.indexOf("CHAT_RESPONSE")!=-1)
					{
						System.out.println("In CHAT Response +++++++++++++++++");
						handleChatResponse(jsonInput);
					}
					else if(jsonString.indexOf("PING")!=-1)
					{
						ping(jsonInput);
					}
					else if(jsonString.indexOf("ACK")!=-1)
					{
						System.out.println("I got the ACK for the PING message");
					}
					else if(jsonString.indexOf("ROUTING_INFO")!=-1)
					{
						System.out.println("*RUTING INFO**");
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
		static PeerModule thisPeerModule;
				
		ThisPeer(PeerModule peerModule)
		{
			this.thisPeerModule = peerModule;
		}
		
		private void joinNetwork(JSONObject jsonObj,String boot_ip) 
		{	
			thisPeerModule.joinNetwork(jsonObj,boot_ip);
		}
		
		private void leaveNetwork(JSONObject jsonObj) 
		{
			thisPeerModule.leaveNetwork(jsonObj);
		}
		
		public void sendMessage(String ipAddress,int port,JSONObject jsonObj)
		{
			thisPeerModule.sendMessage(ipAddress, jsonObj);
		}
		private void chat(JSONObject jsonObj) 
		{
			thisPeerModule.chat(jsonObj);	
		}
		
		private void chatRetrieve(JSONObject jsonObj) 
		{
			thisPeerModule.chatRetrieve(jsonObj);
		}
		
		private void ping(JSONObject jsonObj) 
		{
			thisPeerModule.ping(jsonObj);
		}	
		
		private int hashCode(String str)
		{
			return thisPeerModule.hashCode(str);
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
							/*System.out.println("Enter TARGET NODE ID : ");
							targetNodeId = br.readLine();*/
							targetNodeId = Integer.toString(hashCode(tag));
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
							/*System.out.println("Enter TARGET NODE ID : ");
							targetNodeId = br.readLine();*/
							targetNodeId = Integer.toString(hashCode(tag));
							jsonobj.put("tag",tag);
							jsonobj.put("node_id",targetNodeId);
							jsonobj.put("sender_id",nodeId);
							chatRetrieve(jsonobj);
							break;
						case 4:
							System.out.println("PING\n---------");
							System.out.println("Enter TARGET NODE ID : ");
							targetNodeId = br.readLine();
							jsonobj.put("type","PING");
							jsonobj.put("target_id",targetNodeId);
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
		PeerModule pm = new PeerModule(new ServerSocket(8767));	
		ThisPeer tp = new ThisPeer(pm);
		(new Thread(tp)).start();
		pm.init();
	}

}
