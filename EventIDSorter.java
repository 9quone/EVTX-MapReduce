import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class EventIDSorter {
	public static class EventIDMapper extends Mapper<Object, Text, Text, Text> {
		private Text eventID = new Text();
		private Text time = new Text();
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] event = value.toString().split(" ");
			eventID.set(event[0]);
			time.set(event[1] + " " + event[2]);
			context.write(eventID, time);
		}
	}
	
	public static class EventIDReducer extends Reducer<Text, Text, Text, Text> {
		public void reduce(Text eventID, Iterable<Text> times, Context context) throws IOException, InterruptedException {
			int incidents = 0;
			String allTimes = "";
			for (Text t : times) {
				incidents++;
				if (allTimes.equals("")) {
					allTimes += t.toString();
				} else {
					allTimes += ", " + t.toString();
				}
			}
			context.write(eventID, new Text("" + new IntWritable(incidents)));
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Event ID Sorter");
		job.setJarByClass(EventIDSorter.class);
		job.setMapperClass(EventIDMapper.class);
		job.setReducerClass(EventIDReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
