package standrews.CBT;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.encog.ml.data.MLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class SelectSequenceNet {
	
	private final static boolean isTest = false;
	
	public static void main(String[] args) {
		System.out.println("Select Sequence");
		
		//Server
		createTrainingSetOnServer();	
		getTrainingSetFromServer();
		refineCSV();
		
		//Neural Network
		getNet();
		
		//Test
		if(isTest) testoutput();
	}
	
	public static void createTrainingSetOnServer(){
		try {
			URL url = new URL(Util.METHOD_URL);
			try {
				
				 BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
					        in.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	public static void getTrainingSetFromServer(){
		try {
			URL url = new URL(Util.DATA_URL);
			ReadableByteChannel rbc = null;
			FileOutputStream fos = null;
			
			try {
				 rbc = Channels.newChannel(url.openStream());
				 
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				fos = new FileOutputStream(Util.dataCSVFileNameLoad);
				//System.out.println("Created the csv file?");
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void getNet() {
		AutomaticTrainingGeneralisation atg = new AutomaticTrainingGeneralisation(isTest);
		
		// Retrieve net
		BasicNetwork network = atg.runGeneralisation();
		writeFile(network);
	}
	
	public static void refineCSV(){
		System.out.println("Refine process...");
		try {
			CSVReader reader = new CSVReader(new FileReader(Util.dataCSVFileNameLoad));
			CSVWriter writer = null;
			try {
				writer = new CSVWriter(new FileWriter(Util.dataCSVFileName));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String [] nextLine;
			int loopCouter = 0;
			try {
				while ((nextLine = reader.readNext()) != null) {
					String [] inLine = new String[Util.NUM_INPUT_BITS+Util.NUM_OUTPUT_BITS];
					if (loopCouter > 0) {
//						for (int i=0; i<nextLine.length; i++) {
//							System.out.print(nextLine[i]);
//						}
						
//						Change the control level to binary
						double[] ctrl = refineBinary(Integer.parseInt(nextLine[0]), Util.CTRL_DIGIT);
						for (int i=0; i<Util.CTRL_DIGIT; i++) {
							inLine[i] = String.valueOf(ctrl[i]);
						}
						
//						Change the age to group of age in binary
						double[] age = refineAge(Integer.parseInt(nextLine[1]));
						for (int i=0; i<Util.AGE_DIGIT; i++) {
							inLine[Util.CTRL_DIGIT+i] = String.valueOf(age[i]);
						}
						
//						Add the gender
						inLine[Util.CTRL_DIGIT+Util.AGE_DIGIT] = nextLine[2];
							
//						Change the sequence rate if null
						for (int i=0; i<Util.NUM_SEQ; i++) {
							inLine[Util.CTRL_DIGIT+Util.AGE_DIGIT+Util.GENDER_DIGIT+i] = (nextLine[3+i].isEmpty()?"0.5":nextLine[3+i]);
						}
						
					} else {
						inLine = new String[] {"control_level_0", "control_level_1", "control_level_2", "control_level_3", "control_level_4",
									  		   "age_0", "age_1", "age_2", "gender", 
									  		   "seq_1", "seq_2", "seq_3", "seq_4", "seq_5", "seq_6", "seq_7", "seq_8" };
					}
					
					writer.writeNext(inLine);			
					loopCouter++;
					
				}
				
				writer.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static double[] refineBinary(final int input, int digit) {
		int decimal = input;
		double[] binary = new double[digit];
		for (int i=digit-1; i>=0; i--) {
			binary[i] = Math.floor(decimal/Math.pow(2, i));
			decimal %= Math.pow(2, i);
		}
		return binary;
	}
	
	private static double[] refineAge(final int age) {
		if (age<=17) {
			return new double[] { 0.0, 0.0, 0.0 };
		} else if (age<=24) {
			return new double[] { 0.0, 0.0, 1.0 };
		} else if (age<=49) {
			return new double[] { 0.0, 1.0, 0.0 };
		} else if (age<=64) {
			return new double[] { 0.0, 1.0, 1.0 };
		} else {
			return new double[] { 1.0, 0.0, 0.0 };
		}
	}
	
	public static void testoutput(){
		if (Util.ready_to_parse) {
			BasicNetwork net = (BasicNetwork) EncogDirectoryPersistence.loadObject(Util.neuralNetfile);
			
			double[] input1 =  { 1, 1, 0, 0, 0, 1, 0, 0, 1 };
			double[] input2 =  { 0, 1, 0, 1, 0, 0, 1, 0, 0 };
			double[] input3 =  { 1, 0, 1, 1, 0, 1, 1, 0, 0 };
			double[] input4 =  { 0, 1, 0, 0, 1, 0, 0, 1, 1 };
			
			double[] output1 = new double[8];
		    double[] output2 = new double[8];
		    double[] output3 = new double[8];
		    double[] output4 = new double[8];
		    
		    net.compute(input1, output1);
		    net.compute(input2, output2);
		    net.compute(input3, output3);
		    net.compute(input4, output4);
		    
		    System.out.println();
		    System.out.println("The output question is: " + output1[0] + " " + output1[1] + " " + output1[2] + " " + output1[3] + 
		    										  " " + output1[4] + " " + output1[5] + " " + output1[6] + " " + output1[7]);
		    System.out.println("The output question is: " + output2[0] + " " + output2[1] + " " + output2[2] + " " + output2[3] + 
					  								  " " + output2[4] + " " + output2[5] + " " + output2[6] + " " + output2[7]);
		    System.out.println("The output question is: " + output3[0] + " " + output3[1] + " " + output3[2] + " " + output3[3] + 
					  								  " " + output3[4] + " " + output3[5] + " " + output3[6] + " " + output3[7]);
		    System.out.println("The output question is: " + output4[0] + " " + output4[1] + " " + output4[2] + " " + output4[3] + 
		    										  " " + output4[4] + " " + output4[5] + " " + output4[6] + " " + output4[7]);
		    System.out.println();
		}
	}
	
	/**  Write out adult net to file
	 * 
	 * @param network	Fully trained net
	 */
	public static void writeFile(BasicNetwork network) {
		EncogDirectoryPersistence.saveObject(Util.neuralNetfile, network);
		Util.ready_to_parse = true;
		System.out.println("Written Net File!!");
	}
	
	public static MLDataSet getTrainingSet() {
		return Util.trainingSet;
	}
	
	public static int getInputBits() {
		return Util.NUM_INPUT_BITS;
	}

	public static int getOuputBits() {
		return Util.NUM_OUTPUT_BITS;
	}
	
}
