package kz.timka;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> list;
    private AuthenticationProvider authenticationProvider;
    public Server(int port) {
        this.port = port;
        this.list = new ArrayList<>();
        this.authenticationProvider = new InMemoryAuthProvider();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключение клиента...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String msg) throws IOException {
        for(ClientHandler clientHandler : list) {
            clientHandler.sendMessage(msg);
            //saveTextInHistory(msg);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        list.add(clientHandler);
        sendClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        list.remove(clientHandler);
        sendClientList();
    }

    public boolean isUserOnline(String username) {
        for(ClientHandler clientHandler : list) {
            if(clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void sendPrivateMsg(ClientHandler sender, String receiver, String msg) throws IOException {
        for(ClientHandler c : list) {
            if(c.getUsername().equals(receiver)) {
                c.sendMessage("From: " + sender.getUsername() + " Message: " + msg);
                sender.sendMessage("Receiver: " + receiver + " Message: " + msg);
                saveTextInHistory("From" + sender.getUsername() + "to " + receiver + ": " + msg);
                return;
            }
        }
        sender.sendMessage("Unable to send message to " + receiver);
    }

    public void sendClientList() {
        StringBuilder builder = new StringBuilder("/clients_list ");
        for(ClientHandler c : list) {
            builder.append(c.getUsername()).append(" ");
        }
        builder.setLength(builder.length() - 1);
        // /clients_list Bob Alex John
        String clientList = builder.toString();
        for(ClientHandler c : list) {
            c.sendMessage(clientList);
        }
    }



    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void changeUsername(ClientHandler sender, String oldUsername, String newUsername) {
        if(authenticationProvider.changeUsername(oldUsername, newUsername)){
            unsubscribe(sender);
            subscribe(sender);
            sender.sendMessage("Your username was successfully changed!");
        }
    }

    public void saveTextInHistory(String message) throws IOException {
        try (FileOutputStream out = new FileOutputStream("text_history.txt", true)) {
            byte[] arr = message.getBytes(StandardCharsets.UTF_8);
            out.write(arr);
            out.write(10);
        } catch (IOException e) {
            System.out.println("Произошла ошибка при записи в файл: " + e.getMessage());
            throw e;
        }
    }
}
