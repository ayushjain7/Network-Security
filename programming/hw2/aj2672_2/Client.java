import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.security.*;
import java.util.Random;

/**
 * Created by ayush on 2/29/16.
 * Name : Ayush Jain
 * Uni : aj2672
 */
public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Wrong number of arguments. Exiting");
            System.exit(1);
        }
        String host = args[0];
        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        }catch (NumberFormatException e){
            System.err.println("Port is not an integer. Exiting.");
            System.exit(0);
        }
        String keyStoreFile = args[2];
        String password = args[3];
        if(!(new File(keyStoreFile).exists())){
            System.err.println("Keystore file name entered does not exist. Exiting");
            System.exit(1);
        }

        {
            System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
            System.setProperty("javax.net.ssl.keyStorePassword", password);
            System.setProperty("javax.net.ssl.trustStore", keyStoreFile);
            System.setProperty("javax.net.ssl.trustStorePassword", password);
        }

        //loading keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyStoreFile), password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password.toCharArray());

        SSLContext secureSocket = SSLContext.getInstance("TLS");
        secureSocket.init(kmf.getKeyManagers(), null, null);
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(host, port);
        sslSocket.setNeedClientAuth(true);
        java.security.cert.Certificate cert = ks.getCertificate("client");

        try (
                OutputStream out = sslSocket.getOutputStream();
                InputStream in = sslSocket.getInputStream();
                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            String line = "";
            while (!"stop".equals(line)) {
                line = userIn.readLine();
                String[] input = line.split(" ");
                if(!"stop".equals(line) && input.length<3){
                    System.out.println("Missing parameters, a minimum of a filename and \"N\" or \"E\" is required");
                    continue;
                }

                String fileName = input[1];
                out.write((line+ "\n").getBytes());
                switch (input[0]) {
                    case ("put"):
                        if ("E".equals(input[2])) {
                            //If in E mode, send the signature and encrypted file
                            if(input.length != 4){
                                System.out.println("Missing parameters, a minimum of a filename and password is required in \"E\" mode");
                                break;
                            }
                            byte[] key = getAESKey(input[3]);
                            sign(out, fileName, (PrivateKey) ks.getKey("client", password.toCharArray()));
                            encryptFile(out, key, fileName);
                        } else if ("N".equals(input[2])) {
                            //If in N mode, send the signature and unencrypted file
                            if(input.length != 3){
                                System.out.println("Missing parameters, only filename and password is required in \"N\" mode");
                                break;
                            }
                            sign(out, fileName, (PrivateKey) ks.getKey("client", password.toCharArray()));
                            sendFile(out, fileName);
                        }else{
                            System.out.println("Invalid Parameter " + input[1]);
                            break;
                        }
                        System.out.println("Transfer of " + fileName + " complete");
                        break;
                    case ("get"):
                        //Receive hash, the file(encrypted or unencrypted) and then verify the signature.
                        byte[] hash = receiveHash(in);
                        receiveFile(in, fileName, input[2], input);
                        verify(hash, fileName, cert.getPublicKey());
                        break;
                    case ("stop"):
                        break;
                    default:
                        System.out.println("Invalid commands, options are get, put or stop");
                        break;
                }
            }
        }
    }

    private static byte[] receiveHash(InputStream in) throws IOException {
        /**
         *  Receives the hash from the socket.
         *  @param in
         *      socket inputStream
         *  @returns
         *      the hash value.
         */
        byte[] hash = new byte[256];
        int sizeByte;
        String sizeString ="";
        while((sizeByte = in.read())!= '\n' && sizeByte!='\r')
            sizeString += (char)sizeByte;
        int size = Integer.parseInt(sizeString.trim());
        int h = in.read(hash, 0, size);
        return hash;
    }

    private static byte[] getAESKey(String password) {
        /**
         *      Uses the SecureRandom
         **/
        byte[] random = new byte[16];
        Random prng = new Random(password.hashCode());
        prng.nextBytes(random);
        return random;
    }

    private static void verify(byte[] signature, String fileName, PublicKey key) throws Exception{
        /**
         *   Verifies the signature received from the client with the contents of the file.
         *   @param fileName
         *       name of the file, to verify the signature against.
         *   @param signature
         *       byte[] of the signature to verify
         *   @param key
         *       client's public key used for verification.
         *   Outputs whether the verification passed or failed.
         */
        //Initializing signature
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(key);

        File file = new File(fileName);
        FileInputStream datafis = new FileInputStream(fileName);
        BufferedInputStream bufin = new BufferedInputStream(datafis);

        byte[] buffer = new byte[256];
        int len;
        while (bufin.available() != 0) {
            len = bufin.read(buffer);
            sig.update(buffer, 0, len);
        }
        bufin.close();
        if(!sig.verify(signature)) {
//            file.delete();
            System.out.println("Verification failed.");
        }else{
            System.out.println("File received successfully");
        }
    }

    private static void receiveFile(InputStream in, String fileName, String parameter, String[] password) throws Exception {
        /**
         *  decrypts a file using AES/CBC mode with PKCS5 padding and writes it to a file.
         *  @param in
         *      Socket input stream
         *  @param fileName
         *      fileName to store the contents
         *  @param parameter
         *      mode E or N in which the file is to be sent.
         *  @param password
         *      seed used to generate the key for AES encryption
         */
        FileOutputStream fileWriter = new FileOutputStream(fileName);
        String sizeString ="";
        byte[] fileBuffer = new byte[1024];
        int sizeByte, total=0;
        while((sizeByte = in.read())!= '\n' && sizeByte!='\r')
            sizeString += (char)sizeByte;
        int size = Integer.parseInt(sizeString.trim());
        if("E".equals(parameter)) {
            byte[] iv = new byte[16];
            int i = in.read(iv, 0, 16);
            //Initializing cipher to the decrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(getAESKey(password[3]), "AES"), new IvParameterSpec(iv));
            //Cipher stream from the client
            CipherInputStream cipherIn = new CipherInputStream(in, cipher);

            //Stream to a file
            int bytesRead;
            do {
                bytesRead = cipherIn.read(fileBuffer, 0, 1024);
                total += bytesRead;
                fileWriter.write(fileBuffer);
                fileWriter.flush();
                //size-16 to account for the size of the IV.
            } while(total < size && bytesRead==1024);
        }else{
            //Stream to a file
            int bytesRead;
            do {
                bytesRead = in.read(fileBuffer, 0, 1024);
                total += bytesRead;
                fileWriter.write(fileBuffer);
                fileWriter.flush();
            } while(total < size && bytesRead==1024);
        }
        fileWriter.close();
    }

    private static void sendFile(OutputStream out, String fileName) throws Exception {
        /*
         *  Sends file as requested by the user.
         */
        FileInputStream fis = new FileInputStream(fileName);
        //Instantiating signature.
        byte[] dataBytes = new byte[1024*1024];
        int bytesRead, total = 0;
        while((bytesRead = fis.read(dataBytes, total, 1024)) > 0){
            total += bytesRead;
        }
        out.write((total+"\n").getBytes());
        out.flush();
        out.write(dataBytes, 0, total);
        out.flush();
        fis.close();
    }

    private static void sign(OutputStream out, String fileName, PrivateKey privKey) throws Exception{
        /**
         * signs the contents of a file using SHA-256 hash algorithm and RSA encryption
         * @param out
         *       output stream to the socket
         * @param fileName
         *       String name of file whose contents are to be signed
         * @param privKey
         *       String name of file containing the private key used to sign the contents
         * @return
         *       byte[] of signature
         */
        //Opening stream to the file to read contents.
        FileInputStream fis = new FileInputStream(fileName);
        //Buffered Stream to file since it is efficient.
        BufferedInputStream bufin = new BufferedInputStream(fis);
        //Instantiating signature.
        Signature mySign = Signature.getInstance("SHA256withRSA");
        mySign.initSign(privKey);
        byte[] dataBytes = new byte[1024];
        int nread;
        while ((nread = bufin.read(dataBytes)) >= 0) {
            mySign.update(dataBytes, 0, nread);
        }
        bufin.close();
        out.write(mySign.sign());
        out.flush();
    }

    private static void encryptFile(OutputStream stream, byte[] key, String fileName) throws Exception{
        /**
         *   encrypts a file using AES/CBC mode with PKCS5 padding and writes it to socket outputStream.
         *   @param stream
         *       Socket output stream
         *   @param fileName
         *       String name of the file to be encrypted and senti
         *   @param password
         *       password used for encryption
         *   @param iv
         *       Initialization vectore used for encryption
         */
        //Instantiating cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        //Create and send IV
        byte[] iv = new byte[16];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(iv);
        //Send File encrypted in AES mode.
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        //Cipher stream from client to server
        CipherOutputStream cipherOut = new CipherOutputStream(stream, cipher);
        byte[] fileBuffer = new byte[1024*1024];
        //File stream to read contents of file.
        InputStream fileReader = new FileInputStream(fileName);
        int bytesRead, i=0;
        while((bytesRead = fileReader.read(fileBuffer, i, 1024)) > 0){
            i += bytesRead;
        }

        stream.write((i + "\n").getBytes());
        stream.flush();
        stream.write(iv);
        stream.flush();
        cipherOut.write(fileBuffer, 0, i);
        cipherOut.flush();
        fileReader.close();
    }
}
