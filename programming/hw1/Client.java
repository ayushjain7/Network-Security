//Name : Ayush Jain
//UNI : aj2672

import java.io.*;
import java.net.*;
import javax.crypto.*;
import java.security.*;
import javax.crypto.spec.*;

public class Client {
	/**
	* Encrypts password using public Key.
	* @param text 
	*		text to be encrypted passed in byte[] format
	* @param keyFile
	*		the name of the file containing server's private key to be used for encryption
	* @return
	*		byte[] of the encrypted password
	* @throws
	*		FileNotFoundException, if the file does not exist.
	* 		IOException, if unable to read file.
	*		IllegalBlockSizeException, if the text size is greater than 256 bytes[]
	*		InvalidKeyException, 		
	*/
	private static byte[] encrypt(byte[] text, String keyFile) throws Exception{
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(keyFile));
		PublicKey key = (PublicKey) inputStream.readObject();
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(text);
	}

	
	public static void main(String[] args) throws Exception {
		if (args.length != 7){
			System.err.println("Usage: java Client <password> <fileName> <host name> <port number> <private key> <public key> <server public key>");
			System.exit(1);
		}

		String password = args[0];
		String fileName = args[1];
		String hostName = args[2];
		if(!(new File(fileName).exists())){
			System.err.println("File name entered to be encoded does not exist. Exiting");
			System.exit(1);
		}

		int portNumber = 0;
		try{
			portNumber = Integer.parseInt(args[3]);
		}catch(Exception e){
			System.err.println("Port is not an integer. Exiting.");
			System.exit(1);
		}

		String privKeyFile = args[4];
		String pubKeyFile = args[5];
		String serverPubKeyFile = args[6]; 
		if(!(new File(privKeyFile).exists()) || !(new File(pubKeyFile).exists()) ||!(new File(serverPubKeyFile).exists())){
			System.err.println("One or more key files do not exist. Exiting");
			System.exit(1);
		}
			
		try (
				Socket socket = new Socket(hostName, portNumber);
				PrintStream out = new PrintStream(socket.getOutputStream());
				BufferedReader in =	new BufferedReader(new InputStreamReader(socket.getInputStream()));
			) {
			//Send encrypted password
			out.write(encrypt(password.getBytes(), serverPubKeyFile));
			//Sending signature
			byte[] signature = sign(fileName, privKeyFile);
			out.write(signature);
			//Initializing IV 
			byte[] iv = new byte[16];	
			SecureRandom prng = new SecureRandom();
			prng.nextBytes(iv);
			//Sending IV
			out.write(iv);
			//Sending File
			encryptFile(socket.getOutputStream(), password, fileName, iv);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostName);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + e + 
					hostName);
			System.exit(1);
		} 
	}

	private static byte[] sign(String fileName, String privKeyFile) throws Exception{
	/**
	* signs the contents of a file using SHA-256 hash algorithm and RSA encryption
	* @param fileName
	*		String name of file whose contents are to be signed
	* @param privKeyFile
	*		String name of file containing the private key used to sign the contents
	* @return 
	*		byte[] of signature	
	*/
		//Opening stream to the file to read contents.
		FileInputStream fis = new FileInputStream(fileName);
		//Read the private key from file.
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(privKeyFile));
		PrivateKey key = (PrivateKey) inputStream.readObject();
		//Buffered Stream to file since it is efficient.
		BufferedInputStream bufin = new BufferedInputStream(fis);
		
		//Instantiationg signature.
		Signature mySign = Signature.getInstance("SHA256withRSA");
		mySign.initSign(key);
		byte[] dataBytes = new byte[1024];
		int nread; 
		while ((nread = bufin.read(dataBytes)) >= 0) {
			mySign.update(dataBytes, 0, nread);
		}
		bufin.close();
		return mySign.sign();
	}

	private static void encryptFile(OutputStream stream, String key, String fileName, byte[] iv) throws Exception{
	/**
	* 	encrypts a file using AES/CBC mode with PKCS5 padding and writes it to socket outputStream.
	*	@param stream
	*		Socket output stream
	*	@param fileName
	*		String name of the file to be encrypted and senti
	*	@param key
	*		password used for encryption
	*	@param iv
	*		Initialization vectore used for encryption
	*/
		//Instantiating cipher
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"), new IvParameterSpec(iv));
		//Cipher stream from client to server
		CipherOutputStream cipherOut = new CipherOutputStream(stream, cipher);
		byte[] fileBuffer = new byte[1024];
		//File stream to read contents of file.
		InputStream fileReader = new BufferedInputStream(new FileInputStream(fileName));
		int bytesRead;
		while((bytesRead = fileReader.read(fileBuffer)) != -1){
			cipherOut.write(fileBuffer, 0, bytesRead);
		}
		cipherOut.flush();
		cipherOut.close();
		fileReader.close();
	}
}
