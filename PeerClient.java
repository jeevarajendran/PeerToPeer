/*
 * To change this license header, choose License Headers in Project Properties.
  * To change this template file, choose Tools | Templates
   * and open the template in the editor.
    */
    

    import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;

import org.json.JSONObject;
    /**
     *
      * @author jeevarajendran
       */

public class PeerClient {
	
	static int nodeId = 1002;
	static Socket clientSoc = null;
	static PrintWriter output = null;
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
						jsonobj.put("ip_address",boot_ip);
						joinNetwork(jsonobj);
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
						leaveNetwork();
						break;
					default:
						System.out.println("Default\n---------");
						break;
				}
				
				
				/*String messagefromserver = null;
				char[] checkpoint = new char[100];
				int i=0;
				int charread;
				charread=bd.read();
			
				while(charread != 13)
				{
					checkpoint[i] = (char)charread;
					charread=bd.read();
					i++;
				}

				messagefromserver = new String(checkpoint);
				System.out.println("Message from Server to you : "+messagefromserver+" \n");*/
			}
		}
		catch(Exception e)
		{
			System.out.println(e + "Exception in client 1 message" );
		}
	}

	private static void leaveNetwork() {
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

	private static void joinNetwork(JSONObject jsonObj) {
		// TODO Auto-generated method stub
		System.out.println(jsonObj);
		try
		{
		clientSoc = new Socket(jsonObj.getString("ip_address"),8767);
		output = new PrintWriter(clientSoc.getOutputStream(),true);
		output.println(jsonObj);
		}
		catch(Exception e)
		{
			
		}
		
	}

}
