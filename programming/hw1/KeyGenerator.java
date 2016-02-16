//Name : Ayush Jain
//Uni : aj2672

import java.io.IOException;
import java.io.File;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;

public class KeyGenerator{
	/**
	*	Generates a pair of RSA keys and saves them into files passed by the parameters
	*	@param pubKeyFile
	*		file name containing the public key
	*	@param privKeyFile
	*		file name containing the private key
	*/
	private static void generateKey(String pubKeyFile, String privKeyFile) throws IOException, NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair key = keyGen.generateKeyPair();

		File privateKeyFile = new File(privKeyFile);
		File publicKeyFile = new File(pubKeyFile);

		// Create files to store public and private key
		createFile(privateKeyFile);
		createFile(publicKeyFile);

		// Saving the Public & Private key in a file
		saveKey(privateKeyFile, key.getPrivate());
		saveKey(publicKeyFile, key.getPublic());
	}

	private static void createFile(File file) throws IOException{
	/**
	*	Creates file and director structure passed by the user to save the private and public keys.
	*/
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		file.createNewFile();
	}

	private static void saveKey(File file, Key key) throws IOException{
	/**
	*	Saves the key into the passed file.
	*/
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
		os.writeObject(key);
		os.close();
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		String privKeyFile = args[0];
		String pubKeyFile = args[1];
		generateKey(pubKeyFile, privKeyFile);
	}

}
