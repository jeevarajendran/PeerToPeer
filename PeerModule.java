/*
 * To change this license header, choose License Headers in Project Properties.
  * To change this template file, choose Tools | Templates
   * and open the template in the editor.
    */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.*;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

    /**
     *
      * @author jeevarajendran
       */
		public class PeerModule
		{
			private ServerSocket serverSocket;
			private static final ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
			private int clientId=0;
			private int chatRoomNo=1000;
		
			List<ChatRoom> chatRoomList = new ArrayList<ChatRoom>();
			List<ClientThread> clientLists = new ArrayList<ClientThread>();
			
			//constructor to initialize server socket
			public ServerSoc(ServerSocket serverSocket)
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
							System.out.println("Waiting for a client to get connected \n-------------------------------------");													Socket socket = serverSocket.accept();
							clientId++;
							System.out.println("Connected to a client\n-------------------------------------");
							ClientThread client = new ClientThread(socket,this,clientId);
							clientLists.add(client);
							ServerSoc.pool.execute(client);
						}
						
					}						                       
				}
				catch(Exception e)
				{
					System.out.println(e);
				}	
			}
			
			
			//method to join the network
			public void joinNetwork()
			{
			
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
			public void chatRetrieve()
			{
			
			}
			
			//method to ping
			public void ping()
			{
			
			}
	
			//peer clients
			public class PeerClient implements Runnable
			{
				private Socket socket;
				private ServerSoc serverSoc;
				private boolean kill;
				private int clientId;
				private String clientName;
			
				PeerClient()
				{
					this.socket=socket;
					this.serverSoc=serverSoc;
					this.kill = false;
					this.clientId=clientId;
				}

				//method to join the network
				public void joinNetwork()
				{
							
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
				public void chatRetrieve()
				{
			
				}
			
				//method to ping
				public void ping()
				{
			
				}
			
				@Override			
				public void run()
				{
					try
					{
						BufferedReader bd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
						while(!kill)
						{
							List<String> inputStrings= new ArrayList<String>();
							String temp = "";

							while(bd.ready())
							{
								inputStrings.add(bd.readLine());
							}
							ListIterator ilstring = inputStrings.listIterator();
							while(ilstring.hasNext())
							{
								temp=temp+(String)(ilstring.next());
							}

							//conditions to check the incoming message from client and route accordingly
							if(temp.equals("JOINING_NETWORK"))
							{
								String messagetoclient = "HELO BASE_TEST";
								messagetoclient = messagetoclient + "\nIP:134.226.58.160\nPort:7777\nStudentID:15310693";
								output.println(messagetoclient);
							}
							else if(temp.startsWith("JOINING_NETWORK")==true)
							{	
								String chatRoomName = ((String)(inputStrings.get(0))).split(":")[1];
								this.clientName =((String)(inputStrings.get(3))).split(":")[1];
								joinChatRoom(chatRoomName,this);
												                   
							}
							else if(temp.startsWith("LEAVE_NETWORK")==true)
							{
								int chatRoomId =Integer.parseInt( (((String)(inputStrings.get(0))).split(":")[1]).trim());
								String message =(((String)(inputStrings.get(3))).split(":")[1].trim());
								chat(chatRoomId,this,message+"\n\n");
							}
							else if(temp.startsWith("ROUNTING_INFO")==true)
							{
								int chatRoomId =Integer.parseInt( (((String)(inputStrings.get(0))).split(":")[1]).trim());
								leaveChatRoom(chatRoomId,this);
							}
							else if(temp.startsWith("CHAT")==true)
							{
								leaveAllChatRooms(this);
								socket.close();
							}
							else if(temp.equals("PINK"))
							{

								kill=true;
								socket.close();
								killService();
						
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
			
			public class ChatRoom
			{
			
				
			}
			
			public static void main(String[] args) throws Exception
			{				   
				ServerSoc serSoc = new ServerSoc(new ServerSocket(7777));
				serSoc.initializeChatRooms();
				serSoc.viewChatRooms();
				serSoc.start();
			}																														
		}