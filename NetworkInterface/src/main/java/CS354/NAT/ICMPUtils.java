package CS354.NAT;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ICMPUtils {
    /**
     * Function that process the error packet request
     * @param dropped - the contents of the dropped packet
     * @param code - the relevant error code
     * @param reply - message of dropped packet
     * @return byte array containing the error packet
     */
	public static synchronized byte[] destUnreach(String dropped, int code, String reply) {
		String err = String.format("3 Destination %s unreachable", (code == 0)? "net":"host");
		byte[] temp = err.getBytes();
		long check = getCRC32(temp);
		byte[] out = creatErrPack(dropped, check, reply);
		return out;
	}

    /**
     * Helper function that constructs the error packet
     * @param dropped - the contents of the dropped packet
     * @param check - the error checksum
     * @param reply - message of dropped packet
     * @return byte array containing the error packet
     */
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
            pack[j++] = r[i];
		}
		return null;
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
}
