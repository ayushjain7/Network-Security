//Name : Ayush Jain
//Uni : aj2672

Programming Assignment 1

Files:
Client.java 	  : Client code. Takes 4 arguments in the following order
					endpoint, port, keyStoreFile, password(for keystore file , "password" for the certificate attached)
					Example invocation :  java Client localhost, 4180, client.jks, password
Server.java		  : Server code. Takes 3 arguments in the following order
					port, server keyStore file, password(for keystore file , "password" for the certificate attached)
					Example invocation :  java Server 4180 server.jks password
client.jks : client's keystore file used for testing
server.jks : server's keystore file used for testing

Generating keystore:
    Server:
        1) keytool -genkey -alias server -keyalg RSA -keystore server.jks
        2) keytool -export -file server.cert -keystore server.jks -storepass password -alias server
        3) keytool -import -file client.cert -keystore server.jks -storepass password -alias client

    Client:
        1) keytool -genkey -alias client -keyalg RSA -keystore client.jks
        2) keytool -export -file client.cert -keystore client.jks -storepass password -alias client
        3) keytool -import -file server.cert -keystore client.jks -storepass ruchira -alias server

How to Run:
1) To Compile:
	Run javac on all java files.
	For e.g. 	
		javac Client.java
		javac Server.java

2) To startup the Server
	java Server 4180 server.jks password

4) To startup the Client
	java Client localhost 4180 server.jks password

Error Messages:
1) Client:
	a) When parameters are not proper
	    "Wrong number of arguments. Exiting" or "Missing parameters,..."
	b) port entered not a proper integer
		"Port is not an integer. Exiting."
	c) key store file do not exist
        "Keystore file name entered does not exist. Exiting"

2) Server:
	a) When parameters are not proper
	    "Wrong number of arguments. Exiting" or "Missing parameters,..."
	b) port entered not a proper integer
		"Port is not an integer. Exiting."
	c) key store file do not exist
	    "Keystore file name entered does not exist. Exiting"
	d) Port not available or free
		"Exception caught when trying to listen on port <portNumber> or listening for a connection"

Special Points:
1) Contains try-with-resources released as part of JAVA 7. Did not compile on hanoi.clic machine(it is still using 1.6 compiler), 
	but worked on taipei.clic and havana.clic.
