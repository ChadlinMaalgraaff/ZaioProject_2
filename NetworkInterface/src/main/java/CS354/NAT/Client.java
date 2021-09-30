package CS354.NAT;

import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Client implements Runnable {

    JFrame home;
    JTextField messageField;
    DataInputStream inputStream;
    DataOutputStream outputStream;
    JTextField destinationField;

    public static Socket socket;
    private static String arg;
    private static int serverPort;
    private static String randomIp;
    private static DatagramSocket dataSoc = null;
    private static InetAddress serverAddress;
    private static String serverIpAddress, serverMacAddress, clientIpAddress, clientMacAddress;
    private static String echoReqMessage, echoRepMessage;

    // @Override
    public void run() {
        // Client connects to the Server through TCP.
        // Client continuously listens for incoming messages from the Server

        byte[] boxMsg = null;
        int boxMsgLength = 0;
        String boxMsgType = "";
        String[] boxMsgPartitions;
        boolean loop = true;

        echoReqMessage = "8 Echo Request";
        echoRepMessage = "0 Echo Reply";

        try {

            // dataSoc = new DatagramSocket();
            // byte[] buf = "TESTING".getBytes();

            // // Send
            // InetAddress iAdd = InetAddress.getByName("255.255.255.255");
            // DatagramPacket dataPack = new DatagramPacket(buf, buf.length, iAdd, 8002);
            // System.out.println("check");
            // dataSoc.send(dataPack);
            // String data = new String(dataPack.getData());
            // System.out.println("\n==> DATA PACKET CONTENT: " + data);

            // // Receive
            // byte[] receiveData = new byte["TESTING".length()];
            // DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            // dataSoc.receive(receivePacket);
            // System.out.println("Discovery response received!" + receivePacket.getAddress() + ":" + receivePacket.getPort());
            // System.out.println("Past receive");


            socket = new Socket("127.0.0.1"/*"100.78.213.61"*/, 8000);
        } catch (Exception e) {
            System.out.println("\n=====> Error in Client.java <=====");
        }

        try {
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("\n==> CLIENT SUCCESFULLY CONNECTED (Waiting to receive packets) <==\n");

            while (loop) {
                System.out.println("\n==> LISTENING FOR MESSAGES FROM SERVER\n");

                // First get the box message length
                boxMsgLength = inputStream.read();
                // System.out.println("box message length " + boxMsg);

                // Initialize box message using the box message length,
                // then read in the sent box message from the server,
                // via a DataInputStream
                boxMsg = new byte[boxMsgLength];
                inputStream.read(boxMsg);

                // Partition the received box message, and store each piece
                // of data in a separate cell in the boxMsgPartitions array
                boxMsgPartitions = new String(boxMsg).split("\\|");
                System.out.println("\n=== BOX MESSAGE CONTENT ===\n" + new String(boxMsg) + "\n===========================\n");

                // Retrieve the box message type
                boxMsgType = boxMsgPartitions[3];

                if (boxMsgType.equals("ACK")) {
                    // Acknowleges a successful message receipt
                    System.out.println("\n==> SUCCESSFUL PACKET ACKNOWLEDGEMENT");
                } else if (boxMsgType.equals("REP")) {
                    // Displays the Address from which a response was received
                    System.out.println("\n==> PING/ECHO REPLY");
                    System.out.println("\n==> PING <==\nREPLY FROM: " + boxMsgPartitions[0] + "\n . RETURN TO LISTENING" + "\n============\n");
                } else if (boxMsgType.equals("REQ")) {
                    System.out.println("\n==> ECHO/PING REQUEST");

                    // Here we check if our Echo was succesfully sent and received.
                    // We use a hard coded, general, echo message. Then, using the
                    // getCRC32 function, we are able to conclude whether
                    // the echo/ping request was successful.
                    byte[] echoBuf  = ("8 Echo Request").getBytes();
                    long ipCheckSum = getCRC32(echoBuf);
                    byte[] echoCheck = new byte[2];

                    // Checksum added to header
                    echoCheck[0] = (byte) (ipCheckSum >> 8);
                    echoCheck[1] = (byte) (ipCheckSum);

                    // We compare our calculated checksum to the expected checksum
                    // and derive whether a successful ping/echo was made.
                    String check = new String(echoCheck);
                    boolean echoSuccess = check.equals(boxMsgPartitions[6]);

                    System.out.println("Calculated Checksum : " + new String(echoCheck) + " Src Checksum : " + boxMsgPartitions[6]);

                    if (echoSuccess) {
                        // Here the message, to be sent after a successfull echo/ping, will be constructed

                        String clientIpAddress = boxMsgPartitions[1];
                        String destIp = boxMsgPartitions[0];
                        String ips = boxMsgPartitions[0] + boxMsgPartitions[1];
                        byte[] ipbuf  = ips.getBytes();
                        long ipHeaderCheck = getCRC32(ipbuf);

                        byte[] rep = echoRepMessage.getBytes();
                        // Echo checksum is calculated
                        long echoCheckSum = getCRC32(rep);

                        byte[] toSend = echoReplyPacket(clientIpAddress, destIp, ipHeaderCheck, "REP", "0", "0",
                                            echoCheckSum, "0", "1");

                        System.out.println("\n===> TO SEND PACKET <===\n TSP CONTENT: " + new String(toSend) + "\n========================");

                        try {
                            // First send length, then the echo reply message
                            outputStream.write(toSend.length);
                            outputStream.write(toSend);
                        } catch (IOException e) {
                            System.out.println("\n==> ERROR SENDING PING REPLY FROM CLIENT");
                        }

                    } else {
                        System.out.println("\n>>> UNSUCCESSFUL PING");
                    }

                } else if (boxMsgType.equals("ERR")) {
                    // If an error occured, an error package will be sent to the Client.
                    // This error package contains a code pertaining top the type of error:
                    // 0 - Destination Network Unreachable (external/internal trying to connect to external)
                    // 1 - Destination Host Network Unreachable (external/internal trying to connect to internal)
                    System.out.println("\n==> ICMP-ERROR PACKAGE RECEVIED");

                    int Type = Integer.parseInt(boxMsgPartitions[4]);
                    int Code = Integer.parseInt(boxMsgPartitions[5]);

                    if (Type == 3) {
                        System.out.println("\n==> ERROR: DESTINATION UNREACHABLE");
                        if (Code == 0) {
                            System.out.println("\n==> ERROR: DESTINATION NETWORK UNREACHABLE");
                        } else {
                            System.out.println("\n==> ERROR: DESTINATION HOST UNREACHABLE");
                        }
                    } else {
                        System.out.println("\n==> ERROR ON LINE 145");
                    }

                } else if (boxMsgType.equals("LEV")) {
                    // Informs client of their expired lease
                    System.out.println("\n==> CLIENT LEASE TIME EXPIRED");
                    break;
                } else if (boxMsgType.equals("MSG")) {

                    // If the message src, boxMsgPartitions[0], is not my ip address and the destination, boxMsgPartitions[1],
                    // is my ip address, then the message will be sent to me.
                    if (!boxMsgPartitions[0].equals(clientIpAddress) && (boxMsgPartitions[1].equals(clientIpAddress))) {
                        // Here we display the incomming message and we verify whether the message
                        // receieved is complete and correct

                        System.out.println("\n=> MESSAGE: " + boxMsgPartitions[4] + "\n   FROM " + boxMsgPartitions[0] + "\n");

                        byte[] msg = boxMsgPartitions[4].getBytes();

                        // Create checksum
                        long checksum = getCRC32(msg);
                        byte[] check = new byte[2];
                        check[0] = (byte) (checksum >> 8);
                        check[1] = (byte) (checksum);

                        String srcCheck = boxMsgPartitions[2].trim();
                        String msgCh = new String(check).trim();

                        if (!(msgCh.equals(srcCheck))) {
                            // If our calculated checksum does not correspond to the src's checksum,
                            // the notify user of such.
                            System.out.println("\n===>ERROR: CHECKSUMS DO NOT CORRESPOND");
                        } else {

                            // send acknowledgement
                            String ack = boxMsgPartitions[1] + "|" + boxMsgPartitions[0] + "|" + boxMsgPartitions[2] + "|ACK|" + boxMsgPartitions[4];
                            byte[] ackBuf = ack.getBytes();
                            outputStream.write(ackBuf.length);
                            outputStream.write(ackBuf);

                            System.out.println("\n=> ACKNOWLEDGEMENT SENT TO NATBOX");
                        }

                    } else {
                        System.out.println("\n==> ERROR: LINE 189");
                    }

                } else {
                    System.out.println("\n==> ERROR: UNKNOWN BOX MESSAGE TYPE");
                    break;
                }

            }

        } catch (Exception ex) {
            System.out.println("\n==> CLOSING CLIENT SOCKET IN EXCEPTION <==");
            System.out.println(ex);
        } finally {
            home.setVisible(false);
            home.dispose();
        }
    }

    private static String DHCP_Client(String status) {
        String hostIp = "127.0.0.1"/*"100.78.213.61"*/;
        int port = 8888;
        String proceed = "";

        try
        {
            dataSoc = new DatagramSocket();
            byte[] buf = new byte[256];

            // dataSoc = new DatagramSocket();
            // byte[] buf = "TESTING".getBytes();

            // // Send
            // InetAddress iAdd = InetAddress.getByName("100.64.24.126");
            // DatagramPacket dataPack = new DatagramPacket(buf, buf.length, iAdd, 8002);
            // System.out.println("check");
            // dataSoc.send(dataPack);
            // String data = new String(dataPack.getData());
            // System.out.println("\n==> DATA PACKET CONTENT: " + data);

            // // Receive
            // byte[] receiveData = new byte["TESTING".length()];
            // DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            // dataSoc.receive(receivePacket);
            // System.out.println("Discovery response received!" + receivePacket.getAddress() + ":" + receivePacket.getPort());
            // System.out.println("Past receive");

            // Set the chosen server status
            if (status == "internal") {
                buf[0] = 'd';
            } else {
                buf[0] = 'e';
            }

            InetAddress iAdd = InetAddress.getByName(hostIp);
            DatagramPacket dataPack = new DatagramPacket(buf, buf.length, iAdd, port);

            // Send the Server Type choice of the client to the server
            dataSoc.send(dataPack);

            System.out.println("===> DHCP CLIENT STARTED <===");
            while (true) {

                byte[] dataBuf = new byte[256];
                dataPack = new DatagramPacket(dataBuf, dataBuf.length, iAdd, port);

                // We continuously receive feedback from the server
                dataSoc.receive(dataPack);

                String data = new String(dataPack.getData());
                System.out.println("\n==> RECV PACKET CONTENT: " + data);


                if (dataBuf[0] == 'f') {

                    System.out.println("\n==> NO ADDRESSES AVAILABLE IN THE POOL <==");
                    // We set the proceed variable to false, and inform the DHCP Server
                    // of such

                    serverPort = dataPack.getPort();
                    serverAddress = dataPack.getAddress();

                    /// Prompt the DHCP Server to print out an "EMPTY POOL" error and
                    //  discontinue communication with this DHCP Client
                    dataBuf[0] = 'r';
                    dataPack = new DatagramPacket(dataBuf, dataBuf.length, serverAddress, serverPort);
                    dataSoc.send(dataPack);
                    proceed = "no";
                    break;

                } else if (dataBuf[0] == 'd') {
                    System.out.println("\n=== SERVER FOUND. REQUEST MAC ADDRESS ===");
                    // If DHCP Client is INTERNAL ('d'), then request the DHCP Server's Mac Address

                    serverPort = dataPack.getPort();
                    serverAddress = dataPack.getAddress();

                    /* ------- execute ARP ---------*/

                    System.out.println("\n==> REQUEST MAC ADDRESS");

                    // Request DHCP Server's Mac Address
                    dataBuf[0] = 'm';
                    dataPack = new DatagramPacket(dataBuf, dataBuf.length, serverAddress, serverPort);
                    dataSoc.send(dataPack);

                    System.out.println("\n==> REQUEST SENT");

                } else if (dataBuf[0] == 'm') {
                    System.out.println("\n==> MAC ADDR RECEIVED");
                    // Received DHCP Server's Mac Address.

                    // Extract the DHCP Server's Ip and Mac Address from the response data
                    String[] split = data.split("\\|");
                    serverIpAddress = split[1]; //split[1];
                    serverMacAddress = split[2]; //split[2];

                    System.out.println("\n==> SERVER MAC ADDRESS: " + serverMacAddress + "\n==> SERVER IP ADDRESS: " + serverIpAddress);

                    // Acknowledge receipt of DHCP Server's Mac Address
                    dataBuf[0] = 'n';
                    dataPack = new DatagramPacket(dataBuf, dataBuf.length, serverAddress, serverPort);
                    dataSoc.send(dataPack);

                } else if (dataBuf[0] == 'o') {
                    System.out.println("\n==> OFFER RECEIVED");
                    // Received Ip and Mac Addresses, to be assigned to this Client, from DHCP Server

                    // Extract the DHCP Client's assigned Ip and Mac Address from the response data
                    String[] split = data.split("\\|");
                    clientIpAddress = split[1];
                    clientMacAddress = split[2];

                    System.out.println("\n==> CLIENT ASSIGNED MAC Address: " + clientMacAddress +
                                        "\n==> CLIENT ASSIGNED IP Address: " + clientIpAddress + "\n");

                    boolean dataComplete = ( !serverMacAddress.isEmpty() )&&( !serverIpAddress.isEmpty() )
                                                &&( !clientMacAddress.isEmpty() )&&( !clientIpAddress.isEmpty());

                    // Send DHCP Server an acknowledgement that all the data was correctly received
                    if (dataComplete) {
                        dataBuf = new String("a|internal|" + clientIpAddress + "|" + hostIp + "|"+ clientMacAddress).getBytes();
                        dataPack = new DatagramPacket(dataBuf, dataBuf.length, serverAddress, serverPort);

                        dataSoc.send(dataPack);
                    }

                    break;
                } else if (dataBuf[0] == 'c') {
                    System.out.println("\n==> EXTERNAL CLIENT CAN CONNECT <==");
                    // Received confirmation, from DHCP Server, that this EXTERNAL ('e') Client can connect

                    // Extract the External Client's assigned Ip and Mac Address from the response data, as
                    // well as the random Ip address given by the DHCP Server
                    String[] split = data.split("\\|");
                    randomIp = split[1];
                    clientIpAddress = split[2];
                    clientMacAddress = split[3];

                    System.out.println("\n== EXTERNAL ==\nRandom Ip: " + randomIp + "\nClient Ip: " + clientIpAddress + "\nClient Mac: " + clientMacAddress );

                    proceed = "yes";
                   break;

                } else {
                    System.out.println("\n==> UNRECODGIZED COMMAND. PLEASE TRY AGAIN <==");
                }

            }

        } catch (IOException e) {
            System.out.println(e.toString());
        } finally {
            if (dataSoc != null) {
                dataSoc.close();
            }
        }

        return proceed;
    }

    private void homeGUI(String clientDetails) {

        home = new JFrame(clientDetails);
        home.setSize(100, 110);
        home.setLayout(null);

        JButton messageButton = new JButton("MESSAGE");
        messageButton.setBounds(0, 0, 100, 30);

        JButton pingButton = new JButton("PING");
        pingButton.setBounds(0, 40, 100, 30);

        home.add(messageButton);
        home.add(pingButton);

        messageButton.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent e){
                System.out.println("MESSAGING");
                messageGUI(clientIpAddress);
            }
        });

        pingButton.addActionListener(new ActionListener() {

            // @Override
            public void actionPerformed(ActionEvent arg0){
                System.out.println("PINGING");
                pingGUI(clientIpAddress);
            }
        });

    }

    private void pingGUI(String clientDetails) {

        JFrame frame = new JFrame(clientDetails);
        JPanel panel  = new JPanel(new BorderLayout(5,5));
        JPanel labels  = new JPanel(new GridLayout(0,1,2,2));

        labels.add(new JLabel("Destination (Ip Address)", SwingConstants.TRAILING));
        panel.add(labels, BorderLayout.LINE_START);

        JPanel input = new JPanel(new GridLayout(0,1,2,2));

        messageField = new JTextField(20);
        messageField.setEditable(true);
        input.add(messageField);
        panel.add(input, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel);

        String destIp = messageField.getText();

        if (destIp.equals("")) {
            System.out.println("\n==> INVALID DESTINATION ADDRESS");
        } else {

            boolean validIp = false;
            String[] split = destIp.split(":");

            if (split.length != 2) {
                validIp = isValidIp(destIp);
            } else {
                validIp = isValidIp(split[0]);
            }

            if (validIp != false) {
                // Here we are sending the echo request

                String ipAddress = clientIpAddress + destIp;
                byte[] ip = ipAddress.getBytes();
                long ipCheckSum = getCRC32(ip);

                byte[] buf = echoReqMessage.getBytes();
                // Echo check sum is constructed using the echo request message
                long echoChecksum = getCRC32(buf);

                // src | dest | checkSum | packetType | Type | Code | checksum | identifier | seq-number
                byte[] toSend = echoReqPacket(clientIpAddress, destIp, ipCheckSum, "REQ", "8", "0",
                                    echoChecksum, "77", "0");

                System.out.println("\n ==> PING SENDING PACKET CONTAINS: " + new String(toSend));

                try {
                    /* first send size of byte array */
                    outputStream.write(new String(toSend).length());

                    /* now send packet */
                    outputStream.write(toSend);
                } catch (IOException e) {
                    System.out.println("\n==> PINGING ERROR");
                }

            } else {
                System.out.println("\n==> INVALID DESTINATION IP ADDRESS");
            }

        }

    }

    private void messageGUI(String clientDetails) {
        JFrame frame = new JFrame(clientDetails);
        JPanel panel  = new JPanel(new BorderLayout(5,5));
        JPanel labels  = new JPanel(new GridLayout(0,1,2,2));

        labels.add(new JLabel("MESSSAGE", SwingConstants.TRAILING));
        labels.add(new JLabel("DESTINATION (Ip ADDRESS)", SwingConstants.TRAILING));
        panel.add(labels, BorderLayout.LINE_START);

        JPanel controls = new JPanel(new GridLayout(0,1,2,2));

        messageField= new JTextField(20);
        messageField.setEditable(true);

        destinationField = new JTextField(20);
        destinationField.setEditable(true);

        controls.add(messageField);
        controls.add(destinationField);

        panel.add(controls, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(
            frame, panel);

        String message = messageField.getText();
        System.out.println("\n==> MESSAGE IS " + message);
        String destination = destinationField.getText();
        System.out.println("\n==> DESTINATION IS " + destination);

        if (destination.equals("")) {
            System.out.println("\n==> INVALID DESTINATION ADDRESS");
        } else {

            boolean validIP = false;
            String[] split = destination.split(":");
            if (split.length==2) {
                validIP = isValidIp(split[0]);
            } else {
                validIP = isValidIp(destination);
            }

            if (validIP == false) {

                System.out.println("\n==> INVALID DESTINATION ADDRESS");

            } else {

                byte[] payload = message.getBytes();

                long chksum = getCRC32(payload);

                byte[] pay = ("|MSG|" + message).getBytes();

                byte[] toSend = messagePacket(clientIpAddress, destination, chksum, pay);
                System.out.println("\n==> (MessageGui) TO SEND: PACKET CONTAINS " + new String(toSend));

                try {
                    outputStream.write(new String(toSend).length());
                    outputStream.write(toSend);
                } catch (IOException e) {
                    System.out.println("\n==> ERROR SENDING A MESSAGE FROM CLIENT");
                }

            }

        }

    }

    /**
     * message packet format: [src] | [recv] | [checksum] | [packetType] | [payload]
     **/
    private static byte[] messagePacket(String src, String dest, long checksum, byte[] data) {

        byte[] s = src.getBytes();
        byte[] d = dest.getBytes();

        byte del = '|';
        byte[] packet = new byte[s.length + d.length + data.length + 5];
        int j = 0;

        for (int i = 0; i < s.length; i++) {
            packet[i] = s[j];
            j++;
        }
        packet[s.length] = del;
        j = 0;
        for (int i = s.length + 1; i < s.length + d.length + 1; i++) {
            packet[i] = d[j];
            j++;
        }
        packet[s.length + d.length + 1] = del;
        j = 0;

        packet[s.length + d.length + 2] = (byte) (checksum >> 8);
        packet[s.length + d.length + 3] = (byte) (checksum);

        for (int i = s.length + d.length + 5; i < packet.length; i++) {
            packet[i] = data[j];
            j++;
        }

        return packet;
    }

    private static boolean isValidIp(String ip) {
        String partitions[] = ip.split("[.]");
        boolean isValid = true;

        for (String partition : partitions) {
            int i = Integer.parseInt(partition);

            if (( i < 0 ) || ( partition.length() > 3 ) || ( i > 255 )) {
                isValid = false;
            }

            if (( partition.length() > 1 ) && ( i == 0 )) {
                isValid = false;
            }

            if (( partition.length() > 1 ) && ( i != 0 ) && ( partition.charAt(0) == '0' )) {
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * echo request packet format: src | dest | checkSum | packetType | Type | Code | checksum | identifier | seq-number
     **/
    private static byte[] echoReqPacket(String srcIp, String destIp, long ipCheckSum,
        String pacType, String Type, String Code, long echCheckSum, String Identifier, String seqNum) {

        byte[] src = (srcIp + "|").getBytes();
        byte[] dest = (destIp + "|").getBytes();
        byte[] packetType = ("|" + pacType + "|").getBytes();
        byte[] type = (Type + "|").getBytes();
        byte[] code = (Code + "|").getBytes();
        byte[] identifier = ("|" + Identifier + "|").getBytes();
        byte[] sequenceNumber = seqNum.getBytes();

        int len = src.length + dest.length + packetType.length + type.length + code.length +
                    identifier.length + sequenceNumber.length + 2 + 2;
        byte[] packet = new byte[len];

        int j = 0;

        for (int i = 0; i < src.length; i++) {
            packet[j] = src[i];
            j++;
        }

        for (int i = 0; i < dest.length; i++) {
            packet[j] = dest[i];
            j++;
        }

        packet[j] = (byte) (ipCheckSum >> 8);
        j++;
        packet[j] = (byte) (ipCheckSum);
        j++;

        for (int i = 0; i < packetType.length; i++) {
            packet[j] = packetType[i];
            j++;
        }

        for (int i = 0; i < type.length; i++) {
            packet[j] = type[i];
            j++;
        }

        for (int i = 0; i < code.length; i++) {
            packet[j] = code[i];
            j++;
        }

        packet[j] = (byte) (echCheckSum >> 8);
        j++;
        packet[j] = (byte) (echCheckSum);
        j++;

        for (int i = 0; i < identifier.length; i++) {
            packet[j] = identifier[i];
            j++;
        }

        for (int i = 0; i < sequenceNumber.length; i++) {
            packet[j] = sequenceNumber[i];
            j++;
        }

        return packet;
    }

    private static long getCRC32(byte[] raw) {
		Checksum chkSum = new CRC32();
		chkSum.update(raw, 0, raw.length);
		return chkSum.getValue();
	}

    /**
     * echo reply packetformat: src | dest | checkSum | packetType | Type | Code | checksum | identifier | seq-number
     **/
    private static byte[] echoReplyPacket(String srcIp, String destIp, long ipCheckSum, String pacType,
                             String Type, String Code, long echoCheckSum, String Identifier, String seqNum) {

        byte[] src = (srcIp + "|").getBytes();
        byte[] dest = (destIp + "|").getBytes();
        byte[] packetType = ("|" + pacType + "|").getBytes();
        byte[] type = (Type + "|").getBytes();
        byte[] code = (Code + "|").getBytes();
        byte[] identifier = ("|" + Identifier + "|").getBytes();
        byte[] sequenceNumber = seqNum.getBytes();

        int len = src.length + dest.length + packetType.length + type.length + code.length +
                    identifier.length + sequenceNumber.length + 2 + 2;
        byte[] packet = new byte[len];

        int j = 0;

        for (int i = 0; i < src.length; i++) {
            packet[j] = src[i];
            j++;
        }

        for (int i = 0; i < dest.length; i++) {
            packet[j] = dest[i];
            j++;
        }

        packet[j] = (byte) (ipCheckSum >> 8);
        j++;
        packet[j] = (byte) (ipCheckSum);
        j++;

        for (int i = 0; i < packetType.length; i++) {
            packet[j] = packetType[i];
            j++;
        }

        for (int i = 0; i < type.length; i++) {
            packet[j] = type[i];
            j++;
        }

        for (int i = 0; i < code.length; i++) {
            packet[j] = code[i];
            j++;
        }

        packet[j] = (byte) (echoCheckSum >> 8);
        j++;
        packet[j] = (byte) (echoCheckSum);
        j++;

        for (int i = 0; i < identifier.length; i++) {
            packet[j] = identifier[i];
            j++;
        }

        for (int i = 0; i < sequenceNumber.length; i++) {
            packet[j] = sequenceNumber[i];
            j++;
        }

        return packet;
    }

    public static void main(String args[]) {
        arg = args[0];

        String status = "";
        String can_Continue = "";
        if (arg.toLowerCase().equals("i")) {
            can_Continue = DHCP_Client("internal");
            status = "internal";
        } else if (arg.toLowerCase().equals("e")) {
            can_Continue = DHCP_Client("external");
            status = "external";
        } else {
            System.out.println("==> Option Not Valid");
            System.exit(0);
        }

        if (can_Continue.equals("no")) {
            System.out.println("\n==> THERE ARE NO AVAILABLE IP ADDRESSES");
            System.exit(0);
        } else {
            Client client = new Client();
            client.homeGUI("CLIENT : " + clientIpAddress + "   STATUS : "+ status);
            client.home.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.home.setVisible(true);
            Thread thd = new Thread(client);
            thd.start();
        }
        return;
    }
}
