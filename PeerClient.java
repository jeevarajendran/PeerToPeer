package peertopeer;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
    /**
     *
      * @author jeevarajendran
       */


public class PeerClient {
	
	static String nodeId = "1002";
	static Socket clientSoc = null;
	static PrintWriter output = null;
	
	static HashMap<String,String> routingInfo = new HashMap<String,String>();
	
	public static void main(String[] args) 
	{	        
	
		try
		{
			
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
						System.out.println("Enter the ip address of the bootstrap node");
						String boot_ip = br.readLine();
						jsonobj.put("type","JOINING_NETWORK");
						jsonobj.put("node_id",nodeId);
						jsonobj.put("ip_address","134.226.58.160");
						joinNetwork(jsonobj,boot_ip);
						
						break;
					case 2:
						System.out.println("CHAT\n---------");
						jsonobj.put("type","CHAT");
						chat(jsonobj);
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
						jsonobj.put("type","LEAVING_NETWORK");
						jsonobj.put("node_id",nodeId);
						leaveNetwork(jsonobj);
						break;
					default:
						System.out.println("Default\n---------");
						break;
				}
				
				
				
			}
		}
		catch(Exception e)
		{
			System.out.println(e + "Exception in client 1 message" );
		}
	}

	private static void addToRouteTable(JSONArray routingInfoArray) {
		// TODO Auto-generated method stub
		Iterator<JSONObject> routingJsonIterator = routingInfoArray.iterator();
		while(routingJsonIterator.hasNext())
		{
			JSONObject routingJson = routingJsonIterator.next();
			routingInfo.put((String)routingJson.get("node_id"), (String)routingJson.get("ip_address"));
		}
		System.out.println("Current Routing Table in client\n -----------");
		System.out.println(routingInfo);
	}
	
	private static void leaveNetwork(JSONObject jsonobj) {
		// TODO Auto-generated method stub
		
	}

	private static void ping() {
		// TODO Auto-generated method stub
		
	}

	private static void chatRetrieval() {
		// TODO Auto-generated method stub
		
	}

	private static void chat(JSONObject jsonObj) {
		// TODO Auto-generated method stub
		
		System.out.println(jsonObj);
	}

	private static void joinNetwork(JSONObject jsonObj,String boot_ip) {
		// TODO Auto-generated method stub
		System.out.println(jsonObj);
		try
		{
			
			clientSoc = new Socket(boot_ip,8767);
			BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
			output = new PrintWriter(clientSoc.getOutputStream(),true);
			output.println(jsonObj);
		
			System.out.println("waiting for the server to write :");

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

}
