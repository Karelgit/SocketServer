package NewsPusherModule.client;

import NewsPusherModule.entity.HandShaker;
import NewsPusherModule.entity.KeepAlive;
import NewsPusherModule.entity.PushInfo;
import NewsPusherModule.util.FileWriterUtil;
import com.alibaba.fastjson.JSON;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 *    C/S架构的客户端对象，持有该对象，可以随时向服务端发送消息。
 * <p>
 * 创建时间：2010-7-18 上午12:17:25
 * @author HouLei
 * @since 1.0
 */
public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
        String serverIp = resourceBundle.getString("SERVER_IP");
        int port = Integer.parseInt(resourceBundle.getString("SERVER_PORT"));
        Client client = new Client(serverIp,port);
        client.addActionMap(Object.class,new HandShakerObjectAction());
        client.start();
    }

    private static BufferedWriter bufferedWriter;

    /**
     * 处理服务端发回的对象，可实现该接口。
     */
    public static interface ObjectAction{
        void doAction(Object obj,Client client);
    }
    public static final class DefaultObjectAction implements ObjectAction{
        public void doAction(Object obj,Client client) {
            try {
                //搜集日志

                FileWriterUtil.writeLog("客户端处理：\t"+obj.toString()+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public static final class HandShakerObjectAction implements ObjectAction {
        private Socket socket;
        private List<HandShaker> handShakerList = new ArrayList<HandShaker>();

//        public HandShakerObjectAction(Socket socket,ArrayList<HandShaker> handShakerList)    {
//            this.socket = socket;
//            this.handShakerList = handShakerList;
//        }
        public void doAction(Object obj,Client client) {
            HandShaker handShaker = ((HandShaker) obj);
            handShaker.setClientSocket(socket);
            handShakerList.add(handShaker);
            System.out.println("客户端获得handShaker的列表"+JSON.toJSONString(handShakerList));
        }

    }

    private String serverIp;
    private int port;
    private Socket socket;
    private boolean running=false;
    private long lastSendTime;
    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class,ObjectAction>();

    public Client(String serverIp, int port) {
        this.serverIp=serverIp;this.port=port;
    }

    public void start() throws UnknownHostException, IOException {
        if(running)return;
        socket = new Socket(serverIp,port);
        System.out.println("本地端口："+socket.getLocalPort());
        lastSendTime=System.currentTimeMillis();
        running=true;

        new Thread(new KeepAliveWatchDog()).start();
        new Thread(new ReceiveWatchDog()).start();
        new Thread(new SendHandShaker()).start();

    }

    public void stop(){
        if(running)running=false;
    }

    /**
     * 添加接收对象的处理对象。
     * @param cls 待处理的对象，其所属的类。
     * @param action 处理过程对象。
     */
    public void addActionMap(Class<Object> cls,ObjectAction action){
        actionMapping.put(cls, action);
    }

    public void sendObject(Object obj) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(obj);
        oos.flush();
    }

    class SendHandShaker implements Runnable{
        public void run() {
            //客户端发送HanndShaker信息，包括用户名，密码
            HandShaker handShaker = new HandShaker();
            handShaker.setUsername("duocai");
            handShaker.setPassword("123456");
            System.out.println("客户端发送handShaker信息是：\t"+ JSON.toJSONString(handShaker));
            try {
                sendObject(handShaker);
            } catch (IOException e) {
                System.out.println("客户端发送handShaker信息时发生错误！");
                e.printStackTrace();
            }
        }
    }

    class KeepAliveWatchDog implements Runnable{
        long checkDelay = 10;
        long keepAliveDelay = 2000;
        public void run() {
            while(running){
                if(System.currentTimeMillis()-lastSendTime>keepAliveDelay){
                    try {
                        Client.this.sendObject(new KeepAlive());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Client.this.stop();
                        //启动重连服务端
                        new Thread(new ReconnectSocket()).start();

                    }
                    lastSendTime = System.currentTimeMillis();
                }else{
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Client.this.stop();
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
                        Object obj = ois.readObject();
                        //搜集日志
                        FileWriterUtil.writeLog("客户端接收：\t"+obj+"\n");
                        ObjectAction oa = actionMapping.get(obj.getClass());
                        if(obj instanceof PushInfo) {
                            //加入推送逻辑
                            System.out.println("收到的推送新闻的信息："+JSON.toJSONString(obj));
                            Long timeBefore = System.currentTimeMillis();
                            Thread.sleep(10*1000);
                            Long timeAfter = System.currentTimeMillis();
                            System.out.println("time elaps: " +(timeAfter-timeBefore)/1000+"seconds");

                        }
                        oa = oa==null?new DefaultObjectAction():oa;
                        oa.doAction(obj, Client.this);
                    }else{
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Client.this.stop();
                }
            }
        }
    }

    class ReconnectSocket implements Runnable    {
        public void run() {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
            String serverIp = resourceBundle.getString("SERVER_IP");
            int port = Integer.parseInt(resourceBundle.getString("SERVER_PORT"));
            Client client = new Client(serverIp,port);
            client.addActionMap(Object.class,new HandShakerObjectAction());
            try {
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}