package com.gengyun.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <类详细说明：推送服务器>
 *
 * @Author： Huanghai
 * @Version: 2016-12-27
 **/
public class PushServer extends ServerSocket {
    private static final int SERVER_PORT = 2013;

    public PushServer() throws IOException {
        super(SERVER_PORT);

        try {
            while (true) {
                Socket socket = accept();
                new CreateServerThread(socket);//当有请求时，启一个线程处理
            }
        } catch (IOException e) {
        } finally {
            close();
        }
    }

    //线程类
    class CreateServerThread extends Thread {
        private Socket client;
        private BufferedReader bufferedReader;
        private PrintWriter printWriter;

        public CreateServerThread(Socket s) throws IOException {
            client = s;

            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));

            printWriter = new PrintWriter(client.getOutputStream(), true);
            System.out.println("Client(" + getName() + ") come in...");

            start();
        }

        public void run() {
            try {
                String line = bufferedReader.readLine();

                while (null !=line) {
                    line = bufferedReader.readLine();
                }

                printWriter.close();
                bufferedReader.close();
                client.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new PushServer();
    }
}
