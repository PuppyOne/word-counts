package com.zxz;

import org.apache.hadoop.conf.Configuration;
import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCounts {

    // Mapper类
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);

        public void map(Object key, Text value, Context context) 
            throws IOException, InterruptedException {
            String line = value.toString();
            
            // 跳过序号行和时间戳行
            if (line.matches("^\\d+$") || line.contains("-->")) 
                return;
            
            // 清洗HTML标签
            String cleanedLine = line.replaceAll("<.*?>", "");
            
            // 分词并过滤空值
            String[] words = cleanedLine.split("\\s+");
            for (String w : words) {
                // 清洗标点符号和数字
                String cleanedWord = w.replaceAll("[^a-zA-Z']", "")
                        .replaceAll("^'+|'+$", "")
                        .toLowerCase();
                if (!cleanedWord.isEmpty()) {
                    context.write(new Text(cleanedWord), one);
                }
            }
        }
    }

    // Reducer类
    public static class IntSumReducer
            extends Reducer<Text, IntWritable, Text, IntWritable> {

        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    // Driver类
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "word counts");
        job.setJarByClass(WordCounts.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class); // 启用Combiner优化
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}