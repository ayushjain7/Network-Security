import java.io.IOException;
import java.io.File;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Key;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import com.sun.xml.internal.org.jvnet.staxex.Base64Encoder;

public class KeyGenerator1{
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
		saveKey(new Base64Encoder.encode(privateKeyFile, key.getPrivate().getEncoded()));
		saveKey(new Base64Encoder.encode(publicKeyFile, key.getPublic().getEncoded()));
	}

	private static void createFile(File file) throws IOException{
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		file.createNewFile();
	}

	private static void saveKey(File file, String key) throws IOException{
		FileOutputStream os = new FileOutputStream(file);
		os.write(key.getBytes());
		os.close();
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		String privKeyFile = args[0];
		String pubKeyFile = args[1];
		generateKey(pubKeyFile, privKeyFile);
	}

}
