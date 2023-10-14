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
                if (clientSocket.getInetAddress().toString().equals("http://today.hit.edu.cn/article/2023/10/13/107918")){
                    System.out.println("拒绝访问");
                    continue;
                }
                if (clientSocket.getInetAddress().toString().contains("http://www.wenku8.net/")){
                    //跳转到其他网站
                    System.out.println("跳转到其他网站");

                }
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
                    System.out.println("lastModified: " + lastModified);
                    System.out.println("If-Modified-Since: " + ifModifiedSince);
                    Socket webServerSocket = new Socket(new URL(url).getHost(), 80);
                    BufferedWriter webServerWriter = new BufferedWriter(new OutputStreamWriter(webServerSocket.getOutputStream()));

                    webServerWriter.write("GET " + url + " HTTP/1.1\r\n");
                    webServerWriter.write("Host: " + new URL(url).getHost() + "\r\n");
                    webServerWriter.write("If-Modified-Since: " + ifModifiedSince + "\r\n");
                    webServerWriter.write("\r\n");
                    webServerWriter.flush();

                    BufferedReader webServerReader = new BufferedReader(new InputStreamReader(webServerSocket.getInputStream()));
                    String response;
                    //如果返回消息是304，那么就不用再次请求了
                    if ((response = webServerReader.readLine()).contains("304")) {
                        System.out.println("缓存是最新的不需要修改");
                        BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));
                        while ((response = fileReader.readLine()) != null) {
                            writer.write(response + "\r\n");
                        }
                    }else {
                        //否则就把缓存文件的内容更新并返回给客户端
                        System.out.println("缓存需要被更新");
                        FileOutputStream fileoutputStream = new FileOutputStream(new File("cache/" + url.hashCode()));
                        writer.write(response + "\r\n");
                        fileoutputStream.write(response.getBytes());
                        while ((response = webServerReader.readLine()) != null) {
                            writer.write(response + "\r\n");
                            fileoutputStream.write(response.getBytes());
                        }
                        fileoutputStream.close();
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
