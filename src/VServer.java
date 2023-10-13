import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VServer {
    private static final int PORT = 8080;
    private static Map<String, File> cache = new HashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("VServer is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                Thread thread = new Thread(() -> handleRequest(clientSocket));
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            String request = reader.readLine();
            System.out.println("Received request: " + request);

            String[] requestParts = request.split(" ");
            String method = requestParts[0];
            String url = requestParts[1];

            if (method.equals("GET")) {
                File cachedFile = cache.get(url);
                if (cachedFile != null) {
                    Date lastModified = new Date(cachedFile.lastModified());
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                    String ifModifiedSince = format.format(lastModified);

                    Socket webServerSocket = new Socket(new URL(url).getHost(), 80);
                    BufferedWriter webServerWriter = new BufferedWriter(new OutputStreamWriter(webServerSocket.getOutputStream()));

                    webServerWriter.write("GET " + url + " HTTP/1.1\r\n");
                    webServerWriter.write("Host: " + new URL(url).getHost() + "\r\n");
                    webServerWriter.write("If-Modified-Since: " + ifModifiedSince + "\r\n");
                    webServerWriter.write("\r\n");
                    webServerWriter.flush();

                    BufferedReader webServerReader = new BufferedReader(new InputStreamReader(webServerSocket.getInputStream()));
                    String response;
                    while ((response = webServerReader.readLine()) != null) {
                        writer.write(response + "\r\n");
                    }

                    webServerReader.close();
                    webServerWriter.close();
                    webServerSocket.close();
                } else {
                    URLConnection connection = new URL(url).openConnection();
                    InputStream inputStream = connection.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("cache/" + url.hashCode()));

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        writer.write(new String(buffer, 0, bytesRead));
                    }

                    inputStream.close();
                    fileOutputStream.close();
                    cache.put(url, new File("cache/" + url.hashCode()));
                }
            }

            writer.flush();
            writer.close();
            reader.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
