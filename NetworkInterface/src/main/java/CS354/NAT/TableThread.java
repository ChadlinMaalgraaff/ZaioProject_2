package CS354.NAT;

import java.io.DataOutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class TableThread extends Thread{

	public TableThread() {
		// Empty constructor
	}

	@Override
	public void run() {
		while (true) {
			tableTrim();
			try {
				Thread.sleep(60000*10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void tableTrim() {
		for (Map.Entry<String, ClientInfo> record : NAT.table.entrySet()) {
			String removeKey = record.getKey();
			ClientInfo removeClient = record.getValue();
			long lastUsed = TimeUnit.MINUTES.convert(removeClient.getLast(), TimeUnit.NANOSECONDS);
			long currTime = TimeUnit.MINUTES.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
			double elapsed = currTime - lastUsed;
			if(elapsed < NAT.refresh_interval) {
				ClientInfo candidate = NAT.table.get(removeKey);
				if (removeKey == null) {
					continue;
				}
				try {
					byte[] raw = (NAT.IP + removeKey).getBytes();
					long rawCheck = getCRC32(raw);
					DataOutputStream candidateOut = NAT.activeClients.get(removeKey);
					byte[] packet = String.format("%s|%s|%l|LEV", NAT.IP, removeKey, (rawCheck >> 8) + rawCheck).getBytes();
					candidateOut.write(packet.length);
					candidateOut.write(packet);
				} catch (Exception e) {
					System.out.println("\nCLIENT LEAVE ERROR");
				}
				clientRemove(removeKey);
			}
		}
	}

    private long getCRC32(byte[] raw) {
		Checksum chkSum = new CRC32();
		chkSum.update(raw, 0, raw.length);
		return chkSum.getValue();
	}

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
	}
}
