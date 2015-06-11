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

import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/** This class generates the best possible net for select sequence
 * 
 * @author Patomporn Loungvara
 *
 */
public class SelectSequenceNet {
	
	// default setting for testing observation (draw full graph if true)
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
	
	/** Create CSV file on server by invoking PHP script
	 * 
	 */
	public static void createTrainingSetOnServer(){
		try {
			URL url = new URL(Util.METHOD_URL);
			try {
				// invoke PHP script
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
	
	/** Load CSV file from server and save to CSV file in the project
	 * 
	 */
	public static void getTrainingSetFromServer(){
		try {
			URL url = new URL(Util.DATA_URL);
			ReadableByteChannel rbc = null;
			FileOutputStream fos = null;
			
			try {		
				// load file as byte through the channel
				rbc = Channels.newChannel(url.openStream());
				
				// open CSV file as the output
				fos = new FileOutputStream(Util.dataCSVFileNameLoad);
				//System.out.println("Created the csv file?");
				
				// write loaded byte as CSV file
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				
				rbc.close();
				fos.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/** Find the best neural network using automatic training harness with generalisation technique 
	 * 
	 */
	public static void getNet() {
		AutomaticTrainingGeneralisation atg = new AutomaticTrainingGeneralisation(isTest);
		
		// Retrieve net
		BasicNetwork network = atg.runGeneralisation();
		// write neural network file (.eg)
		writeFile(network);
	}
	
	/** Refine data in CSV file to the appropriate format
	 * 
	 */
	public static void refineCSV(){
		System.out.println("Refine process...");
		try {
			CSVReader reader = new CSVReader(new FileReader(Util.dataCSVFileNameLoad));
			CSVWriter writer = null;
			try {
				// output file
				writer = new CSVWriter(new FileWriter(Util.dataCSVFileName));
				
				String [] nextLine;
				int loopCouter = 0;
				
				// loop for each row
				while ((nextLine = reader.readNext()) != null) {
					String [] inLine = new String[Util.NUM_INPUT_BITS+Util.NUM_OUTPUT_BITS];
					if (loopCouter > 0) {
//						for (int i=0; i<nextLine.length; i++) {
//							System.out.print(nextLine[i]);
//						}
						
						// Change the control level to binary
						double[] ctrl = Util.refineBinary(Integer.parseInt(nextLine[0]), Util.CTRL_DIGIT);
						for (int i=0; i<Util.CTRL_DIGIT; i++) {
							inLine[i] = String.valueOf(ctrl[i]);
						}
						
						//	Change the age to group of age in binary
						double[] age = Util.refineAge(Integer.parseInt(nextLine[1]));
						for (int i=0; i<Util.AGE_DIGIT; i++) {
							inLine[Util.CTRL_DIGIT+i] = String.valueOf(age[i]);
						}
						
						// Add the gender
						inLine[Util.CTRL_DIGIT+Util.AGE_DIGIT] = nextLine[2];
							
						// Change the sequence rate if null
						for (int i=0; i<Util.NUM_SEQ; i++) {
							inLine[Util.CTRL_DIGIT+Util.AGE_DIGIT+Util.GENDER_DIGIT+i] = (nextLine[3+i].isEmpty()?"0.5":nextLine[3+i]);
						}
						
					} else {
						//header
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
	
	/**  Write out adult net to file
	 * 
	 * @param network	Fully trained net
	 */
	public static void writeFile(BasicNetwork network) {
		EncogDirectoryPersistence.saveObject(Util.neuralNetfile, network);
		Util.ready_to_parse = true;
		System.out.println("Written Net File!!");
	}
	
	public static void testoutput(){
		if (Util.ready_to_parse) {
			BasicNetwork net = (BasicNetwork) EncogDirectoryPersistence.loadObject(Util.neuralNetfile);
			
			double[] input1 =  { 1, 0, 1, 0, 0, 0, 1, 0, 1 };
			double[] input2 =  { 1, 0, 0, 0, 0, 0, 1, 0, 0 };
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

}
