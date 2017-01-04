package com.gengyun.service;

import NewsPusherModule.entity.PushInfo;
import NewsPusherModule.server.PushService;
import NewsPusherModule.server.Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
        pushInfo.setCompanyId("001");
        pushInfo.setUserName("zhangchao001");
        pushInfo.setPassword("1234567;");
        pushInfo.setInitEditor("contetn:黔东南州举行龙舟比赛！");
        pushInfo.setArticlesJSON("articleJson");

        //链接到服务器
        try {
            Socket socket = new Socket("127.0.0.1",65432);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(pushInfo);
            System.out.println("发送：\t"+pushInfo);
            oos.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //新闻发送
//        String articlesJson = "[\n" +
//                "{\n" +
//                "    \"author\": \"杨昌鼎\",\n" +
//                "    \"content\": \"多彩贵州网讯(本网记者 杨昌鼎)12月22\",\n" +
//                "    \"sourceName\": \"多彩贵州网\",\n" +
//                "    \"title\": \"贵州将打造肉牛养殖深加工综合产业园 3年引进\",\n" +
//                "    \"templateId\":\"100000330\",\n" +
//                "\"channelId\": \"3000000000000000\"\n" +
//                "}\n]";
//        String userJSON = "{\n" +
//                "    \"initEditor\": \"100000125\",\n" +
//                "    \"password\": \"1234567;\",\n" +
//                "    \"userName\": \"zhangchao001\",\n" +
//                "}\n";
//        PushService.exportNews(articlesJson, userJSON);
    }



}
