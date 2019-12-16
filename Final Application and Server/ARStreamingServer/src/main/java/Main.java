import org.apache.commons.net.telnet.TelnetClient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        TelnetClient telnet = new TelnetClient();

        telnet.connect("localhost", 5554);
        OutputStream os = telnet.getOutputStream();

        String fileContents = "";

        try {
            File file = new File("/home/damirtha/.emulator_console_auth_token");
            Scanner scanner = new Scanner(file);
            fileContents = "auth " + scanner.next() + "\n";
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }

        os.write(fileContents.getBytes());
        os.flush();

        System.out.println("User Authenticated");

        ServerSocket serverSocket = new ServerSocket(5000);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client Connected: "+clientSocket.getRemoteSocketAddress());

            new Thread(() -> {
                try (
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    String received;
                    while ((received = in.readLine()) != null) {
                        os.write(("sensor set " + received + "\n").getBytes());
                        os.flush();
                    }
                    System.out.println("Client disconnected");
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }).run();

        }
    }
}