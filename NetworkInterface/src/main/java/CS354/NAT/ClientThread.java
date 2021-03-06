package CS354.NAT;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ClientThread extends Thread{
	Socket client;
	String fake_ip;
	DataInputStream inData;
	DataOutputStream outData;
	byte[] receive = null;

	public ClientThread(Socket soc, String ip) {
		client = soc;
		fake_ip = ip;
	}

	public void run() {
		try {
			inData = new DataInputStream(client.getInputStream());
			outData = new DataOutputStream(client.getOutputStream());
			synchronized (NAT.clients) {
				NAT.clients.add(fake_ip);
				NAT.activeClients.put(fake_ip, outData);
			}
			int packetSize = 0;
			while (true) {
				packetSize = inData.read();
				receive = new byte[packetSize];
				inData.read(receive);
				String[] clientMessage = new String(receive).split("\\|");
				preparePacket(clientMessage);
			}
		} catch (Exception e) {
			clientRemove(this.fake_ip);
		}
	}

    /**
     * Checksum function that uses standard java libraries
     * @param raw - Byte array that contains the message to be hashed
     * @return checksum value as long
     */
    private static long getCRC32(byte[] raw) {
        Checksum chkSum = new CRC32();
        chkSum.update(raw, 0, raw.length);
        return chkSum.getValue();
    }

    /**
     * Function called to remove current client when thread is terminated
     * @param candidateKey - The assigned IP of the client to be remove
     */
	private static synchronized void clientRemove(String candidateKey) {
		String ip = candidateKey.split(":")[0];
		ClientInfo candidateClient = NAT.table.get(candidateKey);
		String status = candidateClient.getStatus();
		if (status.equals("internal")) {
			NAT.internal.add(ip);
			NAT.ports.add(candidateKey.split(":")[1]);
		} else {
			NAT.external.add(ip);
		}
		NAT.rand.add(candidateClient.getIP());
		NAT.macs.add(candidateClient.getMAC());
		NAT.clients.remove(candidateKey);
		NAT.activeClients.remove(candidateKey);
		NAT.table.remove(candidateKey);
        NAT.printStats();
	}

    /**
     * Function that inspects incoming messages from client
     * @param msg - String array with the contents of the message
     */
	private void preparePacket(String[] msg) {
		ClientInfo src = null, dest = null;
		ClientInfo portFwd = new ClientInfo("internal", "", "", "", 0, 0);
		src = NAT.table.get(msg[0]);
		dest = NAT.table.get(msg[1]);
		if (msg[1].split(":")[0].equals(NAT.IP)) {
			dest = portFwd;
		}
		String srcStat = src.getStatus();
		ClientInfo record = NAT.table.get(msg[0]);
		record.setLast(System.nanoTime());
		if (src == null) {
			System.out.println("> SOURCE IS NULL");
		} else if (dest == null && srcStat.equals("internal")) {
			System.out.println("> INTERNAL TO EXTERNAL");
			msg[0] = NAT.IP;
			sendPacket("ERR1",msg,src);
		} else if (dest == null && srcStat.equals("external")) {
			System.out.println("> EXTERNAL TO OTHER");
			sendPacket("ERR0",msg,src);
		} else if (dest != null && src != null) {
			String destStat = dest.getStatus();
			if (srcStat.equals("internal")) {
				if (destStat.equals("internal")) {
			        System.out.println("> INTERNAL TO INTERNAL");
					sendPacket(msg[3],msg,src);
				} else {
			        System.out.println("> INTERNAL TO EXTERNAL");
					msg[0] = String.format("%s:%s", NAT.IP, msg[0].split(":")[1]);
					sendPacket(msg[3],msg,src);
				}
			} else {
				if (destStat.equals("internal")) {
			        System.out.println("> EXTERNAL TO INTERNAL");
					if (!msg[1].split(":")[0].equals(NAT.IP)) {
			            System.out.println("> INVALID IP");
						sendPacket("ERR1",msg,src);
						return;
					}
					String[] components = msg[1].split(":");
					String ip = portMatch(components[1]);
					msg[1] = ip;
					sendPacket(msg[3],msg,src);
				} else {
			        System.out.println("> EXTERNAL TO EXTERNAL");
					sendPacket("ERR1",msg,src);
				}
			}
		} else {
			System.out.println("> ERROR: PACKET SEND FAILED");
		}
	}

    /**
     * Function that contructs and sends packets to clients
     * @param type - String containing message type code
     * @param msg - String array containing incoming message info
     * @param record - ClientInfo object containing message senders details
     */
	public void sendPacket(String type, String[] msg, ClientInfo record) {
		DataOutputStream recvStream;
		DataOutputStream sendStream;
		try {
			if (type.equals("ERR0") || type.equals("ERR1")) {
				int code = Integer.parseInt(type.substring(3));
				sendErrPacket(msg, code);
			} else if (type.equals("REP") || type.equals("REQ")) {
				recvStream = NAT.activeClients.get(msg[1]);
				byte[] buf;
				if (type.equals("REP")) {
					buf = ("0 Echo Reply").getBytes();
				} else {
					buf = ("8 Echo Request").getBytes();
				}
				long Check = getCRC32(buf);
				byte[] echoCheck = new byte[2];
				echoCheck[0] = (byte) (Check >> 8);
				echoCheck[1] = (byte) Check;
				if (new String(echoCheck).equals(msg[6])) {
					String send = "";
					for (int i = 0; i < msg.length; i++) {
						if (i == msg.length - 1) {
							send = send + msg[i];
						} else {
							send = send + msg[i] + "|";
						}
					}
			        System.out.println("> SENDING ECHO: "+send);
					byte[] echoPack = send.getBytes();
					recvStream.write(echoPack.length);
					recvStream.write(echoPack);
				} else {
			        System.out.println("> ERROR: SENDING ECHO FAILED");
					sendErrPacket(msg, 1);
				}
			} else if (type.equals("MSG") || type.equals("ACK")) {
				byte[] reply = msg[4].getBytes();
				long Check = getCRC32(reply);
				byte[] echoCheck = new byte[2];
				echoCheck[0] = (byte) (Check >> 8);
				echoCheck[1] = (byte) Check;
				if (!(new String(echoCheck).trim()).equals(msg[2].trim())) {
			        System.out.println("> ERROR: CHECKSUM MISMATCH");
					sendErrPacket(msg, 1);
				} else {
					record.setLast(System.nanoTime());
					recvStream = NAT.activeClients.get(msg[1]);
					byte[] pack = String.format("%s|%s|%s|%s|%s", msg[0], msg[1], msg[2], msg[3], msg[4]).getBytes();
					recvStream.write(pack.length);
					recvStream.write(pack);
				}
			}
		} catch (Exception e) {
			System.out.println("> ERROR: MESSAGE SENDING FAILED");
			return;
		}
	}

    /**
     * Helper function used to send error information back to sender
     * @param msg - contents of incoming message
     * @param code - error code to be sent back to sender
     */
	private static void sendErrPacket(String[] msg, int code) throws IOException {
		DataOutputStream sendStream = NAT.activeClients.get(msg[0]);
		String dropped = String.format("%s|%s|%s|ERR|3|%d|", msg[0],msg[1],msg[2],code);
	    System.out.println("> DROPPED PACKET: "+new String(dropped));
		byte[] dropPack = ICMPUtils.destUnreach(dropped, code, msg[4]);
		sendStream.write(dropPack.length);
		sendStream.write(dropPack);
	}

    /**
     * Helper function used to find the IP address associated with a specific port
     * @param port - supplied port number
     * @return IP address linked to the given port number
     */
	private static String portMatch(String port) {
		for (Map.Entry<String, ClientInfo> record : NAT.table.entrySet()) {
			if (record.getValue().getPort().equals(port)) {
				return record.getKey();
			}
		}
		return null;
	}
}
