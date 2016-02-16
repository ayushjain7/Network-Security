//Name : Ayush Jain
//Uni : aj2672

Programming Assignment 1

Files:
KeyGenerator.java : Generates a pair of RSA keys and stores them into the files passed as arguments.
					The file names are with respect to the current directory.
					Example call : java KeyGenerator private.key public.key
Client.java 	  : Client code. Takes 6 arguments in the following order
					password(used as key for encryption), fileName(to be encrypted), endpoint, port, client's public key file, 
							client's private key file, server's public key file
					Example invocation :  java Client password12345678 file.txt localhost 4180 clientPrivate.key clientPublic.key serverPublic.key
Server.java		  : Server code. Takes 6 arguments in the following order
					port, public key file, private key file, client's public key file
					Example invocation :  java Server 4180 u serverPrivate.key serverPublic.key clientPublic.key
serverPrivate.key : server's private key used for testing
serverPrivate.key : server's private key used for testing
clientPrivate.key : client's private key used for testing
serverPrivate.key : server's private key used for testing

How to Run:
1) To Compile:
	Run javac on all java files.
	For e.g. 	
		javac KeyGenerator.java
		javac Client.java
		javac Server.java

2) To generate RSA keys
	java KeyGenerator serverPrivate.key serverPublic.key
	java KeyGenerator clientPrivate.key clientPublic.key

3) To startup the Server
	java Server 4180 u serverPrivate.key serverPublic.key clientPublic.key

4) To startup the Client
	java Client asdfghjklzxcvbnm file.txt localhost 4180 clientPrivate.key clientPublic.key serverPublic.key

Error Messages:
1) Client:
	a) When parameters are not proper
		"Usage: java Client <password> <fileName> <host name> <port number> <private key> <public key> <server public key>"
	b) port entered not a proper integer
		"Port is not an integer. Exiting."
	c) file to be encrypted does not exist
		"File name entered to be encoded does not exist. Exiting"
	d) any of the key files do not exist
		"One or more key files do not exist. Exiting"

2) Server:
	a) When parameters are not proper
		"Usage: java Server <port number> <mode> <private key> <public key> <client's public key>
	b) port entered not a proper integer
		"Port is not an integer. Exiting."
	c) Mode entered is wrong
		"Mode can be either t or u. Incorrect value for mode. Exiting."
	d) any of the key files do not exist
		"One or more key files do not exist. Exiting"
	e) Port not available or free
		"Exception caught when trying to listen on port <portNumber> or listening for a connection"

Special Points:
1) Contains try-with-resources released as part of JAVA 7. Did not compile on hanoi.clic machine(it is still using 1.6 compiler), 
	but worked on taipei.clic and havana.clic.
