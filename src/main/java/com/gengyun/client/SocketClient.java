package com.gengyun.client;

import com.alibaba.fastjson.JSON;
import com.gengyun.model.HandShaker;
import com.gengyun.model.KeepAlive;
import com.gengyun.model.PushINfo;

import javax.xml.bind.SchemaOutputResolver;
import java.io.*;
import java.net.Socket;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <类详细说明:Socket客户端>
 *
 * @Author： Huanghai
 * @Version: 2016-12-27
 **/
public class SocketClient {
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private Socket socket;
    private boolean running = false;
    private long lastSendTime;
    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class,ObjectAction>();

    public SocketClient(String SERVER_IP,int SERVER_PORT)   {
        this.SERVER_IP = SERVER_IP;
        this.SERVER_PORT = SERVER_PORT;
    }

    /**
     * 处理服务端发回的对象，可实现该接口。
     */
    public static interface ObjectAction{
        void doAction(Object obj,SocketClient client);
    }
    public static final class DefaultObjectAction implements ObjectAction{
        public void doAction(Object obj,SocketClient client) {
            System.out.println("处理：\t"+obj.toString());
        }
    }

    public void start() throws IOException {
        if(running) {
            return;
        }
        ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
        String SERVER_IP = resourceBundle.getString("SERVER_IP");
        int SERVER_PORT = Integer.parseInt(resourceBundle.getString("SERVER_PORT"));
        socket = new Socket(SERVER_IP,SERVER_PORT);
        System.out.println("本地端口：" + socket.getLocalPort());
        /** 发送客户端准备传输的信息 */
        HandShaker handShaker = new HandShaker();
        handShaker.setUsername("duocai");
        handShaker.setPassword("123456");
        //将客户端的对象数据流输出到服务器端去
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(handShaker);
        System.out.println("发送：\t"+handShaker);
        oos.flush();

        lastSendTime = System.currentTimeMillis();
        running = true;
        new Thread(new KeepAliveWatchDog()).start();
        new Thread(new ReceiveWatchDog()).start();
    }

    public void stop(){
        if(running)running=false;
    }

    public void sendObject(Object obj) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(obj);
        System.out.println("发送：\t"+obj);
        oos.flush();
    }

    class KeepAliveWatchDog implements Runnable{
        long checkDelay = 10;
        long keepAliveDelay = 2000;
        public void run() {
            while(running){
                if(System.currentTimeMillis()-lastSendTime>keepAliveDelay){
                    try {
                        SocketClient.this.sendObject(new KeepAlive());
                    } catch (IOException e) {
                        e.printStackTrace();
                        SocketClient.this.stop();
                    }
                    lastSendTime = System.currentTimeMillis();
                }else{
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        SocketClient.this.stop();
                    }
                }
            }
        }
    }

    class ReceiveWatchDog implements Runnable{
        public void run() {
            while(running){
                try {
                    InputStream in = socket.getInputStream();
                    if(in.available()>0){
                        ObjectInputStream ois = new ObjectInputStream(in);
                        PushINfo pushINfo = ((PushINfo) ois.readObject());
                        System.out.println("接收：\t"+JSON.toJSONString(pushINfo));
                    }else{
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                   SocketClient.this.stop();
                }
            }
        }
    }
    public static void main(String[] args) {
        /** 创建Socket*/
        SocketClient socketClient =new SocketClient("127.0.0.1",2013);
        try {
            socketClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
