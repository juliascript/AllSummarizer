/*
 * This file is part of AllSummarizer project
 * 
 * Copyright 2015 Abdelkrime Aries <kariminfo0@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kariminf.as.confs.multiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import kariminf.as.process.extraction.bayes.Feature;
import kariminf.as.process.extraction.bayes.PLeng;
import kariminf.as.process.extraction.bayes.Pos;
import kariminf.as.process.extraction.bayes.RLeng;
import kariminf.as.process.extraction.bayes.TFB;
import kariminf.as.process.extraction.bayes.TFU;
import kariminf.ktoolja.math.Calculus;
import kariminf.ktoolja.file.FileManager;


public class MssTraining {

	private static final String mssFolder = 
			"/home/kariminf/Data/ATS/Mss15Train/";

	private static final String outFolder =
			"/home/kariminf/Data/ATS/Mss15Train/tests/training2015_2/";

	private static final String [] langs = 
		{"af"};
	//"af", "eo", "hr", "ms", "sh", "sl", "sr", "vi"

	private static Feature [] features = {
		new TFU(),
		new TFB(),
		new Pos(),
		new RLeng(),
		new PLeng(),
	};

	//add static thresholds let say 31 from 0.00 to 0.30
	private static String[] th_name = 
		{"ES1", "ES2", "ES3", "dev"/*"mean", "median", "variance", "Hmode", "Lmode", 
		"s_Dn", "D_sn", "D_s",
		"00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
		"10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
		"20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"*/
		};
	private static double[] th_value = 
		{.0, .0, .0, .0/*, .0, 
		.0, .0, .0,
		.00, .01, .02, .03, .04, .05, .06, .07, .08, .09,
		.10, .11, .12, .13, .14, .15, .16, .17, .18, .19,
		.20, .21, .22, .23, .24, .25, .26, .27, .28, .29, .30*/
		};


	private static HashMap<String, Integer> readSizes(String lang){
		HashMap<String, Integer> sizes =
				new HashMap<String, Integer>();

		File sizefile = new File(mssFolder + "target-length/" + lang + ".txt");

		if (! sizefile.exists()) return null;

		try {
			BufferedReader in = new BufferedReader(new FileReader(sizefile));
			String line;
			while ( (line = in.readLine()) != null) {
				if (line.trim().length() < 1) continue;

				String[] values = line.split(",");
				if (values.length<2) continue;

				sizes.put(values[0], new Integer(values[1]));
			}
			in.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return sizes;
		}

		return sizes;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		List<List<Integer>> comb = new ArrayList<List<Integer>>();
		for (int combNbr=1; combNbr <= features.length; combNbr++)
			comb.addAll(Calculus.getCombinations(features.length, combNbr));

		for (String lang: langs){//langs

			MSS mss = new MSS(lang);
			HashMap<String, Integer> sizes = readSizes(lang);
			//System.out.println(sizes);

			File folder = new File(mssFolder + "body/text/" +  lang + "/");
			if (! folder.exists()) continue;
			if (! folder.isDirectory()) continue;

			File[] files = folder.listFiles();
			for (File file: files){
				
				double min = 0.0;
				double max = 0.0;

				String fileName = file.getName();
				if (! fileName.endsWith("_body.txt")) continue;
				
				int summarySize = sizes.get(fileName);
				
				//verify if the file have been already processed
				fileName = fileName.substring(0, fileName.length()-9) + "/";
				if (new File(outFolder + lang + "/" + fileName).exists()){
					System.out.println(fileName  + " already processsed");
					continue;
				}
					
				

				String newfolderName = outFolder + lang + "/";
				FileManager.createFolder(newfolderName);
				newfolderName += fileName;
				FileManager.createFolder(newfolderName);

				
				//start counting
				long prePstartTime = System.currentTimeMillis();

				mss.preprocess(file);
				
				long prePendTime = System.currentTimeMillis();
				
				System.out.println(file.getName()  + " preprocessing...");
				
				//This is used to store statistics
				{

					List<Double> sim = Calculus.delMultiple(mss.getSimilarity(), 0.0);

					double mean = Calculus.mean(sim);
					double dev = Math.sqrt(Calculus.variance(sim));
					double dist = mss.getTermDistribution();
					int sentNum = mss.sentNum();
					
					
					Collections.sort(sim);
					
					min = sim.get(0);
					max = sim.get(sim.size()-1);
					
					double es1 = (mean - min)/3;
					double es2 = (max - mean)/3;
					double es3 = (es1+es2)/2;
					
					th_value[0] = es1;
					th_value[1] = es2;
					th_value[2] = es3;
					th_value[3] = dev;
					/*
					th_value[0] = Calculus.mean(sim);
					th_value[1] = Calculus.median(sim);
					th_value[2] = Calculus.variance(sim);
					th_value[3] = Calculus.modeHigh(sim);
					th_value[4] = Calculus.modeLow(sim);

					double dist = mss.getTermDistribution();
					int sentNum = mss.sentNum();
					th_value[5] = dist/sentNum;
					th_value[6] = 1/(dist * sentNum);
					th_value[7] = 1/dist;*/
					
					

				}
				
				List<Long> CstartTime = new ArrayList<Long>();
				List<Long> CendTime = new ArrayList<Long>();
				 HashMap<Integer, List<Long>> thPstartTime = 
						new HashMap<Integer, List<Long>>();
				HashMap<Integer, List<Long>> thPendTime = 
						new HashMap<Integer, List<Long>>();
				
				System.out.println("summary size= " + summarySize);
				
				for(int th=0;th<th_value.length;th++){//threshold 40

					System.out.println("threshold = " + th_value[th]);
					
					String newfolderName2 = newfolderName + th_name[th] + "/";
					System.out.println(newfolderName2);
					FileManager.createFolder(newfolderName2);
					
					/*if (th==4 && th_value[4] == th_value[3]){
						try {
							FileManager.saveFile(newfolderName2 + "info", "same as hmode");
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println("lmode same as hmode");
						
						CstartTime.add(CstartTime.get(3));
						CendTime.add(CendTime.get(3));
						
						thPstartTime.put(th, thPstartTime.get(3));
						thPendTime.put(th, thPendTime.get(3));
						
						continue;
					}*/
					
					CstartTime.add(System.currentTimeMillis());
					mss.cluster(th_value[th]);
					CendTime.add(System.currentTimeMillis());
					
					List<Long> PstartTime = new ArrayList<Long>();
					List<Long> PendTime = new ArrayList<Long>();
					for (List<Integer> oneComb : comb){//features combinations
						mss.clearFeatures();

						String featused = "";
						for (int index: oneComb){
							mss.addFeature(features[index]);
							featused += features[index].getClass().getSimpleName() + "-";
						}
						featused = featused.substring(0, featused.length()-1);


						String summary="";
						try {
							PstartTime.add(System.currentTimeMillis());
							summary = mss.summarize(summarySize, th_value[th]);
							PendTime.add(System.currentTimeMillis());
						} catch (Exception e1) {
							e1.printStackTrace();
							summary="";
						}

						try {

							FileManager.saveFile(newfolderName2 + featused + ".asz", summary);
						} catch (IOException e) {
							e.printStackTrace();
						}

					} //features combinations
					
					thPstartTime.put(th, PstartTime);
					thPendTime.put(th, PendTime);

				} //thresholds


				String stats = "Thresholds statistics,\n";
				stats = "method,value\n";
				stats += "min," + min + "\n";
				stats += "max," + max + "\n";
				
				for (int i=0; i< 4; i++)
					stats += th_name[i] + "," + th_value[i] + "\n";
/*
				stats += "\n\nTime statistics,\n\n";
				
				stats += "Preprocessing\n";
				stats += "start, end\n";
				stats += prePstartTime + "," + prePendTime + "\n";
				
				stats += "\nClustering & Processing";
				
				for (int th=0; th<th_name.length; th++){
					stats += "\n\nth,start,end\n";
					stats += th_name[th] + "," + CstartTime.get(th);
					stats += "," + CendTime.get(th) + "\n";
					
					
					stats += "processing\n,";
					for(int j=0; j<comb.size();j++)
						stats += comb.get(j).toString().replace(",","+") + ",";
					stats +="\nStart,";
					List<Long> PstartTime = thPstartTime.get(th);
					for(int j=0; j<PstartTime.size();j++)
						stats += PstartTime.get(j).toString() + ",";
					stats +="\nEnd,";
					List<Long> PendTime = thPendTime.get(th);
					for(int j=0; j<PendTime.size();j++)
						stats += PendTime.get(j).toString() + ",";
				}*/
				
				try {

					FileManager.saveFile(newfolderName + "stats2.csv", stats);
				} catch (IOException e) {
					e.printStackTrace();
				}


			} //files

		}//languages


	}

}
