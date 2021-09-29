package CS354.NAT;

import java.io.*;
import java.net.*;
import java.util.*;

public class IPUtils {

    public static String NAT_IP() {
        String ip = "";
        Random r  = new Random();
        do {
            ip = String.format("100.%d.%d.%d", r.nextInt(64) + 64, r.nextInt(256), r.nextInt(256));
        } while (!checkIP(ip));
        return ip;
    }

    public static ArrayList<String> MAC_Pool(int size) {
        ArrayList<String> pool = new ArrayList<String>();
        String macIP = null;
        Random r = null;
        for (int i = 0; i < (size+1); i++) {
            do {
                r = new Random();
                byte[] addr = new byte[6];
                r.nextBytes(addr);
                addr[0] = (byte) (addr[0] & (byte) 254);
                StringBuilder mac = new StringBuilder(18);
                for (byte b : addr) {
                    if (mac.length() > 0) {
                        mac.append(":");
                    }
                    mac.append(String.format("%02x",b));
                }
                macIP = mac.toString();
            } while (pool.contains(macIP));
            pool.add(macIP);
        }
        return pool;
    }

    public static ArrayList<String> internal_Pool(int size) {
        ArrayList<String> pool = new ArrayList<String>();
        String prefix = "192.168.0.";
        String ip = "";
        for (int i = 0; i < size; i++) {
            ip = prefix + i;
            pool.add(ip);
        }
        return pool;
    }

    public static ArrayList<String> random_Pool(int size) {
        Random r = new Random();
        ArrayList<String> pool = new ArrayList<String>();
        String ip = "";
        for (int i = 0; i < size; i++) {
            do {
                ip = String.format("%d.%d.%d.%d", r.nextInt(101), r.nextInt(64), r.nextInt(256), r.nextInt(256));
            } while (!checkIP(ip) && pool.contains(ip));
            pool.add(ip);
        }
        return pool;
    }

    public static ArrayList<String> external_Pool(int size) {
        Random r = new Random();
        ArrayList<String> pool = new ArrayList<String>();
        String ip = "";
        for (int i = 0; i < size; i++) {
            do {
                ip = String.format("%d.%d.%d.%d", r.nextInt(28) + 100, r.nextInt(128) + 128, r.nextInt(256), r.nextInt(256));
            } while (!checkIP(ip) && pool.contains(ip));
            pool.add(ip);
        }
        return pool;
    }

    public static ArrayList<String> ports_Pool(int size) {
        Random r = new Random();
        ArrayList<String> pool = new ArrayList<String>();
        String port = "";
        for (int i = 0; i < size; i++) {
            do {
                port = String.format("%d%d%d%d", r.nextInt(10), r.nextInt(10), r.nextInt(10), r.nextInt(10));
            } while (pool.contains(port));
            pool.add(port);
        }
        return pool;
    }

    public static boolean checkIP(String ip) {
        String a[] = ip.split(".");
        for (String s : a) {
            int c = Integer.parseInt(s);
            if (s.length() > 3 || c < 0 || c > 255)
                return false;
            if (s.length() > 1 && c == 0)
                return false;
            if (s.length() > 1 && c != 0 && s.charAt(0) == '0')
                return false;
        }
        return true;
    }

}
