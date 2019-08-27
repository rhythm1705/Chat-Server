

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 * Chat Server class
 *
 * Class of Chat Server
 *
 * @author Sadiq and Rhythm Goel
 *
 * @version Nov 25, 2018
 *
 */

final class ChatServer {
    private static int uniqueId = 0;
    private final ArrayList<ClientThread> clients = new ArrayList<>();
    private final int port;
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
    private boolean filtration; // Specifies whether the server needs to filter messages or not
    private static String file; // File pathname for filter words


    private ChatServer(int port, boolean filtration) {
        this.port = port;
        this.filtration = filtration;
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(time.format(new Date()) + " Server waiting for Clients on port " + port + ".");
            while (true) {
                Socket socket = serverSocket.accept();
                Runnable runnable = new ClientThread(socket, uniqueId++);
                Thread thread = new Thread(runnable);
                clients.add((ClientThread) runnable);
                thread.start();
                String newClientUsername = clients.get(findIndexOfUsername(uniqueId - 1)).getUsername();
                System.out.println(time.format(new Date()) + " " + newClientUsername
                        + " has joined the server.");
                System.out.println(time.format(new Date()) + " Server waiting for Clients on port " + port + ".");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized int findIndexOfUsername(int id) {
        for (int i = 0; i < clients.size(); i++) {
            if (id == clients.get(i).getId()) {
                return i;
            }
        }
        return -1;
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        ChatServer server;
        if (args.length == 1) {
            server = new ChatServer(Integer.parseInt(args[0]), false);
        } else if (args.length == 2) {
            server = new ChatServer(Integer.parseInt(args[0]), true);
            file = args[1];
        } else {
            server = new ChatServer(1500, false);
        }

        server.start();
    }

    /*
     * This method will broadcast messages to all clients
     */
    private synchronized void broadcast(String msg) {
        if (filtration) {
            ChatFilter chatFilter = new ChatFilter(file);
            msg = chatFilter.filter(msg);
        }
        msg = time.format(new Date()) + " " + msg;
        Thread t = new Thread(new SendAll(msg));
        t.start();
    }

    private synchronized void directMessage(String message, String username) {
        if (filtration) {
            ChatFilter chatFilter = new ChatFilter(file);
            message = chatFilter.filter(message);
        }
        message = time.format(new Date()) + " " + message;
        int i;
        for (i = 0; i < clients.size(); i++) {
            if (username.equals(clients.get(i).getUsername())) {
                clients.get(i).writeMessage("> " + message + "\n");
                System.out.println(message);
            }
        }

    }

    private final class SendAll implements Runnable {
        private String msg;

        private SendAll(String msg) {
            this.msg = msg;
        }

        public synchronized void run() {
            //Print message in the server
            System.out.println(this.msg);

            //Broadcast messages to every user
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).writeMessage("> " + this.msg + "\n")) {
                } else {
                    break;
                }
            }
        }
    }


    /*
     * This method will remove a specific client from clients ArrayList
     */
    private synchronized void remove(int id) {
        Thread t = new Thread(new removeClient(id));
        t.start();
    }

    private final class removeClient implements Runnable {
        private int id;

        private removeClient(int id) {
            this.id = id;
        }

        public synchronized void run() {
            int index = findIndexOfUsername(id);
            if (index == -1) {
                return;
            } else {
                clients.remove(index);
            }
        }
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream socketInput;
        ObjectOutputStream socketOutput;
        int id;
        String username;
        ChatMessage chatMessage;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                socketOutput = new ObjectOutputStream(socket.getOutputStream());
                socketInput = new ObjectInputStream(socket.getInputStream());
                username = (String) socketInput.readObject();
                for (int i = 0; i < clients.size(); i++) {
                    if (username.equals(clients.get(i).getUsername())) {
                        username += id;
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    chatMessage = (ChatMessage) socketInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    if (e instanceof EOFException || e instanceof SocketException) {
                        //Close socket of client if EOFException or SocketException is thrown
                        try {
                            socketInput.close();
                            socketOutput.close();
                            socket.close();
                            remove(id);
                            System.out.println(time.format(new Date()) + " " + username
                                    + " terminated the program inappropriately.");
                        } catch (IOException f) {
                            f.printStackTrace();
                        }
                        break;
                    }
                    e.printStackTrace();
                }

                if (chatMessage.getMsgType() == 0) {
                    if (chatMessage.getMsg().isEmpty()) {
                        writeMessage("You cannot send a blank message.\n");
                    } else {
                        String msg = username + ": " + chatMessage.getMsg();
                        broadcast(msg);
                    }
                } else if (chatMessage.getMsgType() == 2) {
                    System.out.println(time.format(new Date()) + " " + this.username
                    + " executed the command \"/list\"");
                    for (int i = 0; i < clients.size(); i++) {
                        if (!clients.get(i).getUsername().equals(this.username)) {
                            writeMessage(clients.get(i).getUsername() + "\n");
                        }
                    }
                } else if (chatMessage.getMsgType() == 3) {
                    Scanner input = new Scanner(chatMessage.getMsg());
                    ArrayList<String> directM = new ArrayList<>();
                    int k = 0;
                    while (input.hasNext() && k < 2) {
                        directM.add(input.next());
                        k++;
                    }
                    if (input.hasNext()) {
                        directM.add(input.nextLine());
                    }

                    String[] directMessageCommand = new String[3];
                    directMessageCommand = directM.toArray(directMessageCommand);

                    boolean foundUser = false;
                    boolean NotSameUser = false;
                    if (directM.size() == 3) {
                        for (int i = 0; i < clients.size(); i++) {
                            if (directMessageCommand[1].equals(clients.get(i).getUsername())) {
                                if (!directMessageCommand[1].equals(this.username)) {
                                    foundUser = true;
                                    NotSameUser = true;
                                    directMessageCommand[2] = this.username + " -> " + directMessageCommand[1]
                                            + ": " + directMessageCommand[2];
                                    writeMessage("> " + time.format(new Date()) + " " + directMessageCommand[2]
                                            + "\n");
                                    directMessage(directMessageCommand[2], directMessageCommand[1]);
                                } else {
                                    foundUser = true;
                                    break;
                                }
                            }
                        }

                        if (!foundUser) {
                            writeMessage("Username does not exist.\n");
                        } else if (!NotSameUser) {
                            writeMessage("You cannot message yourself!\n");
                        }

                    } else {
                        writeMessage("You need to specify the username AND the message.\n");
                    }

                } else {
                    close();
                    break;
                }

            }
        }


        public String getUsername() {
            return username;
        }

        public int getId() {
            return id;
        }

        private synchronized boolean writeMessage(String msg) {
            if (!socket.isClosed() && socket.isConnected()) {
                try {
                    socketOutput.writeObject(msg);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return false;
            }
        }

        private synchronized void close() {
            try {
                socketInput.close();
                socketOutput.close();
                socket.close();
                remove(id);
                System.out.println(time.format(new Date()) + " " + username + " has logged out successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
