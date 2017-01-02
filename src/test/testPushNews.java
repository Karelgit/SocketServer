import com.gengyun.model.PushInfo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.Socket;

/**
 * <类的详细说明：>
 *
 * @Author: Huanghai
 * @Version: 2016/12/30
 **/
public class testPushNews {
    public static void main(String[] args) {
        PushInfo pushInfo = new PushInfo();
        pushInfo.setUserName("duocai");
        pushInfo.setPassword("123456");
        pushInfo.setInitEditor("send from server!!!");


        try {
            Class clazz = Class.forName("com.gengyun.model.PushInfo");
            Field field = clazz.getField("initEditor");
            System.out.println(field !=null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        //链接到服务器
        try {
            Socket socket = new Socket("127.0.0.1",2013);
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
