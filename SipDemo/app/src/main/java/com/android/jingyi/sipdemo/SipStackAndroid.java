package com.android.jingyi.sipdemo;

import android.gov.nist.javax.sip.SipStackExt;
import android.gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import android.javax.sip.ClientTransaction;
import android.javax.sip.DialogTerminatedEvent;
import android.javax.sip.IOExceptionEvent;
import android.javax.sip.ListeningPoint;
import android.javax.sip.PeerUnavailableException;
import android.javax.sip.RequestEvent;
import android.javax.sip.ResponseEvent;
import android.javax.sip.SipFactory;
import android.javax.sip.SipListener;
import android.javax.sip.SipProvider;
import android.javax.sip.SipStack;
import android.javax.sip.TimeoutEvent;
import android.javax.sip.TransactionTerminatedEvent;
import android.javax.sip.address.Address;
import android.javax.sip.address.AddressFactory;
import android.javax.sip.address.URI;
import android.javax.sip.header.ExpiresHeader;
import android.javax.sip.header.HeaderFactory;
import android.javax.sip.header.ViaHeader;
import android.javax.sip.message.MessageFactory;
import android.javax.sip.message.Request;
import android.javax.sip.message.Response;

import java.util.ArrayList;
import java.util.Properties;

public class SipStackAndroid implements SipListener {

    private static SipStackAndroid instance = null;
    public static SipStack sipStack;
    public static SipProvider sipProvider;
    public static HeaderFactory headerFactory;
    public static AddressFactory addressFactory;
    public static MessageFactory messageFactory;
    public static SipFactory sipFactory;
    public static ListeningPoint udpListeningPoint;
    public static String localIp;
    public static int localPort = 5080;
    public static String localEndpoint = localIp+":"+localPort;
    public static String transport = "udp";
    public static String remoteIp = "23.23.228.238";
    public static int remotePort = 5080;
    public static String remoteEndpoint = remoteIp+":"+remotePort;
    public static String sipUserName;
    public String sipPassword;
    protected SipStackAndroid() {
        initialize();
    }

    public static SipStackAndroid getInstance() {
        if (instance == null) {
            instance = new SipStackAndroid();
        }
        return instance;
    }

    private static void initialize() {
        localIp = "getIPAddress";

        localEndpoint = localIp + ":" + localPort;

        remoteEndpoint = remoteIp + ":" + remotePort;

        sipStack = null;

        sipFactory = SipFactory.getInstance();

        sipFactory.setPathName("android.gov.nist");

        Properties properties = new Properties();
        properties.setProperty("javaxx.sip.OUTBOUND_PROXY", remoteEndpoint + "/"+ transport);properties.setProperty("javaxx.sip.STACK_NAME", "androidSip");

        try {
            // Create SipStack object

            sipStack = sipFactory.createSipStack(properties);
            System.out.println("createSipStack " + sipStack);

        } catch (PeerUnavailableException e) {
            e.printStackTrace();

            System.err.println(e.getMessage());

            System.exit(0);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();

            addressFactory = sipFactory.createAddressFactory();
            
            messageFactory = sipFactory.createMessageFactory();

            udpListeningPoint = sipStack.createListeningPoint(localIp,localPort, transport);

            sipProvider = sipStack.createSipProvider(udpListeningPoint);

            sipProvider.addSipListener(SipStackAndroid.getInstance());

            // this.send_register();
        } catch (Exception e) {
            System.out.println("Creating Listener Points");

            System.out.println(e.getMessage());

            e.printStackTrace();
        }
    }

    private void send_register() {
        try {
            System.out.println();

            SipStackAndroid.getInstance();

            AddressFactory addressFactory = SipStackAndroid.addressFactory;

            SipStackAndroid.getInstance();

            SipProvider sipProvider = SipStackAndroid.sipProvider;

            SipStackAndroid.getInstance();

            MessageFactory messageFactory = SipStackAndroid.messageFactory;

            SipStackAndroid.getInstance();

            HeaderFactory headerFactory = SipStackAndroid.headerFactory;

// Create addresses and via header for the request

            Address fromAddress = addressFactory.createAddress("sip:"
                    + SipStackAndroid.sipUserName + "@" + SipStackAndroid.remoteIp);
            fromAddress.setDisplayName(SipStackAndroid.sipUserName);
            Address toAddress = addressFactory.createAddress("sip:"
                    + SipStackAndroid.sipUserName + "@" + SipStackAndroid.remoteIp);
            toAddress.setDisplayName(SipStackAndroid.sipUserName);
            Address contactAddress = createContactAddress();

            ArrayList viaHeaders = createViaHeader();
            URI requestURI = addressFactory.createAddress("sip:" + SipStackAndroid.remoteEndpoint).getURI();
            // Build the request
            final Request request = messageFactory.createRequest(requestURI,Request.REGISTER, sipProvider.getNewCallId(),
                    headerFactory.createCSeqHeader(1l, Request.REGISTER),
                    headerFactory.createFromHeader(fromAddress, "c3ff411e"),
                    headerFactory.createToHeader(toAddress, null), viaHeaders,
                    headerFactory.createMaxForwardsHeader(70));
            // Add the contact header
            request.addHeader(headerFactory.createContactHeader(contactAddress));
            ExpiresHeader eh = headerFactory.createExpiresHeader(300);
            request.addHeader(eh);
            // Print the request
            System.out.println(request.toString());

// Send the request --- triggers an IOException
            // sipProvider.sendRequest(request);

            ClientTransaction transaction = sipProvider
                    .getNewClientTransaction(request);

// Send the request statefully, through the client transaction.
            transaction.sendRequest();
        } catch (Exception e) {
            
        }

        
    }

    private Address createContactAddress() {
        try {
            SipStackAndroid.getInstance();
            return SipStackAndroid.addressFactory.createAddress("sip:" +
                    SipStackAndroid.sipUserName + "@"
                    + SipStackAndroid.localEndpoint + ";transport=udp"
                    + ";registering_acc=23_23_228_238");
        } catch ( Exception e) {
            return null;
        }
    }

    private ArrayList createViaHeader() {
        ArrayList viaHeaders = new ArrayList();
        ViaHeader myViaHeader;

        try {
            SipStackAndroid.getInstance();
            myViaHeader = SipStackAndroid.headerFactory.createViaHeader(SipStackAndroid.localIp,
                    SipStackAndroid.localPort,
                    SipStackAndroid.transport, null);
            myViaHeader.setRPort();
            viaHeaders.add(myViaHeader);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return viaHeaders;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {

    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = (Response) responseEvent.getResponse();
        ClientTransaction tid = responseEvent.getClientTransaction();
        System.out.println(response.getStatusCode());

        if (response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED
                || response.getStatusCode() == Response.UNAUTHORIZED) {
            AuthenticationHelper authenticationHelper = ((SipStackExt) sipStack)
                    .getAuthenticationHelper(new AccountManagerImpl("", "", ""),
                            headerFactory);

            try {
                ClientTransaction inviteTid = authenticationHelper
                        .handleChallenge(response, tid, sipProvider, 5);
                inviteTid.sendRequest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {

    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {

    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }
}
