package com.zimbra.cs.mailclient;

import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Connection;
import com.zimbra.cs.mailclient.util.TraceInputStream;
import com.zimbra.cs.mailclient.util.TraceOutputStream;

public abstract class MailConnection {
    protected MailConfig config;
    protected Socket socket;
    protected SSLSocketFactory sslSocketFactory;
    protected ClientAuthenticator authenticator;
    protected TraceInputStream traceIn;
    protected TraceOutputStream traceOut;
    protected MailInputStream mailIn;
    protected MailOutputStream mailOut;
    protected boolean closed;
    protected State state;

    public static enum State {
        NON_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }

    private static final String LOGIN = "LOGIN";
    
    protected MailConnection() {}

    protected MailConnection(MailConfig config) {
        this.config = config;
    }

    public static MailConnection getInstance(MailConfig config) {
        if (config instanceof ImapConfig) {
            return new ImapConnection((ImapConfig) config);
        } else if (config instanceof Pop3Config) {
            return new Pop3Connection((Pop3Config) config);
        } else {
            throw new IllegalArgumentException(
                "Unsupported protocol: " + config.getProtocol());
        }
    }

    public void connect() throws IOException {
        socket = config.createSocket();
        initStreams(new BufferedInputStream(socket.getInputStream()),
                     new BufferedOutputStream(socket.getOutputStream()));
        processGreeting();
    }

    protected void initStreams(InputStream is, OutputStream os)
            throws IOException {
        if (config.isTrace()) {
            is = traceIn = config.getTraceInputStream(is);
            os = traceOut = config.getTraceOutputStream(os);
        }
        mailIn = getMailInputStream(is);
        mailOut = getMailInputStream(os);
    }

    protected abstract void processGreeting() throws IOException;
    protected abstract void sendLogin() throws IOException;
    protected abstract void sendAuthenticate(boolean ir) throws IOException;
    protected abstract void sendStartTLS() throws IOException;
    protected abstract MailInputStream getMailInputStream(InputStream is);
    protected abstract MailOutputStream getMailInputStream(OutputStream os);

    public abstract void logout() throws IOException;

    public void login() throws IOException {
        String user = config.getAuthenticationId();
        String pass = config.getPassword();
        if (user == null || pass == null) {
            throw new IllegalStateException(
                "Missing required login username or password");
        }
        sendLogin();
    }
    
    public void authenticate(boolean ir) throws LoginException, IOException {
        String mech = config.getMechanism();
        if (mech == null || mech.equalsIgnoreCase(LOGIN)) {
            login();
            return;
        }
        authenticator = config.createAuthenticator();
        authenticator.initialize();
        sendAuthenticate(ir);
        if (authenticator.isEncryptionEnabled()) {
            initStreams(authenticator.getUnwrappedInputStream(socket.getInputStream()),
                        authenticator.getWrappedOutputStream(socket.getOutputStream()));
        }
    }

    public void authenticate() throws LoginException, IOException {
        authenticate(false);
    }

    protected void processContinuation(String s) throws IOException {
        byte[] response = authenticator.evaluateChallenge(decodeBase64(s));
        if (response != null) {
            mailOut.writeLine(encodeBase64(response));
            mailOut.flush();
        }
    }

    private static byte[] decodeBase64(String s) throws SaslException {
        try {
            return Base64.decodeBase64(s.getBytes("us-ascii"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    protected static String encodeBase64(byte[] b) {
        try {
            return new String(Base64.encodeBase64(b), "us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    public void startTLS() throws IOException {
        sendStartTLS();
        SSLSocket sock = config.createSSLSocket(socket);
        try {
            sock.startHandshake();
            mailIn = new MailInputStream(sock.getInputStream());
            mailOut = new MailOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public String getNegotiatedQop() {
        return authenticator != null ?
            authenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }

    public void setTraceEnabled(boolean enabled) {
        if (traceIn != null) {
            traceIn.setEnabled(enabled);
        }
        if (traceOut != null) {
            traceOut.setEnabled(enabled);
        }
    }

    public MailInputStream getInputStream() {
        return mailIn;
    }

    public MailOutputStream getOutputStream() {
        return mailOut;
    }

    public MailConfig getConfig() {
        return config;
    }
    
    public void close() {
        if (closed) return;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (authenticator != null) {
            try {
                authenticator.dispose();
            } catch (SaslException e) {
                e.printStackTrace();
            }
        }
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
