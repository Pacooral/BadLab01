//监听8080端口，监听来自浏览器的访问请求
//接收到浏览器对远程网站的浏览请求后，在VServer的缓存中检索URL对应的对象
//如果缓存中存在该对象，提取对象文件的最新修改时间，并在客户的请求报文首部插入If-Modified-Since字段，并向原Web服务器转发修改后的请求报文
//如果缓存中没有该对象，则会直接向源服务器转发请求报文，并将源服务器返回的响应报文转发给客户端，同时将响应报文中的对象文件缓存到VServer的缓存中


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class VServer1 {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket server = new ServerSocket(port);
        System.out.println("服务器正在监听端口：" + port);
        System.out.println("服务器正在运行...");
        //System.out.println(server.getInetAddress().getLocalHost());
        //在此处可以对访问IP进行限制
        int index = 0;
        while(true){
            index++;
            Socket accept = server.accept();
            System.out.println("第"+index+"次连接");
            OutputStream out = accept.getOutputStream();
            InputStream in = accept.getInputStream();
            //创建一个新的线程，用来处理客户端的请求
            String requestType = "";
            String requestUrl = "";
            String requestVersion = "";
            String dstaddr = "";
            String dsthost = "";
            int dstPort = 80;
            //头部信息 用来判断是Get还是Post
            StringBuilder header = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String readline = br.readLine();
            System.out.println("开始读取头部信息");
            //读取头信息
            while (readline != null && !readline.isEmpty()) {
                header.append(readline).append("\r\n");
                if (readline.startsWith("GET")) {
                    requestType = "GET";
                    requestUrl = readline.split(" ")[1];
                    requestVersion = readline.split(" ")[2];
                } else if (readline.startsWith("POST")) {
                    requestType = "POST";
                    requestUrl = readline.split(" ")[1];
                    requestVersion = readline.split(" ")[2];
                } else if (readline.startsWith("Host")) {
                    dstaddr= readline.split(" ")[1];
                }
                readline = br.readLine();
            }
            System.out.println(header.toString());
            if (requestUrl == null){
                continue;
            }
            if(requestUrl.contains("443")||requestUrl.contains("https"))
            {
                System.out.println(requestUrl+"  是 https 请求，不做处理");
                continue;
            }


            if(dstaddr.split(":").length>1)
            {
                dstPort = Integer.parseInt(dstaddr.split(":")[1]);
            }
            dsthost = dstaddr.split(":")[0];
            if(requestType.isEmpty()) {
                System.out.println("未得到请求类型本次连接结束");
                continue;
            }
            System.out.println("请求消息的Type为："+ requestType);
            System.out.println("请求的url为"+requestUrl);
            System.out.println("开始转发消息");
            //转发消息

            if (dstaddr.contains("accc.hit.edu.cn")){
                System.out.println("本次请求的是哈工大accc的网站");
                System.out.println("这个网站不允许访问！");
                String body = "<html><head><title>403 Forbidden</title></head><body bgcolor=\"white\"><center><h1>403 Forbidden</h1></center><hr><center>nginx</center></body></html>";
                String response = "HTTP/1.1 403 Forbidden\r\n" +
                        "Server: nginx\r\n" +
                        "Date: Sun, 20 Dec 2020 14:54:00 GMT\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: "+body.length()+"\r\n" +
                        "Connection: close\r\n" +
                        "Vary: Accept-Encoding\r\n" +
                        "\r\n" +
                        body;
                out.write(response.getBytes());
                out.flush();
                accept.close();
                continue;
            }
            if (dstaddr.contains("pacooral.cn")){
                System.out.println("本次请求的是pacooral.cn的网站");
                System.out.println("即将为你转到今日哈工大主页！");
                dstaddr = "hit.edu.cn";
                dsthost = "hit.edu.cn";
                dstPort = 80;
                header = new StringBuilder("GET http://today.hit.edu.cn/ HTTP/1.1\n" +
                        "Host: today.hit.edu.cn\n" +
                        "Proxy-Connection: keep-alive\n" +
                        "Cache-Control: max-age=0\n" +
                        "Upgrade-Insecure-Requests: 1\n" +
                        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36 Edg/117.0.2045.60\n" +
                        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\n" +
                        "Accept-Encoding: gzip, deflate\n" +
                        "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\n"+"\n");

            }


            if(dsthost!=null && !dsthost.isEmpty()){
                //向目标服务器转发请求
                Socket socket = new Socket(dsthost, dstPort);
                OutputStream proxyOut = socket.getOutputStream();
                InputStream proxyIn = socket.getInputStream();
                //向目标服务器发送请求
                if(requestType.equals("POST")){
                    //什么也不做
                } else if(requestType.equals("GET")){
                    boolean hasFile = false;
                    String filePath = "./cache/"+dstaddr+".txt";
                    File file = new File(filePath);
                    File folder = new File("./cache");
                    if(!folder.exists()){
                        folder.mkdir();
                    }
                    File[] files = folder.listFiles();
                    for (File ListFile :files){
                        if(ListFile.getName().equals(dstaddr+".txt")){
                            hasFile = true;
                            System.out.println("缓存中存在"+requestUrl+"的文件");
                            file = ListFile;
                            break;
                        }
                    }
                    if(hasFile){
                        System.out.println("缓存中存在"+dstaddr+"的文件");
                        //缓存中存在该文件
                        Scanner scanner = new Scanner(new FileReader(file));
                        String line = null;
                        String date = null;
                        while (scanner.hasNextLine()){
                            line = scanner.nextLine();
                            if(line.startsWith("Date")){
                                date = line.substring(6);
                            }
                        }
                        scanner.close();
                        //向目标服务器发送请求
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("GET "+requestUrl+" HTTP/1.1\r\n");
                        buffer.append("Host: "+dsthost+":"+dstPort+ "\r\n");
                        buffer.append("If-Modified-Since: "+date+"\r\n");
                        buffer.append("\r\n");
                        proxyOut.write(buffer.toString().getBytes());
                        proxyOut.flush();
                        //接收目标服务器的响应
                        byte[] bytes = new byte[1024];
                        int len = proxyIn.read(bytes);
                        String response = new String(bytes, 0, len);
                        System.out.println(response);
                        if(response.contains("304")){
                            System.out.println("缓存中的文件是最新的,可以直接使用");
                            //缓存中的文件是最新的
                            //向客户端发送响应
                            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                            byte[] buffer1 = new byte[1024];
                            int len1 = 0;
                            while ((len1 = inputStream.read(buffer1)) != -1) {
                                out.write(buffer1, 0, len1);
                            }
                            inputStream.close();
                            out.flush();
                            out.close();
                        } else {
                            System.out.println("缓存中的文件不是最新的");
                            //缓存中的文件不是最新的
                            //向客户端发送响应
                            proxyOut.write(header.toString().getBytes());
                            proxyOut.flush();
                            socket.shutdownOutput();
                            //接收目标服务器的响应
                            //缓存
                            FileOutputStream outputStream = new FileOutputStream(file);
                            BufferedInputStream inputStream = new BufferedInputStream(proxyIn);
                            byte[] buffer2 = new byte[1024];
                            int len2 = 0;
                            while ((len2 = inputStream.read(buffer2)) != -1) {
                                out.write(buffer2, 0, len2);
                                outputStream.write(buffer2, 0, len2);
                            }
                            inputStream.close();
                            outputStream.flush();
                            outputStream.close();
                            out.flush();
                            out.close();
                        }
                    }else {
                        proxyOut.write(header.toString().getBytes());
                        proxyOut.flush();
                        socket.shutdownOutput();
                        //接收目标服务器的响应
                        //缓存
                        System.out.println("缓存中不存在"+requestUrl+"的文件");
                        file.createNewFile();
                        FileOutputStream outputStream = new FileOutputStream(file);
                        BufferedInputStream inputStream = new BufferedInputStream(proxyIn);
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        while ((len = inputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            outputStream.write(buffer, 0, len);
                        }
                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        out.flush();
                        out.close();
                    }
                }
            } else {
                System.out.println("请求xxxxxxxxxxxxx");
                Socket proxy = new Socket(dsthost,dstPort);
                OutputStream proxyOut = proxy.getOutputStream();
                InputStream proxyIn = proxy.getInputStream();
                //向目标服务器发送请求
                proxyOut.write(header.toString().getBytes());
                proxyOut.flush();
                proxy.shutdownOutput();
                //接收目标服务器的响应
                BufferedInputStream inputStream = new BufferedInputStream(proxyIn);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                inputStream.close();
                out.flush();
                out.close();
            }
            accept.close();
            //休息5s继续下一次
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

