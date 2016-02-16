//Name : Ayush Jain
//UNI : aj2672

import java.net.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

public class Server {

	private static byte[] decryptPassword(byte[] text, String keyFile) throws Exception{
		/** 
		 * Decrypts password word using public Key.
		 * @param text 
		 *       text to be decrypted passed in byte[] format
		 * @param keyFile
		 *       the name of the file containing own public key to be used for encryption
		 * @return
		 *       byte[] of the decrypted password
		 * @throws
		 *       FileNotFoundException, if the file does not exist.
		 *       IOException, if unable to read file.
		 *       IllegalBlockSizeException, if the text size is greater than 256 bytes[]
		 *       InvalidKeyException,        
		 */
		//Input stream from key file.
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(keyFile));
		PrivateKey privateKey = (PrivateKey) inputStream.readObject();
		//Initializing cipher
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decryptedText = cipher.doFinal(text);
		return decryptedText;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.err.println("Usage: java Server <port number> <mode> <private key> <public key> <client's public key>");
			System.exit(1);
		}

		int portNumber = 0;
		try{
			portNumber = Integer.parseInt(args[0]);
		}catch(Exception e){
			System.err.println("Port is not an integer. Exiting." + e);
			System.exit(1);
		}

		String mode = args[1];
		if(!"t".equals(mode) && !"u".equals(mode)){
			System.err.println("Mode can be either t or u. Incorrect value for mode. Exiting.");
			System.exit(1);
		}
		String privKeyFile = args[2];
		String pubKeyFile = args[3];
		String clientPubKeyFile = args[4];
		if(!(new File(privKeyFile).exists()) || !(new File(pubKeyFile).exists()) ||!(new File(clientPubKeyFile).exists())){
			System.err.println("One or more key files do not exist. Exiting");
			System.exit(1);
		}

		try (
				ServerSocket serverSocket =	new ServerSocket(Integer.parseInt(args[0]));
				Socket clientSocket = serverSocket.accept();     
			) {
			//Receiving password
			byte[] encrypted = new byte[256];
			int num1 = clientSocket.getInputStream().read(encrypted, 0, 256);
			String password = new String(decryptPassword(encrypted, privKeyFile));
			//Receiving signature of the file
			byte[] signature = new byte[256];
			int num2 = clientSocket.getInputStream().read(signature, 0, 256);
			//Receiving 16 byte IV
			byte[] iv = new byte[16];
			int num3 = clientSocket.getInputStream().read(iv, 0,16);
			//Receiving and decrypting file
			decryptFile(password, clientSocket.getInputStream(), iv);
			//Comparing the signature with the file
			verify(mode, signature, clientPubKeyFile);
		} catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port "
					+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
	}


	private static void decryptFile(String key, InputStream stream, byte[] iv) throws Exception{
		/**
		 * 	decrypts a file using AES/CBC mode with PKCS5 padding and writes it to a file name decryptedFile.
		 *	@param stream
		 *		Socket input stream
		 *	@param key
		 *		password used for encryption/decryption
		 *	@param iv
		 *		Initialization vector used for encryption
		 */
		//Initializing cipher to the decrypt
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"), new IvParameterSpec(iv));
		//Cipher stream from the client
		CipherInputStream cipherIn = new CipherInputStream(stream, cipher);
		byte[] fileBuffer = new byte[1024];
		//Stream to a file
		BufferedOutputStream fileWriter = new BufferedOutputStream(new FileOutputStream("decryptedFile"));
		int bytesRead;
		while((bytesRead = cipherIn.read(fileBuffer)) != -1){
			fileWriter.write(fileBuffer, 0, bytesRead);
		}
		cipherIn.close();
		fileWriter.flush();
		fileWriter.close();
	}

	private static void verify(String mode, byte[] signature, String keyFile) throws Exception{
		/**
		*	Verify the signature recieved from client with the contents of a file, based on the mode.
		*	@param mode
		*		t/u, which defines the file used for verification of the signature
		*	@param signature
		*		byte[] of the signature received from the client
		*	@param keyFile
		*		client's public key file, used for verification.
		*/
		String fileName = "";
		if("t".equals(mode)){
			//In trusted mode, use received file.
			fileName = "decryptedFile";
		}
		else{
			//In untrusted mode, use fakefile stored in the same folder.
			fileName = "fakefile";
		}
		if(compare(fileName, signature, keyFile)){
			System.out.println("Verification Passed");
		}else{
			System.out.println("Verification Failed");
		}
	}

	private static boolean compare(String fileName, byte[] signature, String keyFile) throws Exception{
		
		/**
		*	Vereifies the signature received from the client with the contents of the file.
		*	@param fileName
		*		name of the file, to verify the signature against.
		*	@param signature
		*		byte[] of the signature to verify
		*	@param keyFile
		*		name of the file, containig client's public key used for verification.
		*	Outputs whether the verification passed or failed.
		*/
		//Initializing stream to the key used for verification
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(keyFile));
		PublicKey key = (PublicKey) inputStream.readObject();
		//Initializing signature
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initVerify(key);

		FileInputStream datafis = new FileInputStream(fileName);
		BufferedInputStream bufin = new BufferedInputStream(datafis);

		byte[] buffer = new byte[1024];
		int len;
		while (bufin.available() != 0) {
			len = bufin.read(buffer);
			sig.update(buffer, 0, len);
		}
		bufin.close();
		return sig.verify(signature);
	}
}
