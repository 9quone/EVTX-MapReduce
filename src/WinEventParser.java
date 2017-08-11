import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WinEventParser {
	public static class EventIDMapper extends Mapper<NullWritable, BytesWritable, Text, Text> {
		public void map(NullWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
			byte[] data = value.getBytes();
			final int fileH = 4096;
			final int chunkH = 512;
			
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
					String erDate = "Not Available";
					int pos = erStart;
					int eventID = 0;
					
					while (pos < erStart + erSize) {
						byte[] sequence = Arrays.copyOfRange(data, pos, pos + 5);
						int offset = 0;
						
						if (SubArrayDecoder.matchSubArray14(sequence)) {
							offset = 5 + 83;
						} else if (SubArrayDecoder.matchSubArray12(sequence)) {
							offset = 5 + 75;
						} else {
							pos++;
							continue;
						}
						
						eventID = ByteBuffer.wrap(Arrays.copyOfRange(data, pos + offset, pos + offset + 2)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
						int dateEnd = pos + offset;	
						
						while (true) {
							if (data[dateEnd] == 0x01 && (data[dateEnd - 8] == (byte)0x80 || data[dateEnd - 9] == (byte)0x80 
									|| data[dateEnd - 8] == (byte)0x40 || data[dateEnd - 9] == (byte)0x40
									|| data[dateEnd - 8] == (byte)0x20 || data[dateEnd - 9] == (byte)0x20)) {
								erDate = SubArrayDecoder.getTime(Arrays.copyOfRange(data, dateEnd - 7, dateEnd + 1));
								break;
							} if (dateEnd >= erStart + erSize) {
								break;
							}
							dateEnd++;
						}
						break;
					}
					context.write(new Text("" + eventID), new Text(erDate));
					erStart += erSize;
				}
				chunkCount--;
				chunkPos += 65536;
			}
		}
	}
	
	public static class EventIDReducer extends Reducer<Text, Text, Text, Text> {
		public void reduce(Text eventID, Iterable<Text> times, Context context) throws IOException, InterruptedException {
			int incidents = 0;
			for (@SuppressWarnings("unused") Text t : times) {
				incidents++;
			}
			context.write(eventID, new Text("" + incidents));
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Win Event Parser");
		job.setJarByClass(WinEventParser.class);
		job.setMapperClass(EventIDMapper.class);
		job.setReducerClass(EventIDReducer.class);
		job.setInputFormatClass(WholeFileInputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
