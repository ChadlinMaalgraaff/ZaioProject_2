package CS354.NAT;

public class ClientInfo {

    private String status, mac, ip, port;
    private long lease, last_active;

    public ClientInfo(String status, String mac, String ip, String port, long lease, long last_active) {
        this.status = status;
        this.mac = mac;
        this.ip = ip;
        this.port = port;
        this.lease = lease;
        this.last_active = last_active;
    }

    public String getStatus() {
        return this.status;
    }

    public String getMAC() {
        return this.mac;
    }

    public String getIP() {
        return this.ip;
    }

    public String getPort() {
        return this.port;
    }

    public long getLease() {
        return this.lease;
    }

    public long getLast() {
        return this.last_active;
    }

    public void setStatus(String s) {
        this.status = s;
    }

    public void setPort(String s) {
        this.port = s;
    }

    public void setLease(long l) {
        this.lease = l;
    }

    public void setLast(long l) {
        this.last_active = l;
    }

    public String toString() {
        String out = String.format("IP: %s | Status: %s | MAC: %s |", ip, status, mac);
        return out;
    }
}
