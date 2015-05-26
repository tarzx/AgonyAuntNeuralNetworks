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


public class Util {
	public static final String METHOD_URL = "http://pl44.host.cs.st-andrews.ac.uk/AndroidApp/v2/slot_intervention_to_CSV.php";
	public static final String DATA_URL = "http://pl44.host.cs.st-andrews.ac.uk/AndroidApp/v2/slotIntervention.csv";
	
	// CSV File
	static String dataCSVFileNameLoad = "slotIntervention.csv";
	static String dataCSVFileName = "slotInterventionRefine.csv";
	static String[] dataCSVSet = {"value_set1.csv", "value_set2.csv", "value_set3.csv", 
								 "value_set4.csv", "value_set5.csv"};
	static String[] dataCSVValidationSet = {"validation_set1.csv", "validation_set2.csv", "validation_set3.csv", 
		 						 		   "validation_set4.csv", "validation_set5.csv"};
	static int nSet = dataCSVSet.length;
	
	static final int NUM_SLOT = 6;
	static final int CTRL_DIGIT = 5;
	static final int AGE_DIGIT = 3;
	static final int GENDER_DIGIT = 1;
	// Number of bits representing input
	static final int NUM_INPUT_BITS = CTRL_DIGIT + AGE_DIGIT + GENDER_DIGIT;
	// Number of bits representing output 
	static final int NUM_OUTPUT_BITS = NUM_SLOT;
	
	
	// Training data
	static MLDataSet trainingSet;
	static MLDataSet[] trainingSets = new MLDataSet[nSet];
	static MLDataSet[] validationSets = new MLDataSet[nSet];


	// Whether the net can be parsed for sending to the app
	static boolean ready_to_parse = false;
	static File neuralNetfile = new File("neuralNetslotIntervention.eg");
	
	// Maximum number of neurons to have
	final static int MAX_NEURONS = 10;
	// Maximum epoch 
	final static int MAX_EPOCH = 1000;
	// Amount to increase epoch by
	final static int STEP_EPOCH = 20;
	// Threshold for Error
	final static double ERROR_THRESHOLD = 1E-10;
	final static double THRESHOLD = 3000;
	
	public static void setTraining() {
		//Training
		trainingSet = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
				dataCSVFileName, true, Util.NUM_INPUT_BITS, Util.NUM_OUTPUT_BITS);	
		
		//Validation
		refineCSVset();
		for (int i=0; i<nSet; i++) {
			trainingSets[i] = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
					dataCSVSet[i], true, NUM_INPUT_BITS, NUM_OUTPUT_BITS);
			validationSets[i] = TrainingSetUtil.loadCSVTOMemory(CSVFormat.ENGLISH, 
					dataCSVValidationSet[i], true, NUM_INPUT_BITS, NUM_OUTPUT_BITS);
		}
	}
	
	private static void refineCSVset(){
		try {
			for (int i=0; i<nSet; i++) {
				CSVReader reader = new CSVReader(new FileReader(dataCSVFileName));
				CSVWriter writer, writerVal = null;
				try {
					writer = new CSVWriter(new FileWriter(dataCSVSet[i], false));
					writerVal = new CSVWriter(new FileWriter(dataCSVValidationSet[i], false));
				
					String [] nextLine;
					int loopCounter = 0;
					while ((nextLine = reader.readNext()) != null) {
						//System.out.println(nextLine[0]+nextLine[1]+nextLine[2]+nextLine[3]);
						if (loopCounter == 0) {
							writer.writeNext(nextLine);
							writerVal.writeNext(nextLine);
						} else if (loopCounter%nSet!=i) {
							writer.writeNext(nextLine);
						} else {
							writerVal.writeNext(nextLine);
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
	
	public static double[] refineBinary(final int input, int digit) {
		int decimal = input;
		double[] binary = new double[digit];
		for (int i=digit-1; i>=0; i--) {
			binary[i] = Math.floor(decimal/Math.pow(2, i));
			decimal %= Math.pow(2, i);
		}
		return binary;
	}
	
	public static double[] refineAge(final int age) {
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


