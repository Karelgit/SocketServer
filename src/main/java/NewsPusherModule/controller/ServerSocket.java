package NewsPusherModule.controller;

import NewsPusherModule.server.Server;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ResourceBundle;

/**
 * <类详细说明>
 *
 * @Author： HuangHai
 * @Version: 2017-01-03
 **/
@RestController
@RequestMapping("mrpPushServer")
public class ServerSocket {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("conf");
    private Server server = new Server(Integer.parseInt(resourceBundle.getString("SERVER_PORT")));

    @RequestMapping("/start")
    public boolean startServer()    {
        return server.startPushServer();
    }
}
