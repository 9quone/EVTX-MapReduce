import org.apache.commons.codec.binary.Hex;
import java.nio.file.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FilenameFilter;

public class Hexdump {
	public static void main(String[] args) throws IOException {
		File inputDir = new File(args[0]);
		
		FilenameFilter evtxFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.endsWith(".evtx");
			}
		}; 
		
		File[] eventLogs = inputDir.listFiles(evtxFilter);
		for (File e : eventLogs) {
			String result = "";
			byte[] data = Files.readAllBytes(e.toPath());
			result = Hex.encodeHexString(data);
			
			File fileOut = new File(inputDir.getPath() + "\\" + e.getName().substring(0, getPeriodIndex(e.getName())) + "HEX.txt");
			PrintWriter out = new PrintWriter(fileOut);
			out.print(result);
			out.close();
		}
	}
	
	private static int getPeriodIndex(String fileName) {
		for (int i = 0; i < fileName.length(); i++) {
			if (fileName.charAt(i) == '.') {
				return i;
			}
		}
		return 0;
	}
}