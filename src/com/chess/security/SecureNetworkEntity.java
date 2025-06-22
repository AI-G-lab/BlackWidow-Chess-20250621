package com.chess.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;

public abstract class SecureNetworkEntity extends Thread {

    protected ObjectOutputStream outputStream;
    protected ObjectInputStream inputStream;
    protected Socket connectionHandle;
    protected Object receivedMessage;
    
    // Security enhancements
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();
    private static final AtomicInteger CONNECTION_COUNT = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 10;
    private static final long MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB limit
    
    static {
        // Whitelist allowed classes for deserialization
        ALLOWED_CLASSES.add("com.chess.engine.classic.board.Board");
        ALLOWED_CLASSES.add("com.chess.engine.classic.board.Move");
        ALLOWED_CLASSES.add("java.lang.String");
    }

    SecureNetworkEntity(final String name) {
        super(name);
        
        // Check connection limit
        if (CONNECTION_COUNT.incrementAndGet() > MAX_CONNECTIONS) {
            CONNECTION_COUNT.decrementAndGet();
            throw new SecurityException("Maximum number of connections exceeded");
        }
    }

    public abstract void run();

    public void getStreams() throws IOException {
        outputStream = new ObjectOutputStream(connectionHandle.getOutputStream());
        outputStream.flush();
        inputStream = new SecureObjectInputStream(connectionHandle.getInputStream());
    }

    public void closeConnection() {
        CONNECTION_COUNT.decrementAndGet();
        
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (connectionHandle != null) {
                connectionHandle.close();
                System.out.println("Connection closed with " + 
                    connectionHandle.getInetAddress().getHostName());
            }
        } catch (IOException e) {
            System.err.println("Problems experienced when closing connection: " + e.getMessage());
        }
    }

    public void processIncomingData() throws IOException {
        do {
            try {
                receivedMessage = inputStream.readObject();
                
                // Validate received message
                if (!isValidMessage(receivedMessage)) {
                    System.err.println("Invalid message received, ignoring");
                    continue;
                }
                
                if (receivedMessage instanceof Move) {
                    final Move m = (Move) receivedMessage;
                    handleMove(m);
                } else if (receivedMessage instanceof Board) {
                    final Board b = (Board) receivedMessage;
                    handleBoard(b);
                } else if (receivedMessage instanceof String) {
                    final String message = (String) receivedMessage;
                    handleStringMessage(sanitizeString(message));
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Unknown class received from " + 
                    connectionHandle.getInetAddress().getHostName());
            } catch (SecurityException e) {
                System.err.println("Security violation: " + e.getMessage());
                break; // Close connection on security violation
            }
        } while (receivedMessage != null);
    }
    
    // Abstract methods for subclasses to implement
    protected abstract void handleMove(Move move);
    protected abstract void handleBoard(Board board);
    protected abstract void handleStringMessage(String message);

    public void sendData(final Object objToSend) {
        if (!isValidMessage(objToSend)) {
            throw new SecurityException("Attempt to send invalid object");
        }
        
        try {
            outputStream.writeObject(objToSend);
            outputStream.flush();
        } catch (IOException ioe) {
            System.err.println("Error sending data: " + ioe.getMessage());
        }
    }
    
    private boolean isValidMessage(final Object message) {
        if (message == null) {
            return false;
        }
        
        final String className = message.getClass().getName();
        return ALLOWED_CLASSES.contains(className);
    }
    
    private String sanitizeString(final String input) {
        if (input == null) {
            return "";
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "")
                   .substring(0, Math.min(input.length(), 1000)); // Limit length
    }
    
    // Custom ObjectInputStream that validates classes before deserialization
    private static class SecureObjectInputStream extends ObjectInputStream {
        
        public SecureObjectInputStream(java.io.InputStream in) throws IOException {
            super(in);
        }
        
        @Override
        protected Class<?> resolveClass(java.io.ObjectStreamClass desc) 
                throws IOException, ClassNotFoundException {
            
            final String className = desc.getName();
            
            if (!ALLOWED_CLASSES.contains(className)) {
                throw new SecurityException("Unauthorized deserialization attempt: " + className);
            }
            
            return super.resolveClass(desc);
        }
    }
}
