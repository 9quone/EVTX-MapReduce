import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class SubArrayDecoder {
	public static String getTime(byte[] time) {
		long windowsTime = ByteBuffer.wrap(time).order(ByteOrder.LITTLE_ENDIAN).getLong();
		long unixTime = (windowsTime - 116444736000000000L) / 10000;
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a z");
		String date = sdf.format(new Date(unixTime));
		return date;
	}
	
	public static boolean matchSubArray14(byte[] bytes) {
		byte[] magicStart14 = {0x14, 0x00, 0x00, 0x00, 0x01};
		return Arrays.equals(magicStart14, bytes);
	}
	
	public static boolean matchSubArray12(byte[] bytes) {
		byte[] magicStart12 = {0x12, 0x00, 0x00, 0x00, 0x01};
		return Arrays.equals(magicStart12, bytes);
	}
}