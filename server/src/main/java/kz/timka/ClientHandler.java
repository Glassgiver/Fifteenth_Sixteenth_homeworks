package kz.timka;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String username;


    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if(msg.startsWith("/login ")) {
                        // /login Bob@gmail.com 111
                        String[] tokens = msg.split("\\s+");
                        if(tokens.length != 3) {
                            sendMessage("Server: Incorrect command");
                            continue;
                        }
                        String login = tokens[1];
                        String password = tokens[2];
                        String nick = server.getAuthenticationProvider()
                                .getUsernameByLoginAndPassword(login, password);
                        if(nick == null) {
                            sendMessage("/login_failed Incorrect login/password");
                            continue;
                        }

                        if(server.isUserOnline(nick)) {
                            sendMessage("/login_failed this username is already in use");
                            continue;
                        }
                        username = nick;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        break;
                    }
                }
                while (true) {
                    String msg = in.readUTF();
                    // /p Bob Hello
                    if(msg.startsWith("/")) {
                        executeCmd(msg);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

    }


    public void executeCmd(String msg) throws IOException {
        if(msg.startsWith("/p ")) {
            String[] tokens = msg.split("\\s+", 3);
            server.sendPrivateMsg(this, tokens[1], tokens[2]);
            return;
        }
        if(msg.startsWith("/change_nick ")){
            String[] tokens = msg.split("\\s", 2);
            String oldUsername = username;
            this.username = tokens[1];
            server.changeUsername(this, oldUsername, tokens[1]);
            return;
        }
        if(msg.startsWith("/who_am_i")){
            sendMessage("Server: Your current username: " + this.username);
            return;
        }

    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            server.saveTextInHistory(msg);
        }catch (IOException e) {
            disconnect();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUsername() {
        return username;
    }
}
