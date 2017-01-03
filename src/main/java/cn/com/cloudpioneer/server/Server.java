package cn.com.cloudpioneer.server;

import cn.com.cloudpioneer.entity.HandShaker;
import cn.com.cloudpioneer.entity.PushInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C/S架构的服务端对象。
 * <p>
 * 创建时间：2010-7-18 上午12:17:37
 * @author HouLei
 * @since 1.0
 */
public class Server {

    private List<HandShaker> handShakerList = new ArrayList<HandShaker>();
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");


    /**
     * 要处理客户端发来的对象，并返回一个对象，可实现该接口。
     */
    public interface ObjectAction {
        Object doAction(Object rev);
    }

    public static final class DefaultObjectAction implements ObjectAction {
        public Object doAction(Object rev) {
            System.out.println("处理并返回：" + rev);
            return rev;
        }
    }

    public final class HandShakerAction implements ObjectAction {
        private HandShaker handShaker;
        public HandShakerAction(HandShaker handShaker)    {
            this.handShaker = handShaker;
        }
        public Object doAction(Object rev) {
            handShaker = ((HandShaker) rev);
            handShakerList.add(handShaker);
            System.out.println("服务器获得的handshaker: "+ JSON.toJSONString(handShaker));
            return handShaker;
        }
    }

    public final class PushInfoAction implements ObjectAction   {
        private PushInfo pushInfo;
        public PushInfoAction(PushInfo pushInfo) {
           this.pushInfo = pushInfo;
        }
        public Object doAction(Object rev) {
            pushInfo = ((PushInfo) rev);
            //通过pushInfo里面客户信息和handShaker比对，拿到对应的客户端的socket
            Socket socket = null;
            for (HandShaker handShaker : handShakerList) {
                if(handShaker.getUsername().equals(pushInfo.getUserName())) {
                     socket= handShaker.getClientSocket();
                }
            }
            System.out.println("服务器获得的PushInfo是： "+JSON.toJSONString(pushInfo));
            return socket;
        }
    }

    /*public static void main(String[] args) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
        int port = Integer.parseInt(resourceBundle.getString("SERVER_PORT"));
        Server server = new Server(port);
        server.start();
    }*/

    /**
     * 启动serverSocket新闻推送服务
     */
    public void startPushServer() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
        int port = Integer.parseInt(resourceBundle.getString("SERVER_PORT"));
        Server server = new Server(port);
        server.start();
    }

    private int port;
    private volatile boolean running = false;
    private long receiveTimeDelay = 3000;
    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class, ObjectAction>();
    private Thread connWatchDog;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        if (running) return;
        running = true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    @SuppressWarnings("deprecation")
    public void stop() {
        if (running) running = false;
        if (connWatchDog != null) connWatchDog.stop();
    }

    public void addActionMap(Class<?> cls, ObjectAction action) {
        actionMapping.put(cls, action);
    }

    class ConnWatchDog implements Runnable {
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(port, 50);
                while (running) {
                    Socket s = ss.accept();
                    new Thread(new SocketAction(s)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Server.this.stop();
            }

        }
    }

    class SocketAction implements Runnable {
        Socket s;
        boolean run = true;
        long lastReceiveTime = System.currentTimeMillis();

        public SocketAction(Socket s) {
            this.s = s;
        }

        public void run() {
            while (running && run) {
                if (System.currentTimeMillis() - lastReceiveTime > receiveTimeDelay) {
                    overThis();
                } else {

                    try {
                        InputStream in = s.getInputStream();
                        if (in.available() > 0) {
                            //添加handShanker处理逻辑
                            addActionMap(HandShaker.class,new HandShakerAction(new HandShaker()));
                            //添加新闻推送PushInfo处理逻辑
                            addActionMap(PushInfo.class,new PushInfoAction(new PushInfo()));
                            ObjectInputStream ois = new ObjectInputStream(in);
                            Object obj = ois.readObject();
                            lastReceiveTime = System.currentTimeMillis();
                            System.out.println("接收：\t" + obj);
                            ObjectAction oa = actionMapping.get(obj.getClass());

                            if(oa instanceof HandShakerAction)    {
                                //处理handShaker，并把对应客户端占用的socket信息写入handShaker中
                                oa = new HandShakerAction((HandShaker)obj);

                                HandShaker handShaker = ((HandShaker) obj);
                                handShaker.setClientSocket(s);
                                oa.doAction(handShaker);
                            }else if(oa instanceof PushInfoAction)  {
                                //处理新闻推送PushInfo到对应的客户端（通过handShaker的信息）
                                oa = new PushInfoAction((PushInfo) obj);
                                PushInfo pushInfo = ((PushInfo) obj);
                                Socket certainSocket = ((Socket) oa.doAction(pushInfo));
                                if (pushInfo != null) {
                                    ObjectOutputStream oos = new ObjectOutputStream(certainSocket.getOutputStream());
                                    oos.writeObject(pushInfo);
                                    oos.flush();
                                }

                            } else   {
                                //处理心跳链接包
                                oa = oa == null ? new DefaultObjectAction() : oa;
                                Object out = oa.doAction(obj);
                                if (out != null) {
                                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                                    oos.writeObject(out);
                                    oos.flush();
                                }
                            }

                        } else {
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        overThis();
                    }
                }
            }
        }

        private void overThis() {
            if (run) run = false;
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("关闭：" + s.getRemoteSocketAddress());
        }

    }

    /**
     * MRP推送信息转换PushInfo
     * @param articlesJSON
     * @Param userJSON
     *
     */
    private PushInfo convertToPushInfo(final String articlesJSON,final String userJSON){
        if(articlesJSON==null||"".equals(articlesJSON)||userJSON==null||"".equals(userJSON)){
            throw new IllegalArgumentException("articles and duocaiInfo's json content can not be null or empty ");
        }
        final JSONObject duocaiMap = JSON.parseObject(userJSON);
        final PushInfo pushInfo = new PushInfo();
        final String initEditor = duocaiMap.getString("initEditor");
        final String password = duocaiMap.getString("password");
        final String userName = duocaiMap.getString("userName");
        if (initEditor == null ||"".equals(initEditor)||password==null||"".equals(password)||userName==null||"".equals(userName)){
            throw  new IllegalArgumentException("initEditor,password,userName in json string can not be null or empty");
        }
        pushInfo.setUserName(userName);
        pushInfo.setInitEditor(initEditor);
        pushInfo.setPassword(password);
        pushInfo.setArticlesJSON(articlesJSON);
        pushInfo.setCustomerInfoJSON(userJSON);
        return  pushInfo;
    }

    /**
     * 推送新闻列表
     * @param articlesJSON
     * @param userJSON
     */
    public void exportNews(String articlesJSON,String userJSON){
        final  PushInfo pushInfo = convertToPushInfo(articlesJSON,userJSON);
        try {
            Socket socket = new Socket(resourceBundle.getString("SERVER_IP"),Integer.parseInt(resourceBundle.getString("SERVER_PORT")));
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(pushInfo);
            System.out.println("发送：\t"+pushInfo);
            oos.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
