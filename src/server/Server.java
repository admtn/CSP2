package server;

import util.*;

import java.net.Socket;
import javax.net.ServerSocketFactory;
import javax.net.ssl.*;

import entities.*;

import java.io.*;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_PORT = 12345;
    private static final String CONNECTION_TYPE = "TLS";
    private static final String TRUST_STORE_PATH = "../Certificates/Server/servertruststore";
    private static final String KEY_STORE_PATH = "../Certificates/Server/serverkeystore";
    private static final char[] STORE_PASSWORD = "serverpw".toCharArray();

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private static int activeConnections = 0;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.executorService = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        LOGGER.info("Starting server...");

        int portNumber = DEFAULT_PORT;
        if (args.length >= 1) {
            try {
                portNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid port number provided. Using default port: {0}", DEFAULT_PORT);
            }
        }

        try {
            ServerSocketFactory serverSocketFactory = getServerSocketFactory(CONNECTION_TYPE);
            if (serverSocketFactory == null) {
                LOGGER.severe("Failed to get ServerSocketFactory. Exiting.");
                return;
            }

            ServerSocket serverSocket = serverSocketFactory.createServerSocket(portNumber);
            if (serverSocket instanceof SSLServerSocket sslServerSocket) {
                sslServerSocket.setNeedClientAuth(true);
            }

            Server server = new Server(serverSocket);
            LOGGER.info("Server has started.");

            server.start();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot start server due to error: {0}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String connectionType) {
        if ("TLS".equalsIgnoreCase(connectionType)) {
            try {
                // Load KeyStore and TrustStore
                KeyStore keyStore = KeyStore.getInstance("JKS");
                KeyStore trustStore = KeyStore.getInstance("JKS");

                try (FileInputStream keyStoreStream = new FileInputStream(KEY_STORE_PATH);
                     FileInputStream trustStoreStream = new FileInputStream(TRUST_STORE_PATH)) {

                    keyStore.load(keyStoreStream, STORE_PASSWORD);
                    trustStore.load(trustStoreStream, STORE_PASSWORD);
                }

                // Initialize KeyManager and TrustManager
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keyStore, STORE_PASSWORD);

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                trustManagerFactory.init(trustStore);

                // Initialize SSLContext
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

                return sslContext.getServerSocketFactory();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize SSL context: {0}", e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                synchronized (Server.class) {
                    activeConnections++;
                }
                LOGGER.log(Level.INFO, "Client connected. Active connections: {0}", activeConnections);
                executorService.execute(new ClientHandler(clientSocket));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error accepting client connection: {0}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private ClientCommandHandler inputManager;
        private Person person;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket clientSocket = this.socket;
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                if (clientSocket instanceof SSLSocket sslSocket) {
                    SSLSession session = sslSocket.getSession();
                    X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                    String subject = cert.getSubjectX500Principal().getName();
                    LOGGER.info("Client connected: " + subject);

                    inputManager = new ClientCommandHandler();
                    person = inputManager.getPerson(cert);
                }

                String clientMsg;
                while ((clientMsg = in.readLine()) != null) {
                    if (clientMsg.isEmpty() || "quit".equalsIgnoreCase(clientMsg)) {
                        break;
                    }

                    String response = inputManager.handleClientInput(clientMsg, person);
                    out.println(response);
                    out.println("ENDOFMSG");

                    if ("Write information".equals(response)) {
                        String information = in.readLine();
                        String[] msgParts = clientMsg.split(" ");
                        if (msgParts.length > 1) {
                            response = inputManager.writeInformation(msgParts[1], information, person);
                            out.println(response);
                            out.println("ENDOFMSG");
                        } else {
                            out.println("Invalid command format.");
                            out.println("ENDOFMSG");
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Client connection error: {0}", e.getMessage());
                e.printStackTrace();
            } finally {
                synchronized (Server.class) {
                    activeConnections--;
                }
                LOGGER.log(Level.INFO, "Client disconnected. Active connections: {0}", activeConnections);
                if (inputManager != null) {
                    try {
                        inputManager.save();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error saving InputManager state: {0}", e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
