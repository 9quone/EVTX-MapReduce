import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class WinEventLogger {
	public static void main(String[] args) throws IOException {
		Path path = Paths.get("C:\\Windows\\System32\\winevt\\Logs\\Application.evtx");
		byte[] data = Files.readAllBytes(path);
		final int fileH = 4096;
		final int chunkH = 512;
		
		PrintWriter out = new PrintWriter("C:\\Users\\abhi\\Documents\\Research\\SEAP\\Programs\\application_event_data.txt");
		
		String start = null;
		try {
			start = new String(Arrays.copyOfRange(data, 0, 7));
			if (!start.equals("ElfFile")) {
				System.err.println("Error! Input File not a Windows Event Log (.evtx).");
				System.exit(1);
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Error! Input File not a Windows Event Log (.evtx).");
			System.exit(1);
		}
		
		short chunkCount = ByteBuffer.wrap(Arrays.copyOfRange(data, 42, 44)).order(ByteOrder.LITTLE_ENDIAN).getShort();
		int chunkPos = fileH;

		while (chunkCount > 0) {
			int lastEROffset = chunkPos + ByteBuffer.wrap(Arrays.copyOfRange(data, chunkPos + 44, chunkPos + 44 + 4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
			int erStart = chunkPos + chunkH;
			
			while (erStart <= lastEROffset) {
				int erSize = ByteBuffer.wrap(Arrays.copyOfRange(data, erStart + 4, erStart + 8)).order(ByteOrder.LITTLE_ENDIAN).getInt();
				String erDate = "N/A";
				int pos = erStart;
				int eventID = 0;
				
				while (pos < erStart + erSize) {
					byte[] sequence = Arrays.copyOfRange(data, pos, pos + 5);
					int offset = 0;
					
					if (matchSubArray14(sequence)) {
						offset = 5 + 83;
					} else if (matchSubArray12(sequence)) {
						offset = 5 + 75;
					} else {
						pos++;
						continue;
					}
					
					eventID = ByteBuffer.wrap(Arrays.copyOfRange(data, pos + offset, pos + offset + 2)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
					int dateEnd = pos + offset;	
					
					while (true) {
						if (data[dateEnd] == 0x01 && (data[dateEnd - 8] == (byte)0x80 || data[dateEnd - 9] == (byte)0x80 || data[dateEnd - 8] == (byte)0x40 || data[dateEnd - 9] == (byte)0x40)) {
							erDate = getTime(Arrays.copyOfRange(data, dateEnd - 7, dateEnd + 1));
							break;
						} if (dateEnd >= erStart + erSize) {
							break;
						}
						dateEnd++;
					}
					break;
				}
				out.println(eventID + " " + erDate);
				erStart += erSize;
			}
			chunkCount--;
			chunkPos += 65536;
		}
		out.close();
	}
	
	private static String getTime(byte[] time) {
		long windowsTime = ByteBuffer.wrap(time).order(ByteOrder.LITTLE_ENDIAN).getLong();
		long unixTime = (windowsTime - 116444736000000000L) / 10000;
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a z");
		String date = sdf.format(new Date(unixTime));
		return date;
	}
	
	private static boolean matchSubArray14(byte[] bytes) {
		byte[] magicStart14 = {0x14, 0x00, 0x00, 0x00, 0x01};
		return Arrays.equals(magicStart14, bytes);
	}
	
	private static boolean matchSubArray12(byte[] bytes) {
		byte[] magicStart12 = {0x12, 0x00, 0x00, 0x00, 0x01};
		return Arrays.equals(magicStart12, bytes);
	}
}