package CS354.NAT;

import java.io.DataOutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
				// TODO Auto-generated catch block
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
					long rawCheck = calCheckSum(raw, raw.length);
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

	private long calCheckSum(byte[] raw, int length) {
		int i = 0;
		long sum = 0;
		while (length > 0) {
			sum += (raw[i++]&0xff) << 8;
			if ((--length)==0) break;
			sum += (raw[i++]&0xff);
			--length;
		}
		return (~((sum & 0xFFFF)+(sum >> 16))) & 0xFFFF;
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
