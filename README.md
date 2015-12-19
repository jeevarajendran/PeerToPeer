# PeerToPeer

Student Name : Jeeva Rajendran
Project : Peer to Peer 

Project Description:

	This project implements peer to peer communication (decentralized node to node interaction).
	Functionalities:
		-	Joining        : A peer node can join to the network through bootstrap node or any other peer node in the network.  BootStrap node ip address is taken as input
		- 	Chat           : A chat message along with tag can be indexed in a node. The tag will be converted into a node id and message passes from one node to another using the pastry routing algorithm
		- 	Chat retrieval : A node can retrieve message indexed to a tag from a peer node following the overlay path
		- 	Chat response  : A peer node can send the chat response to the requester through the overlay network path
		- 	Ping           : A node can ping a suspected dead node to ensure the path existence  
		- 	Leave          : A node can leave the network by sending leaving messages to the nodes in the routing table
	Routing algorithm:
		           This project follows the basic routing algorithm of Pastry in which , the message is passed from one node to the next numerically nearest node towards the target node
	Routing table    : (Node Id , Ip Address)
	Chat Message structure : (Tag , Text)
 

Class Files:

	-> Peer Module  : The main class that implements the major functionalities . It runs two threads : one to address the requests from other peer nodes and the second one to frame and send requests to other 
			  peer nodes. This class maintaing the routing table and chat message pool of the current node
	-> Peer Client (in line class) : The thread class to accept the incoming peer client connection , read the message from them and invoke appropriate functions in the main class . It acts like an interface for the other peer nodes
	-> ThisPeer    (in line class) : Current Node . This is a thread class to accept inputs from the current node user , frame request messages and call appropriate functions in the main class

Programming Language : Java

Message format : JSON Objects

Jar files : Apache-jakarta-commons-collections, json-simple-1.1

 


