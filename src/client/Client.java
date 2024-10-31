package client;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * SSL Client that connects to a server, authenticates using certificates, and communicates securely.
 */
public class Client {

    // Configuration Constants
    private static final String USAGE = "USAGE: java client <host> <port>";
    private static final String GOVERNMENT_STORE_PATH = "../Certificates/Govt/";
    private static final String CLIENT_STORE_PATH = "../Certificates/Client/";
    private static final String GOVERNMENT_TRUSTSTORE = "govtruststore";
    private static final String CLIENT_TRUSTSTORE = "clienttruststore";
    private static final String GOVERNMENT_TRUSTSTORE_PASSWORD = "govpass";
    private static final String CLIENT_TRUSTSTORE_PASSWORD = "clientpw";

    public static void main(String[] args) {
        // Validate and parse command-line arguments
        if (args.length < 2) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String host = args[0];
        int port = parsePort(args[1]);

        // Read user credentials
        UserCredentials credentials = promptUserCredentials();

        // Initialize SSL Context
        SSLContext sslContext = initializeSSLContext(credentials.getUsername(), credentials.getPassword());
        if (sslContext == null) {
            System.err.println("SSL context initialization failed. Exiting.");
            System.exit(1);
        }

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Establish SSL connection
        try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
            System.out.println("\nSocket before handshake:\n" + sslSocket + "\n");
            sslSocket.startHandshake(); // Initiate SSL handshake

            SSLSession session = sslSocket.getSession();
            displaySessionInfo(session, sslSocket);

            // Setup I/O streams
            try (PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                 BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {

                // Send username to the server
                System.out.println("Sending username...\n");
                out.println(credentials.getUsername());

                // Start communication loop
                communicateWithServer(consoleReader, in, out);
            }

            System.out.println("Secure connection closed.");
        } catch (IOException e) {
            System.err.println("I/O error during communication: " + e.getMessage());
        }
    }

    /**
     * Parses the port number from the command-line argument.
     *
     * @param portArg The port number as a string.
     * @return The port number as an integer.
     */
    private static int parsePort(String portArg) {
        try {
            return Integer.parseInt(portArg);
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer.");
            System.out.println(USAGE);
            System.exit(1);
            return -1; // Unreachable, but required by the compiler
        }
    }

    /**
     * Prompts the user for username and password.
     *
     * @return A UserCredentials object containing the entered username and password.
     */
    private static UserCredentials promptUserCredentials() {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String username = null;
        String password = null;

        while (true) {
            try {
                System.out.print("Enter Username: ");
                username = consoleReader.readLine();

                password = readPassword(consoleReader);

                if (username != null && !username.trim().isEmpty()
                        && password != null && !password.trim().isEmpty()) {
                    break;
                } else {
                    System.err.println("Username and password cannot be empty. Please try again.");
                }
            } catch (IOException e) {
                System.err.println("Error reading input. Please try again.");
            }
        }

        return new UserCredentials(username.trim(), password.trim());
    }

