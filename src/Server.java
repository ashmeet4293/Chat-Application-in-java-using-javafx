
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private int port;
    Thread run, send, receive, manage;
    public long serverStartTime;
    InetAddress ip;

    Boolean running = false, raw = false, exits = false;
    DatagramSocket socket;

    public List<ServerClient> clients = new ArrayList<ServerClient>();

    public Server(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        try {
            ip = InetAddress.getLocalHost();
            System.out.println("Ip of server : " + ip.getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        run = new Thread(this, "Run Thread");
        run.start();
    }

    public void run() {
        running = true;
        serverStartTime = System.currentTimeMillis();
        System.out.println("Server Started Time : " + serverStartTime);
        System.out.println("Server Started on Port " + port);
        manageClients();
        receive();

        Scanner scanner = new Scanner(System.in);
        while (running) {
            String text = scanner.nextLine();
            if (!text.startsWith("/")) {
                sendToAll("/m/Server : " + text + "/e/");
            }
            text = text.substring(1);
            if (text.startsWith("raw")) {
                raw = !raw;
                if (raw) {
                    System.out.println("raw mode on ");
                } else {
                    System.out.println("raw mode off");
                }
            } else if (text.startsWith("online")) {
                System.out.println("^^^^^^^^^^^^^^^^^^");
                for (int i = 0; i < clients.size(); i++) {
                    ServerClient client = clients.get(i);
                    System.out.println("ID : " + client.getID() + " UserName : " + client.getName());
                }
                System.out.println("|| No of Clients : " + clients.size());
                System.out.println("====================");
            } else if (text.startsWith("time")) {
                System.out.println(serverStartTime);
            } else if (text.startsWith("kick")) {
                String clientName = text.split(" ")[1];
                boolean num = true;
                int id = 0;
                try {
                    id = Integer.parseInt(clientName);
//            num=true;
                } catch (Exception ex) {
                    num = false;
                }
                if (num) {
                    for (int i = 0; i < clients.size(); i++) {
                        ServerClient c = clients.get(i);
                        if (id == c.getID()) {
                            disconnect(id, true);
                            System.out.println("Client " + id + " has been kicked by Server");
                            break;
                        } else {
                            System.out.println("Client Doesnot Exists ! Check ID number");
                        }
                    }
                } else {
                    for (int i = 0; i < clients.size(); i++) {
                        ServerClient c = clients.get(i);
                        if (clientName.equals(c.name)) {
                            disconnect(c.getID(), true);
                            System.out.println("client " + clientName + " has been kicked by server");
                        } else {
                            System.out.println("client " + clientName + " doesnot exists ! Check username and try again !");
                        }
                    }
                }
            } else if (text.startsWith("help")) {
                System.out.println("===============================================================");
                System.out.println("/raw => raw mode");
                System.out.println("/online => To View online connected clients");
                System.out.println("/kick [username or userID] => To kick particular clients");
                System.out.println("/help => help menu");
                System.out.println("/quit => To quit Server ");
                System.out.println("===============================================================");

            } else if (text.startsWith("quit")) {
                quit();
            }
        }
    }

    public void manageClients() {
        manage = new Thread("Manage") {
            public void run() {
                System.out.println("Manage Thead is running");
            }
        };
        manage.start();
    }

    private void send(final byte[] data, final InetAddress address, final int port) {
        Thread send = new Thread("Server Send Thread") {
            public void run() {
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                try {
                    socket.send(packet);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        send.start();
    }

    private void send(String message, InetAddress address, int port) {
        message += "/e/";
        send(message.getBytes(), address, port);
    }

    public void receive() {
        receive = new Thread(" Server Receive Thread") {
            @Override
            public void run() {
                while (running) {
                    byte[] data = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    process(packet);
                    // clients.add(new ServerClient("Ashmeet",packet.getAddress(),packet.getPort(),50));
                    //System.out.println(clients.get(0).address.toString()+ " : "+clients.get(0).port);
                    // System.out.println("Clients Size : "+clients.size());
                    String string = new String(packet.getData());
                    string.substring(3);
                    string = string.split("/e/")[0];
                    System.out.println("received String " + string);// shows the String that are Recived VIA socket 
                }
            }
        };
        receive.start();
    }

    private void process(DatagramPacket packet) {
        String string = new String(packet.getData());
        if (raw) {
            System.out.println(string);
        }
        if (string.startsWith("/c/")) {
            int id = UniqueIdentifier.getIdentifier();
            System.out.println("Client ID : " + id);
            String name = string.split("/c/|/e/")[1];
          

            System.out.println("client connected " + name + " || clients no : " + clients.size());
            String ID = "/c/" + id;
            send(ID, packet.getAddress(), packet.getPort());
        } else if (string.startsWith("/m/")) {
            sendToAll(string);
        } else if (string.startsWith("/t/")) {
            sendToAll(string);
        } else if (string.startsWith("/d/")) {
            String id = string.split("/d/|/e/")[1];
            System.out.println("Disconnected Message received ");
            disconnect(Integer.parseInt(id), true);
        } else if (string.startsWith("/s/")) {
            sendToAll(string);
            System.out.println("Server send the seen message request ");
        } else {
            System.out.println("No Connetion packet ReceiveD");
        }
    }

    private void sendToAll(String message) {
        if (message.startsWith("/m/")) {
            String text = message.split("/m/|/e/")[1];
            System.out.println(text);
        }
        for (int i = 0; i < clients.size(); i++) {
            ServerClient client = clients.get(i);
            send(message.getBytes(), client.address, client.port);
        }
    }

    public void disconnect(int id, boolean status) {
        ServerClient c = null;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getID() == id) {
                c = clients.get(i);
                clients.remove(i);
                break;
            }
        }
        String message = "";
        if (status) {
            message = "username: " + c.name.toString() + " || ID: " + c.getID() + " || IP: " + c.address.toString() + " || port: " + c.port + " || Status: disconnected";
            String string = c.name.toString() + "/i/" + c.getID();
//            System.out.println(string);
            sendToAll("/d/" + string + "/e/");
        } else {
            message = "username: " + c.name.toString() + " || ID: " + c.getID() + " || IP: " + c.address.toString() + " || port: " + c.port + " || Status: Timed Out";
        }
        System.out.println(message);
    }

    private void quit() {
        for (int i = 0; i < clients.size(); i++) {
            disconnect(clients.get(i).getID(), true);
        }
        running = false;
        try {
            socket.close();
            System.out.println("Server Down ! ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

 }
