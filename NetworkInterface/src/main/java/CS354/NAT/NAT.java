package CS354.NAT;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NAT {

    public static long refresh_interval = 3600;
    public long last_active = 0;
    public static int pool_size = 4;
    public static String IP = "";
    public static String MAC = "";
    public static String FakeIP = "";

    public static ArrayList<String> internal;
    public static ArrayList<String> external;
    public static ArrayList<String> macs;
    public static ArrayList<String> ports;
    public static ArrayList<String> rand;

    public static Map<String, ClientInfo> table;
    public static ConcurrentHashMap<String, DataOutputStream> activeClients;
    public static ArrayList<String> clients = new ArrayList<String>();
    public static ServerSocket server;
    public static Socket client;

    public static void main(String[] args) throws IOException {
        refresh_interval = Long.parseLong(args[0]);
        pool_size = Integer.parseInt(args[1]);
        initialise();

        table = new HashMap<String, ClientInfo>();
        activeClients = new ConcurrentHashMap<String, DataOutputStream>();
        IP = IPUtils.NAT_IP();
        MAC = macs.remove(0);
        System.out.printf("NAT-box info:\nIP Address: %s\nMAC Address: %s\n\n", IP, MAC);

        try {
        	server = new ServerSocket(8002);
        } catch (Exception e) {
			System.err.println(e);
		}
        System.out.println("### SERVER RUNNING ###");

        DatagramSocket socket = new DatagramSocket(8888);
        TableThread manager = new TableThread();
        manager.start();

        while (true) {
        	if (DHCP_Check(socket)) {
        		try {
        			System.out.println("Client confirmed");
        			client = server.accept();
        			ClientThread user = new ClientThread(client, FakeIP);
        			user.start();
        		} catch (Exception e) {}
        	} else {
        		System.out.printf("NAT-box cannot accept more connections\n\n");
        	}
        }
    }

    public static void initialise() {
        if (pool_size > 256 || pool_size < 0) {
            pool_size = 256;
        }
        internal = IPUtils.internal_Pool(pool_size);
        external = IPUtils.external_Pool(pool_size);
        ports = IPUtils.ports_Pool(pool_size);
        macs = IPUtils.MAC_Pool(pool_size);
        rand = IPUtils.random_Pool(pool_size);
    }

    private static boolean DHCP_Check(DatagramSocket sock) throws IOException {
    	System.out.println("DHCP Called");
    	/*byte[] raw = new byte[256];
    	byte[] reply = new byte[256];
    	byte[] greet = ("HELLO").getBytes();
    	DatagramPacket packet = new DatagramPacket(raw, raw.length);
    	sock.receive(packet);
        System.out.println("RECEIVED");
    	int port = packet.getPort();
    	InetAddress addr = packet.getAddress();
    	System.out.println("addr: "+addr.getHostAddress()+" - name: "+addr.getHostName());
    	DatagramPacket handshake = new DatagramPacket(greet, greet.length, addr, port);
    	sock.send(handshake);*/
    	while (true) {
            byte[] raw = new byte[256];
            byte[] reply = new byte[256];
    		DatagramPacket packet = new DatagramPacket(raw, raw.length);
    		sock.receive(packet);
    		int port = packet.getPort();
    		InetAddress addr = packet.getAddress();

    		switch (raw[0]) {
			case 'd':
                System.out.println("d");
				if(activeClients.size() == pool_size) {
					reply[0] = 'd';
				} else {
					reply = String.format("d|%s", rand.remove(0)).getBytes();
				}
				packet = new DatagramPacket(reply, reply.length, addr, port);
				sock.send(packet);
				break;
			case 'm':
                System.out.println("m");
				reply = String.format("m|%s|%s", MAC, IP).getBytes();
				packet = new DatagramPacket(reply, reply.length, addr, port);
				sock.send(packet);
				break;
			case 'n':
                System.out.println("n");
				String ip = "", mac = "", p = "";
				if(activeClients.size() == pool_size) {
					reply[0] = 'f';
				} else {
					ip = internal.get(0);
					mac = macs.get(0);
					p = ports.get(0);
					reply = String.format("o|%s:%s|%s", ip, p, mac).getBytes();
				}
				packet = new DatagramPacket(reply, reply.length, addr, port);
				sock.send(packet);
				break;
			case 'a':
                System.out.println("a");
				String content = new String(packet.getData());
				String[] setup = content.split("\\|");
				String status = setup[1].trim();
				FakeIP = setup[2].trim();
				String real_ip = setup[3].trim();
				String macAddr = setup[4].trim();
				String[] ip_port = FakeIP.split(":");
				ClientInfo record = new ClientInfo(status, macAddr, real_ip, ip_port[1], refresh_interval, System.nanoTime());
				table.put(FakeIP, record);
				internal.remove(ip_port[0]);
				macs.remove(macAddr);
				ports.remove(ip_port[1]);
				return true;
			case 'r':
                System.out.println("r");
				return false;
			case 'e':
                System.out.println("e");
				boolean result;
				if (activeClients.size() == pool_size) {
					reply[0] = 'f';
					result = false;
				} else {
					FakeIP = external.remove(0);
					real_ip = rand.remove(0);
					macAddr = macs.remove(0);
					reply = String.format("c|%s|%s|%s", real_ip, FakeIP, macAddr).getBytes();
					record = new ClientInfo("external", macAddr, real_ip, "", refresh_interval, System.nanoTime());
					table.put(FakeIP, record);
					result = true;
				}
				packet = new DatagramPacket(reply, reply.length, addr, port);
				sock.send(packet);
				return result;
			default:
				System.out.println("\nUNKNOWN COMMAND\n");
				break;
			}
    	}
    }
}