    /**
     * Reads the password securely from the console.
     *
     * @param consoleReader The BufferedReader to read input from.
     * @return The entered password as a string.
     * @throws IOException If an I/O error occurs.
     */
    private static String readPassword(BufferedReader consoleReader) throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("Enter Password: ");
            return new String(passwordChars);
        } else {
            System.out.print("Enter Password: ");
            return consoleReader.readLine();
        }
    }

    /**
     * Initializes the SSLContext based on the user's credentials.
     *
     * @param username The username of the client.
     * @param password The password for the client's keystore.
     * @return The initialized SSLContext, or null if initialization fails.
     */
    private static SSLContext initializeSSLContext(String username, String password) {
        try {
            // Load KeyStore and TrustStore
            KeyStore keyStore = loadKeyStore(username, password);
            KeyStore trustStore = loadTrustStore(username);

            // Initialize KeyManagerFactory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, password.toCharArray());

            // Initialize TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return sslContext;
        } catch (Exception e) {
            System.err.println("Failed to initialize SSL context: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the client's KeyStore based on the username.
     *
     * @param username The username of the client.
     * @param password The password for the KeyStore.
     * @return The loaded KeyStore.
     * @throws Exception If loading the KeyStore fails.
     */
    private static KeyStore loadKeyStore(String username, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        String keyStorePath = determineKeyStorePath(username);
        try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreStream, password.toCharArray());
        }
        return keyStore;
    }

    /**
     * Loads the TrustStore based on the username.
     *
     * @param username The username of the client.
     * @return The loaded TrustStore.
     * @throws Exception If loading the TrustStore fails.
     */
    private static KeyStore loadTrustStore(String username) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        String trustStorePath = determineTrustStorePath(username);
        String trustStorePassword = determineTrustStorePassword(username);
        try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
        }
        return trustStore;
    }

    /**
     * Determines the KeyStore path based on the username.
     *
     * @param username The username of the client.
     * @return The path to the KeyStore file.
     */
    private static String determineKeyStorePath(String username) {
        if ("govks".equalsIgnoreCase(username)) {
            return GOVERNMENT_STORE_PATH + username;
        } else {
            return CLIENT_STORE_PATH + username;
        }
    }

    /**
     * Determines the TrustStore path based on the username.
     *
     * @param username The username of the client.
     * @return The path to the TrustStore file.
     */
    private static String determineTrustStorePath(String username) {
        if ("govks".equalsIgnoreCase(username)) {
            return GOVERNMENT_STORE_PATH + GOVERNMENT_TRUSTSTORE;
        } else {
            return CLIENT_STORE_PATH + CLIENT_TRUSTSTORE;
        }
    }

    /**
     * Determines the TrustStore password based on the username.
     *
     * @param username The username of the client.
     * @return The TrustStore password.
     */
    private static String determineTrustStorePassword(String username) {
        if ("govks".equalsIgnoreCase(username)) {
            return GOVERNMENT_TRUSTSTORE_PASSWORD;
        } else {
            return CLIENT_TRUSTSTORE_PASSWORD;
        }
    }

    /**
     * Displays information about the SSL session and server certificate.
     *
     * @param session    The SSLSession object.
     * @param sslSocket  The SSLSocket used for the connection.
     */
    private static void displaySessionInfo(SSLSession session, SSLSocket sslSocket) {
        try {
            String cipherSuite = session.getCipherSuite();
            System.out.println("Cipher Suite: " + cipherSuite);

            X509Certificate serverCert = (X509Certificate) session.getPeerCertificates()[0];
            String subjectDN = serverCert.getSubjectX500Principal().getName();
            System.out.println("Server Certificate Subject DN: " + subjectDN + "\n");

            System.out.println("Socket after handshake:\n" + sslSocket + "\n");
            System.out.println("Secure connection established.\n");
        } catch (SSLPeerUnverifiedException e) {
            System.err.println("Could not verify peer: " + e.getMessage());
        }
    }

    /**
     * Handles the communication loop between the client and the server.
     *
     * @param consoleReader The BufferedReader for reading user input.
     * @param in            The BufferedReader for reading server responses.
     * @param out           The PrintWriter for sending messages to the server.
     * @throws IOException If an I/O error occurs.
     */
    private static void communicateWithServer(BufferedReader consoleReader, BufferedReader in, PrintWriter out) throws IOException {
        while (true) {
            // Read and display server messages
            String serverResponse;
            while ((serverResponse = in.readLine()) != null && !serverResponse.equals("ENDOFMSG")) {
                System.out.println(serverResponse);
            }

            // Prompt user for input
            System.out.print("> ");
            String userInput = consoleReader.readLine();
            if (userInput == null) {
                break; // EOF reached
            }

            out.println(userInput); // Send user input to the server

            if ("quit".equalsIgnoreCase(userInput.trim())) {
                break; // Exit the loop if the user types "quit"
            }
        }
    }

    /**
     * Represents user credentials consisting of a username and password.
     */
    private static class UserCredentials {
        private final String username;
        private final String password;

        public UserCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
