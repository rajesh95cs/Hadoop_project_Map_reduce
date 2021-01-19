import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MRJoin
{
	public static class MortalityMapper extends Mapper < Object, Text, Text, Text >
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{
			String record = value.toString(); //Read each record
			String[] parts = record.split(","); //Parse CSV file
			String str = parts[0] +","+ parts[1];
			context.write(new Text(str), new Text("MORTALITY_RATE "+ parts[2])); //Label Mortality rate in european countries due to //infectious and parasitic disease
		}
	}
	
	public static class EmigrationMapper extends Mapper < Object, Text, Text, Text >
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{
			String record = value.toString(); //Read each record
			String[] parts = record .split(","); //Parse CSV file
			String str = parts[0]+","+parts[1];
			context.write(new Text(str), new Text("AVERAGE_NO_PEOPLE "+ parts[2])); //Label Average no of people crossed Countries
		}
	}
	
	public static class ReduceJoinReducer extends Reducer < Text, Text, Text, Text >
	{
		public void reduce(Text key, Iterable < Text > values, Context context) throws IOException, InterruptedException
		{
			String value = "";
			double total = 0.0;
			//int count = 0;
			/* Here is where the logic of the JOIN/ reduction is laid out where the 
			datasets are join according to its key-value pair her the keys will the 
			country,year and the value will be the one which is pertaining to its 
			country and year*/
			for (Text t: values)
			{	//System.out.println("the Value of t "+t);
				String parts[] = t.toString().split(" ");
				if(parts[0].equals("AVERAGE_NO_PEOPLE"))
				{
					no_of_people += Float.parseFloat(parts[1]); 
				}
				else if(parts[0].equals("MORTALITY_RATE"))
				{
					value = parts[1]; 
				}
			}
			String str = String.format("%s %.2f",key,no_of_people);
			context.write(new Text(str), new Text(value));
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		Configuration conf = new Configuration();
		Job job = new Job(conf, "Reduce-side-Join");
		job.setJarByClass(MRJoin.class);
		job.setReducerClass(ReduceJoinReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, MortalityMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, EmigrationMapper.class);
		Path outputPath = new Path(args[2]);
		FileOutputFormat.setOutputPath(job, outputPath);
		outputPath.getFileSystem(conf).delete(outputPath);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
