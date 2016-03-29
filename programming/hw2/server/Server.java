import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;

public class Server{
    public static void main(String[] args) throws Exception {
        if(args.length != 3){
            System.err.println("Wrong number of arguments. Exiting");
            System.exit(1);
        }
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e){
            System.err.println("Port is not an integer. Exiting.");
            System.exit(0);
        }
        String keyStoreFile = args[1];
        if(!(new File(keyStoreFile).exists())){
            System.err.println("Keystore file name entered does not exist. Exiting");
            System.exit(1);
        }
        String password = args[2];

        {
            System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
            System.setProperty("javax.net.ssl.keyStorePassword", password);
            System.setProperty("javax.net.ssl.trustStore", keyStoreFile);
            System.setProperty("javax.net.ssl.trustStorePassword", password);
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyStoreFile), password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password.toCharArray());

        SSLContext secureSocket = SSLContext.getInstance("TLS");
        secureSocket.init(kmf.getKeyManagers(), null, null);
        SSLServerSocketFactory ssf = secureSocket.getServerSocketFactory();
        SSLServerSocket socket = (SSLServerSocket) ssf.createServerSocket(port);
        socket.setNeedClientAuth(true);

        try(
                Socket clientSocket = socket.accept();
                OutputStream out = clientSocket.getOutputStream();
                InputStream in = clientSocket.getInputStream();
        ) {
            String line = "";
            while (!"stop".equals(line)) {
                line = "";
                int i=0;char c;
                while((c = (char)in.read()) != '\n' && c!='\r'){
                    line += c;
                }
                String[] input = line.split(" ");
                String fileName = input[1];
                switch (input[0]){
                    case ("put"):
                        saveHash(fileName, in);
                        saveFile(fileName, in, input[2]);
                        System.out.println("File and Hash saved");
                        break;
                    case ("get"):
                        sendHash(out, fileName+".sha256");
                        sendFile(out, fileName);
                        break;
                    case ("stop"):
                        System.exit(0);
                }
            }
        }
    }

    private static void sendFile(OutputStream out, String fileName) throws Exception {
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

    private static void sendHash(OutputStream out, String fileName) throws Exception{
        FileInputStream fis = new FileInputStream(fileName);
        byte[] dataBytes = new byte[1000];
        int bytesRead = fis.read(dataBytes, 0, 1000);
        out.write((bytesRead+"\n").getBytes());
        out.flush();
        out.write(dataBytes, 0, bytesRead);
        out.flush();
        fis.close();
    }

    private static void saveFile(String fileName, InputStream in, String mode) throws Exception {
        FileOutputStream writer = new FileOutputStream(fileName);
        byte[] fileIn = new byte[1024];
        int n=0, k, size;
        char c;
        String line = "";
        while((c = (char)in.read()) != '\n'){
            line += c;
        }
        size = Integer.parseInt(line);
        if("E".equals(mode)) {
            //Writing IV.
            n = in.read(fileIn, 0, 16);
            writer.write(fileIn);
            writer.flush();
        }

        //Writing file.
        do {
            k = in.read(fileIn, 0, 1024);
            n += k;
            writer.write(fileIn);
            writer.flush();
        } while(n < size && k==1024);
        writer.close();
    }

    private static void saveHash(String fileName, InputStream in) throws IOException {
        byte[] hash = new byte[256];
        int h = in.read(hash, 0, 256);
        FileOutputStream writer= new FileOutputStream(fileName+".sha256");
        writer.write(hash, 0, h);
        writer.flush();
        writer.close();
    }
}
