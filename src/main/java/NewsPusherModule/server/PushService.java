package NewsPusherModule.server;

import NewsPusherModule.entity.PushInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ResourceBundle;

/**
 * <类详细说明:Mrp推送文章>
 *
 * @Author： HuangHai
 * @Version: 2017-01-03
 **/
public class PushService {
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");

    /**
     * MRP推送信息转换PushInfo
     * @param articlesJSON
     * @Param userJSON
     *
     */
    private static PushInfo convertToPushInfo(final String articlesJSON, final String userJSON){
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
    public static boolean exportNews(String articlesJSON,String userJSON){
        final  PushInfo pushInfo = convertToPushInfo(articlesJSON,userJSON);
        try {
            Socket socket = new Socket(resourceBundle.getString("SERVER_IP"),Integer.parseInt(resourceBundle.getString("SERVER_PORT")));
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(pushInfo);
            System.out.println("发送：\t"+pushInfo);
            oos.flush();
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
