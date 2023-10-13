import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.net.InetAddress;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class SocketServer {
    public static void main(String[] args) {
        try {
            int port = 8080;
            ServerSocket server= new ServerSocket(port);
            System.out.println("启动服务器");
            System.out.println("客户端:"+server.getInetAddress().getLocalHost()+"已连接到服务器");
            int index=0;
            while(true)
            {
                index++;
                Socket s = server.accept();
                System.out.println("第"+index+"条请求:");
                OutputStream output = null;
                InputStream input = null;
                output = s.getOutputStream();
                input = s.getInputStream();

                boolean ifblockuser = false;
                if(ifblockuser)
                {
                    if(s.getInetAddress().getLocalHost().getHostAddress().equals("192.168.32.1"))
                    {
                        String body = "<h1>你不许上网</h1>";
                        String response = "HTTP/1.1 200 ok\r\n" +
                                "Content-Length: " + body.getBytes().length + "\r\n" +
                                "Content-Type: textml; charset-utf-8\r\n" +
                                "\r\n" +
                                body + "\r\n";
                        System.out.println("已屏蔽用户");
                        output.write(response.getBytes());
                        //按照协议，将返回请求由outputStream写入
                        output.flush();
                        continue;
                    }
                }


                String type = null , dsthost = null , URL = null , dstaddr = null;
                int dstport = 80;
                int flag=1;
                StringBuilder header = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(input));
                String readLine = br.readLine();
                while (readLine != null && !readLine.equals("")) {
                    if(flag==1)
                    //第一行获取url和请求类型
                    {
                        type = readLine.split(" ")[0];
                        URL = readLine.split(" ")[1];
                        flag = 0;
                    }
                    String[] s1=readLine.split(": ");
                    for(int i=0;i<s1.length;i++){
                        if(s1[i].equals("Host"))
                        {
                            dstaddr=s1[i+1];
                        }
                    }
                    header.append(readLine).append("\r\n");
                    readLine = br.readLine();
                }
                //解析首部完成

                if(URL==null)
                {
                    continue;
                }
                if(URL.contains("443"))
                {
                    System.out.println(URL+"  是 https");
                    continue;
                }

                if(URL.contains("yzb.hit.edu.cn"))
                {
                    System.out.println("钓到鱼了");
                    dstaddr="hituc.hit.edu.cn";
                    header= new StringBuilder("GET http://hituc.hit.edu.cn/ HTTP/1.1\n"+
                            "Host: hituc.hit.edu.cn\n" +
                            "Proxy-Connection: keep-alive\n" +
                            "Upgrade-Insecure-Requests: 1\n" +
                            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36 Edg/105.0.1343.53\n" +
                            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\n" +
                            "Accept-Encoding: gzip, deflate\n" +
                            "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\n"+"\n");
                }


                //钓鱼网站

                if (dstaddr.split(":").length > 1) {//代表包含端口号
                    dstport = Integer.valueOf(dstaddr.split(":")[1]);
                }
                dsthost = dstaddr.split(":")[0];
                //获取目的地址和端口号
                header.append("\r\n");

                if(dstaddr.contains("jwc.hit.edu.cn")){
                    //将today.hit.edu.cn作为过滤对象
                    System.out.println(type);
                    System.out.println(URL);
                    String body = "<h1>你不许访问这个网站</h1>";
                    String response = "HTTP/1.1 200 ok\r\n" +
                            "Content-Length: " + body.getBytes().length + "\r\n" +
                            "Content-Type: textml; charset-utf-8\r\n" +
                            "\r\n" +
                            body + "\r\n";
                    System.out.println("用户访问了jwc.hit.edu.cn，已拒绝");
                    output.write(response.getBytes());
                    //按照协议，将返回请求由outputStream写入
                    output.flush();
                    s.close();
                    continue;
                }

                System.out.println(type+"->"+URL);
                System.out.println("-----------");

                if(dsthost!=null && !dsthost.equals(""))
                {
                    Socket proxy = new Socket(dsthost,dstport);
                    OutputStream proxyOut = proxy.getOutputStream();
                    InputStream proxyIn = proxy.getInputStream();
                    if (type.equals("GET")) {
                        boolean ifhasfile = false;
                        System.out.println(dsthost);
                        String dir ="./cache/"+ dsthost+index +".txt";
                        File file = new File(dir);
                        File folder = new File("cache");
                        for(File file0:folder.listFiles())
                        {
                            BufferedReader fileReader=new BufferedReader(new InputStreamReader(new FileInputStream(file0)));
                            if(fileReader.readLine().equals(URL))//缓存命中
                            {
                                System.out.println(URL+"-->	hit!缓存命中");
                                ifhasfile = true;
                                file = file0;
                                break;
                            }
                        }

                        if(ifhasfile)
                        {
                            Scanner sc =new Scanner (new FileReader(file));
                            String line,date=null;
                            while(sc.hasNextLine())
                            {
                                line = sc.nextLine();
                                if(line.contains("Date"))
                                {
                                    date = line.substring(6);
                                }
                            }
                            StringBuffer ifGetReqBuffer = new StringBuffer();
                            ifGetReqBuffer.append("GET " + URL + " HTTP/1.1\r\n");
                            ifGetReqBuffer.append("Host: " + dsthost + ":" + dstport + "\r\n");
                            ifGetReqBuffer.append("If-modified-since: " + date + "\r\n");
                            ifGetReqBuffer.append("\r\n");
                            String ifGetReq = ifGetReqBuffer.toString();
                            proxyOut.write(ifGetReq.getBytes());
                            proxyOut.flush();
                            byte[] tempBytes = new byte[30];
                            int len = proxyIn.read(tempBytes);
                            String res = new String(tempBytes, 0, len);
                            if(res.contains("304"))
                            {
                                System.out.println("缓存内容未更新，直接使用");
                                BufferedInputStream inputStream=new BufferedInputStream(new FileInputStream(file));
                                String pad=URL+"\r\n";
                                inputStream.read(pad.getBytes());
                                int length=-1;
                                byte[] bytes=new byte[1024];
                                proxy.shutdownOutput();
                                while((length=inputStream.read(bytes))!=-1){
                                    output.write(bytes,0,length);
                                }
                                output.flush();
                            }
                            else
                            {
                                System.out.println("缓存内容更新，重新缓存并使用");
                                file.delete();
                                file.createNewFile();
                                FileOutputStream outputStream=new FileOutputStream(file);
                                String pad=URL+"\r\n";
                                outputStream.write(pad.getBytes("utf-8"));//将URL作为缓存文件的第一行
                                outputStream.write(tempBytes,0,len);
                                output.write(tempBytes,0,len);
                                proxy.shutdownOutput();
                                BufferedInputStream inputStream=new BufferedInputStream(proxyIn);
                                byte[] buf = new byte[1024];
                                int size = 0;
                                while (( size = inputStream.read(buf)) != -1) {
                                    output.write(buf,0,size);
                                    outputStream.write(buf,0,size);
                                }
                                output.flush();
                                outputStream.flush();
                            }
                        }
                        else
                        {
                            proxyOut.write(header.toString().getBytes("utf-8"));
                            proxyOut.flush();
                            proxy.shutdownOutput();
                            //如果缓存不存在，则将服务器传来的数据传给客户端并且缓存
                            System.out.println(URL+"	miss!缓存未命中");
                            file.createNewFile();
                            FileOutputStream outputStream=new FileOutputStream(file);
                            String pad=URL+"\r\n";
                            outputStream.write(pad.getBytes("utf-8"));//将URL作为缓存文件的第一行
                            BufferedInputStream inputStream=new BufferedInputStream(proxyIn);
                            byte[] buf = new byte[1];
                            int size = 0;
                            while (( size = inputStream.read(buf)) != -1) {
                                output.write(buf,0,size);
                                outputStream.write(buf,0,size);
                            }
                            System.out.println("okk");
                            output.flush();
                            outputStream.flush();
                        }
                    }
                }
                else
                {
                    Socket proxy = new Socket(dsthost,dstport);
                    OutputStream proxyOut = proxy.getOutputStream();
                    InputStream proxyIn = proxy.getInputStream();
                    proxyOut.write(header.toString().getBytes());
                    proxyOut.flush();
                    proxy.shutdownOutput();
                    BufferedInputStream inputStream=new BufferedInputStream(proxyIn);
                    byte[] buf = new byte[1];
                    int size = 0;
                    while (( size = inputStream.read(buf)) != -1) {
                        output.write(buf,0,size);
                    }
                    output.flush();
                }

                s.close();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}