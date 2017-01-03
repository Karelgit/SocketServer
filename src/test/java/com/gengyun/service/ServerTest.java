package com.gengyun.service;

import NewsPusherModule.server.Server;

/**
 * <类详细说明>
 *
 * @Author： HuangHai
 * @Version: 2017-01-03
 **/
public class ServerTest {

    public static void main(String[] args) {
        Server server =new Server(65432);
        System.out.println(server.startPushServer());
    }
}
