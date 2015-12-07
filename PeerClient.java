/*
 * To change this license header, choose License Headers in Project Properties.
  * To change this template file, choose Tools | Templates
   * and open the template in the editor.
    */
    

    import java.util.*;
    import java.lang.*;
    import java.io.*;
    import java.net.*;
    /**
     *
      * @author jeevarajendran
       */
		public class ClientSoc 
		{
			public static void main(String[] args) 
			{
	              
				Socket clientSoc = null;
				PrintWriter output = null;
					        
				try
				{
					clientSoc = new Socket("localhost",7777);
				}
				catch(Exception e)
				{
					System.out.println(e + "Exception in client 1 connection");
				}
			  
				try
				{
					output = new PrintWriter(clientSoc.getOutputStream(),true);
					BufferedReader bd = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
					while(true)
					{
						System.out.println("Enter your message to the server : ");
			        		InputStreamReader rd = new InputStreamReader(System.in);
						BufferedReader br = new BufferedReader(rd);
						
						String temp = br.readLine();
						output.println(temp);
						
						String messagefromserver = null;
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
						System.out.println("Message from Server to you : "+messagefromserver+" \n");
					}
				}
				catch(Exception e)
				{
					System.out.println(e + "Exception in client 1 message" );
				}
			}  
		}