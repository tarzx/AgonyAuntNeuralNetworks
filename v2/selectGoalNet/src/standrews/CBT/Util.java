package standrews.CBT;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.encog.ml.data.MLDataSet;
import org.encog.util.csv.CSVFormat;
import org.encog.util.simple.TrainingSetUtil;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/** This class provide useful function for training process
 * 
 * @author Patomporn Loungvara
 *
 */
public class Util {
	// URL to invoke sever and download CSV file
	public static final String METHOD_URL = "http://pl44.host.cs.st-andrews.ac.uk/AndroidApp/v2/select_goal_to_CSV.php";
	public static final String DATA_URL = "http://pl44.host.cs.st-andrews.ac.uk/AndroidApp/v2/selectGoal.csv";
	
	// CSV File in process
	static String dataCSVFileNameLoad = "selectGoal.csv";
	static String dataCSVFileName = "selectGoalRefine.csv";
	static String[] dataCSVSet = {"value_set1.csv", "value_set2.csv", "value_set3.csv", 
								 "value_set4.csv", "value_set5.csv"};
	static String[] dataCSVValidationSet = {"validation_set1.csv", "validation_set2.csv", "validation_set3.csv", 
		 						 		   "validation_set4.csv", "validation_set5.csv"};
	// number of trail
	static int nSet = dataCSVSet.length;
	
	// input | output bit unit and each value in binary bit unit
	static final int NUM_GOAL = 2;
	static final int CTRL_DIGIT = 5;
	static final int AGE_DIGIT = 3;
	static final int GENDER_DIGIT = 1;
	static final int PREVG_DIGIT = 4;
	// Number of bits representing input
	static final int NUM_INPUT_BITS = CTRL_DIGIT + AGE_DIGIT + GENDER_DIGIT + PREVG_DIGIT;
	// Number of bits representing output 
	static final int NUM_OUTPUT_BITS = NUM_GOAL;
	
	// Training data | Validation data
	static MLDataSet trainingSet;
	static MLDataSet[] trainingSets = new MLDataSet[nSet];
	static MLDataSet[] validationSets = new MLDataSet[nSet];


	// Whether the net can be parsed for sending to the app
	static boolean ready_to_parse = false;
	
	// neural network file
	static File neuralNetfile = new File("neuralNetSelectGoal.eg");
	
	// Maximum number of neurons to have
	final static int MAX_NEURONS = 10;
	// Maximum epoch 
	final static int MAX_EPOCH = 1000;
	// Amount to increase epoch by
	final static int STEP_EPOCH = 20;
	// Threshold for Error
	final static double ERROR_THRESHOLD = 1E-10;
	
	/** Created training and validation dataSet as CSV files
	 * 
	 */
	public static void setTraining() {
		
		// load all data from CVS to dataset
		trainingSet = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
				dataCSVFileName, true, Util.NUM_INPUT_BITS, Util.NUM_OUTPUT_BITS);	
		
		// divide data into nSet trial
		divideCSVset();
		
		// set training data and validation dataset for each trial
		for (int i=0; i<nSet; i++) {
			// load training data from CSV file
			trainingSets[i] = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
					dataCSVSet[i], true, NUM_INPUT_BITS, NUM_OUTPUT_BITS);
			// load validation data from CSV file
			validationSets[i] = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
					dataCSVValidationSet[i], true, NUM_INPUT_BITS, NUM_OUTPUT_BITS);
		}
	}
	
	/** Divide all data in loaded CSV file to nSet trial including training data and validation data as CSV files
	 * 
	 */
	private static void divideCSVset(){
		try {
			// loop for each trial
			for (int i=0; i<nSet; i++) {
				CSVReader reader = new CSVReader(new FileReader(dataCSVFileName));
				CSVWriter writer, writerVal = null;
				try {
					// write training data CSV file
					writer = new CSVWriter(new FileWriter(dataCSVSet[i], false));
					// write validation data CSV file
					writerVal = new CSVWriter(new FileWriter(dataCSVValidationSet[i], false));
				
					String [] nextLine;
					int loopCounter = 0;
					while ((nextLine = reader.readNext()) != null) {
						//System.out.println(nextLine[0]+nextLine[1]+nextLine[2]+nextLine[3]);
						if (loopCounter == 0) {
							//header
							writer.writeNext(nextLine);
							writerVal.writeNext(nextLine);
						} else if (loopCounter%nSet==i) {
							// select row for validation
							writerVal.writeNext(nextLine);
						} else {
							// other row
							writer.writeNext(nextLine);
						}
						loopCounter++;
					}
					
					writer.close();
					writerVal.close();
					reader.close();
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** Refine value to binary bit
	 * 
	 * @param input		integer input value
	 * @param digit	 	number of bit that it should produce
	 * @return			array of binary bit
	 */
	public static double[] refineBinary(final int input, int digit) {
		int decimal = input;
		double[] binary = new double[digit];
		
		// loop for each bit
		for (int i=digit-1; i>=0; i--) {
			// find each bit value
			binary[i] = Math.floor(decimal/Math.pow(2, i));
			decimal %= Math.pow(2, i);
		}
		return binary;
	}
	
	/** Refine value to binary bit
	 * 
	 * @param 	age		input age
	 * @return			array of binary bit for age group
	 */
	public static double[] refineAge(final int age) {
		// define value for different group of age
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
}


