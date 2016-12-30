package com.gengyun.server;

import com.alibaba.fastjson.JSON;
import com.gengyun.model.HandShaker;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * <类详细说明：推送服务器>
 *
 * @Author： Huanghai
 * @Version: 2016-12-27
 **/
public class PushServer extends ServerSocket {
    private static final int SERVER_PORT = 2013;
    private volatile boolean running=false;
    private long receiveTimeDelay=3000;
    private Thread connWatchDog;
    List<HandShaker> clientList = new ArrayList<HandShaker>();

    public PushServer() throws IOException {
        super(SERVER_PORT);
        while (true) {
            Socket socket = accept();
            new CreateServerThread(socket);//当有请求时，启一个线程处理

        }
    }

    //每接受一个客户端，创建一个线程类
    class CreateServerThread extends Thread {
        private Socket client;
        public CreateServerThread(Socket s) throws IOException {
            client = s;
            start();
            //监控客户端是否已死
            connWatchDog = new Thread(new ConnWatchDog());
            connWatchDog.start();
        }

        public void run() {
            System.out.println("线程开启");
            //对象数据到的获取与返回
            HandShaker handShaker = null;
            try {
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                handShaker = ((HandShaker)in.readObject());
                handShaker.setClientSocket(client);
                //把用户名密码和socket的握手信息存入客户消息List中
                clientList.add(handShaker);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("获得客户端的handShaker信息：" + JSON.toJSONString(handShaker));
        }
    }

    //检查客户端的机制
    class ConnWatchDog implements Runnable{
        public void run(){
            try {
                ServerSocket ss = new ServerSocket(SERVER_PORT);
                while(running){
                    Socket s = ss.accept();
                    new Thread(new SocketAction(s)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                PushServer.this.stop();
            }

        }
    }

    //调用心跳检查程序
    class SocketAction implements Runnable{
        Socket s;
        boolean run=true;
        long lastReceiveTime = System.currentTimeMillis();
        public SocketAction(Socket s)   {
            this.s = s;
        }
        public void run() {
            while (run) {
                if(System.currentTimeMillis()-lastReceiveTime>receiveTimeDelay) {
                    overThis();
                }
            }
        }
        private void overThis() {
            if(run) {
                run = false;
            }
            if(s !=null)    {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("关闭："+s.getRemoteSocketAddress());
        }
    }

    class NewsPusher implements Runnable{


        public void run() {

        }
    }
    public void stop()  {
        connWatchDog.stop();
    }

    public static void main(String[] args) throws IOException {
        new PushServer();
    }
}
