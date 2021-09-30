package CS354.NAT;

public class ICMPUtils {
	public static synchronized byte[] destUnreach(String dropped, int code, String reply) {
		String err = String.format("3 Destination %s unreachable", (code == 0)? "net":"host");
		byte[] temp = err.getBytes();
		long check = calCheckSum(temp, temp.length);
		byte[] out = creatErrPack(dropped, check, reply);
		return out;
	}

	private static byte[] creatErrPack(String dropped, long check, String reply) {
		byte[] first = dropped.getBytes();
		byte[] r = reply.getBytes();
		int len = first.length + 3 + ((r.length <= 8)? r.length:8);
		byte[] pack = new byte[len];
		int j = 0;
		for (int i = 0; i < first.length; i++) {
			pack[j++] = first[i];
		}
		pack[j++] = (byte) (check >> 8);
		pack[j++] = (byte) check;
		pack[j++] = '|';
		for (int i = 0; i < ((r.length <= 8)? r.length:8); i++) {

		}
		return null;
	}

	private static long calCheckSum(byte[] raw, int length) {
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
}