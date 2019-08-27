import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Chat Client class
 *
 * Class of Chat Client
 *
 * @author Sadiq and Rhythm Goel
 *
 * @version Nov 25, 2018
 *
 */

final class ChatClient {
    private ObjectInputStream socketInput;
    private ObjectOutputStream socketOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            if (e instanceof ConnectException) {
                //If a connection exception occurred, return an error message.
                System.out.println("Cannot connect to server.");
                return false;
            } else {
                e.printStackTrace();
            }
        }

        // Create your input and output streams
        try {
            socketInput = new ObjectInputStream(socket.getInputStream());
            socketOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            socketOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            socketOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults
        ChatClient client;
        if (args.length == 1) {
            client = new ChatClient("localhost", 1500, args[0]);
        } else if (args.length == 2) {
            client = new ChatClient("localhost", Integer.parseInt(args[1]), args[0]);
        } else if (args.length == 3) {
            client = new ChatClient(args[
                    2], Integer.parseInt(args[1]), args[0]);
        } else {
            client = new ChatClient("localhost", 1500, "Anonymous");
        }

        // Create your client and start it
        if (!client.start()) {
            //If the client is not created, halt the main method.
            return;
        }
        Scanner input = new Scanner(System.in);

        //Wait for input and send message
        while (true) {
            String msg = input.nextLine();
            if (msg.isEmpty() || msg.replaceAll("[\\s]", "").isEmpty()) {
                //If message is empty or blank(whitespaces)
                msg = "";
                client.sendMessage(new ChatMessage(msg, 0));
            } else if (msg.equals("/logout")) {
                try {
                    client.sendMessage(new ChatMessage(msg, 1));
                    client.socketInput.close();
                    client.socketOutput.close();
                    client.socket.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (msg.equals("/list")) {
                client.sendMessage(new ChatMessage(msg, 2));
            } else if (msg.contains("/msg") && msg.substring(0, 4).equals("/msg")) {
                client.sendMessage(new ChatMessage(msg, 3));
            } else if (msg.charAt(0) == '/') {
                System.out.printf("\"%s\" is an invalid command.\n", msg);
            } else {
                client.sendMessage(new ChatMessage(msg, 0));
            }

        }

    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {

        public void run() {
            System.out.println("Connection accepted " + socket.getInetAddress() + ":"
                    + socket.getPort());
            while (true) {
                try {
                    String msg = (String) socketInput.readObject();
                    System.out.print(msg);
                } catch (IOException | ClassNotFoundException e) {
                    if (e instanceof EOFException) {
                        //Handling disconnections of server
                        System.out.println("Server has disconnected.");
                        break;
                    } else if (e instanceof SocketException
                            && ((SocketException) e).getMessage().equals("Socket closed")) {
                        //Handling logging out of the server
                        System.out.println("You have logged out from the server.");
                        break;
                    }
                    e.printStackTrace();
                    break;
                }
            }

        }
    }
}
