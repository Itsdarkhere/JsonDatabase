package Json.Database;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    public static void main(String[] args) {

        new Server().start();

    }

    public void start() {

        ExecutorService executor = Executors.newFixedThreadPool(100);
        Gson gson = new Gson();
        String address = "127.0.0.1";
        int port = 23451;
        boolean on = true;

        try {
            System.out.println("Server started!");
            ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address));

            //while "on" server is accepting new connections
            while (on) {

                //accept a new client connection
                Socket socket = server.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream());

                //receive input from user
                String jsonString = input.readUTF();
                Input inputInForm = gson.fromJson(jsonString, Input.class);

                Engine engine = new Engine(socket, inputInForm, gson);

                //giving incoming tasks to threads via executorService
                executor.submit(() -> {

                    try {
                        engine.run();

                        while (true) {

                            if (engine.isSocketAlive()) {
                                executor.shutdown();
                                socket.close();
                                server.close();
                                break;

                            }

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });

                //checking if exit command has been called
                if (executor.isShutdown()) {
                    on = false;
                    server.close();
                }


            }

        } catch (SocketException | UnknownHostException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}

