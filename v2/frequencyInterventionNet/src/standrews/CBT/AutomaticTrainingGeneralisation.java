package standrews.CBT;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.mathutil.randomize.ConsistentRandomizer;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

/** This class generates the best possible net for a given problem
 * 
 * @author Patomporn Loungvara
 *
 */
public class AutomaticTrainingGeneralisation {
	
	// define number of checked box to stop training
	private final int NUM_BOXES = 3;
	// default setting for testing observation (draw full graph if true)
	private boolean isTest = false;
	
	/** Constructor
	 * 
	 * @param isTest	status of testing observation
	 */
	public AutomaticTrainingGeneralisation(boolean isTest) {
		this.isTest = isTest;
	}
	
	/** Create a net with the best error with generalisation technique
	 * 
	 * @return			Best network
	 */	
	public BasicNetwork runGeneralisation() {
		System.out.println("Generalisation process...");
		
		// create training data
		Util.setTraining();
		
		// find the best trial from cross-validation training
		TrainedNet best_trained = findTrial();
		
		// draw graph if it is in the test environment
		if (isTest) drawGraph(best_trained);
		System.out.println("-----------------------------------------");
		
		
		return best_trained.getNetwork();
	}
	
	/** draw graph of input network for both training and validation errors
	 * 
	 * @param	best_trained	input network
	 */	
	private void drawGraph(TrainedNet best_trained) {
		double[] ep = new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1];
		
		//set epoch data
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			ep[e/Util.STEP_EPOCH] = e;
		}
		
		//draw graph using plotGraph class
		plotGraph pg = new plotGraph("Error Graph", "Epoch", "Error rate", 
				ep, best_trained.getTrain(), best_trained.validation, best_trained.getEpoch());
		pg.plot();
	}
	
	/** draw normal training process 
	 * 
	 * @param	neurones	number of hidden neuron
	 * @param	trainingSet	training data set
	 * @return				a series of training error in a set of epoch
	 */	
	private double[] trainNet(int neurones, MLDataSet trainingSet) {
		double[] er = new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1];
		
//		System.out.println("-----------------------------------------");
		
		// loop for each epoch with STEP_EPOCH incrementally
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			// create new network with input number of hidden neuron
			BasicNetwork res_best_network = createNetwork(neurones);
			
			// set training process with resilient propagation
			final ResilientPropagation train = new ResilientPropagation(res_best_network, trainingSet);
			train.fixFlatSpot(false);
		
			// train the network up to a certain epoch
			int epoch = 0;
			do {
				train.iteration();
				epoch++;
			} while (epoch < e);
			
			// calculate training error for each epoch
			er[e/Util.STEP_EPOCH] = calError(trainingSet, res_best_network);
			
//			System.out.println("Epoch: " + (e+1) + " | Training error: " + train.getError());
		}
		
		return er;
	}
	
	/** find the best trial from cross-validation training
	 * 
	 * @return			TrainedNet object that contains information of the best networks
	 * 					e.g. series of validation error and training error in a set of epoch, number of hidden neuron and etc.
	 */	
	private TrainedNet findTrial() {
		TrainedNet trial_trained = new TrainedNet();
		for (int k=0; k<Util.nSet; k++) {
			TrainedNet neuron_trained = new TrainedNet();
			int neurons = 0;
			while (neurons<Util.MAX_NEURONS) {
//				System.out.println("-----------------------------------------");
				TrainedNet epoch_trained = stopEpoch(k, ++neurons);
				
				if (epoch_trained.getError()<neuron_trained.getError()) {
					neuron_trained = epoch_trained.clone();
				}
			}
			neuron_trained.setTrain(trainNet(neuron_trained.getNeurones(), Util.validationSets[k]));
			
//			System.out.println("-----------------------------------------");
//			System.out.println("Trail: " + neuron_trained.getTrail() + 
//							   " | Best Neurons: " + neuron_trained.getNeurons() + 
//							   " | Best Epoch: " + neuron_trained.getEpoch() + 
//							   " | Best Validation error: " + neuron_trained.getError());
				
			if (neuron_trained.getError()<trial_trained.getError()) {
				trial_trained = neuron_trained.clone();
			}	

		}
		
		// test the neural network
		System.out.println("-----------------------------------------");
		System.out.println("Best Trail: " + trial_trained.getTrail() +
							" | Best Network Neurons: " + trial_trained.getNeurones() + 
							" | Best Epoch: " + trial_trained.getEpoch() + 
							" | Best Validation Error: " + trial_trained.getError());
		
		Encog.getInstance().shutdown();
		
		return trial_trained;
	}
	
	/** find the best network that stop training before the validation error starts increasing
	 * 
	 * @param	k			trial number
	 * @param	neurones	number of hidden neuron in the network
	 * @return				TrainedNet object that contains information of the network that stop training before the validation error starts increasing
	 * 						e.g. series of validation error and training error in a set of epoch, number of hidden neuron and etc.
	 */
	private TrainedNet stopEpoch(int k, int neurones) {
		TrainedNet epoch_trained = new TrainedNet();
		TrainedNet previous_trained = new TrainedNet();
		TrainedNet current_trained = new TrainedNet();
		current_trained.setValidation(new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1]);
		double[] box_validation_error = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		double[] previous_box_validation_error;
		boolean isStop = false;
		
		// loop of training neural network for each epoch incrementally
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			// set previous trained net as the last current net
			previous_trained = current_trained.clone();
			
			// get current net trained with e epoch and also containing the information of last net
			current_trained = validateNet(previous_trained.getValidation(), k, neurones, e);
			
			// set error value in considered box and previous box from last values in box and current error value
			previous_box_validation_error = box_validation_error.clone();
			box_validation_error[0] = box_validation_error[1];
			box_validation_error[1] = box_validation_error[2];
			box_validation_error[2] =  current_trained.getError();
			
			// check if it already stop, continue assign value
			if (!isStop) {
				// stop if the error is less than threshold or it reaches maximum epoch
				if (current_trained.getError() < Util.ERROR_THRESHOLD || (e+Util.STEP_EPOCH+1 > Util.MAX_EPOCH)) {
					// return current epoch
					epoch_trained = current_trained.clone();
					if (isTest) {
						// in case of testing, continue assign value after stop in order to plot a graph
						isStop = true;
					} else { break; }
				// check if average value from boxcar filter is less than the previous one, continue find the minimum
				} else if (getAvg(box_validation_error)<getAvg(previous_box_validation_error)) {
					epoch_trained = previous_trained.clone();
				} else {
					// if average value from boxcar filter is less than the previous one for next NUM_BOXES iterations, stop
					if ((e+1) > epoch_trained.getEpoch() + (NUM_BOXES*Util.STEP_EPOCH)) {
						//System.out.println("Stop..." + epoch_trained.getEpoch());
						if (isTest) {
							// in case of testing, continue assign value after stop in order to plot a graph
							isStop = true;
						} else { break; }
					}
				}
			} else {
				// continue assign value after stop
				epoch_trained.setValidationIdx((e/Util.STEP_EPOCH), current_trained.getError());	
			}
			
		}
		
//		System.out.println("-----------------------------------------");
//		System.out.println("Trail: " + epoch_trained.getTrail() + 
//						   " | Neurons: " + neurons + 
//						   " | Best Epoch: " + epoch_trained.getEpoch() + 
//						   " | Best Validation error: " + epoch_trained.getError());
		
		return epoch_trained;
	}
	
	/** find the best network that is trained for a certain epoch
	 * 
	 * @param	prev_val	the error value of previous networks that are trained with lower epoch
	 * @param	k			trial number
	 * @param	neurones	number of hidden neuron in the network
	 * @return				TrainedNet object that is trained for a certain epoch
	 * 						e.g. series of validation error and training error in a set of epoch, number of hidden neuron and etc.
	 */
	private TrainedNet validateNet(double[] prev_val, int k, int neurones, int e) {
		// create new neural network with input hidden neuron [neurones]
		BasicNetwork res_best_network = createNetwork(neurones);
		
		// set resilient propagation for training method with training data of input [k] trial
		final ResilientPropagation train = new ResilientPropagation(res_best_network, Util.trainingSets[k]);
		train.fixFlatSpot(false);
	
		// train the network up to a certain epoch
		int epoch = 0;
		do {
			train.iteration();
			epoch++;
		} while (epoch < e);
		
		// calculate validation error for each epoch from validation data of input [k] trial
		double res_best_validation_error = calError(Util.validationSets[k], res_best_network);
		
//		System.out.println("Trail: " + (k+1) + 
//						   " | Neurons: " + neurons + 
//						   " | Epoch: " + e + 
//						   " | Best Validation error: " + res_best_validation_error);
		
		// set current validation error with the previous one
		prev_val[e/Util.STEP_EPOCH] = res_best_validation_error;	
		
		//return new TrainedNet object contains current neural network, series of validation error values and current  validation value
		return new TrainedNet(res_best_network, prev_val, res_best_validation_error, e, neurones, (k+1));
	}
	
	/** calculate error from the output of network comparing with input data set
	 * 
	 * @param dataSet	input data set that is used to find find the error (training or validation)
	 * @param network	input network that is used to find output
	 * @return			mean square error rate with additional weight penalty
	 */
	private double calError(MLDataSet dataSet, BasicNetwork network) {
		double avg_error = 0.0;
		double sum_square_error = 0.0;
		
		// consider each input in dataset
		for (MLDataPair pair : dataSet) {
			// compute actual output from neural network
			final MLData output = network.compute(pair.getInput());
//			System.out.println(pair.getInput().getData(0) + ","
//				+ pair.getInput().getData(1) + ","
//				+ pair.getInput().getData(1) + ", actual="
//				+ output.getData(0) + ", ideal="
//				+ pair.getIdeal().getData(0));
			// sum each mean square error of actual output and ideal data (input in dataset)
			sum_square_error += Math.pow((output.getData(0))-(pair.getIdeal().getData(0)), 2);
		}
		// find average mean square error
		avg_error = sum_square_error/dataSet.getInputSize();
		
		// find each weight in neural network (use as a penalty of neural network)
		double[] weights = network.getFlat().getWeights();
		double weight_decay = 0.0;
		for (int j=0; j<weights.length; j++) {
			// sum each square weight in neural network
			weight_decay += Math.pow(weights[j],2);
		}
		
		// find gamma constant value that lead the sum square weight is less than average mean square error
		double learning_const = 1;
		while (weight_decay * learning_const > avg_error) {
			learning_const /= 10;
		}
		
//		System.out.println("Average Error: " + avg_error);
//		System.out.println("Weight Decay: " + weight_decay);
//		System.out.println("Learning Const: " + learning_const);
//		System.out.println("Sum Error: " + (learning_const * weight_decay + avg_error));
//		
		// return average mean square error including penalty from weight
		return (learning_const * weight_decay +  avg_error);
	}
	
	/** find average value from inpue set of double
	 * 
	 * @param inp		input array of double
	 * @return			average value
	 */
	private double getAvg(double[] inp) {
		double sum = 0.0;
		// loop for all input value
		for (int i=0; i<inp.length; i++) {
			if (sum != Double.MAX_VALUE) {
				sum += inp[i];
			} else {
				// if summary value exceed the boundary value of double type
				return Double.MAX_VALUE;
			}
		}
		// return average value
		return (sum/inp.length);
	}
	
	/** Create a net with a given number of hidden neurons
	 * 
	 * @param neurons	Number of hidden neurons
	 * @return			New network
	 */
	private BasicNetwork createNetwork(int n) {
		// create a neural network, without using a factory
		BasicNetwork network = new BasicNetwork();
		// First layer
		network.addLayer(new BasicLayer(null, true, Util.NUM_INPUT_BITS));
		// Hidden layer, with [neurons] neurons and sigmoid activation
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, n));
		// Output later and sigmoid activation
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, Util.NUM_OUTPUT_BITS));
		network.getStructure().finalizeStructure();
		network.reset();
		
		// set initial weight in neural network randomly between -1 to 1
		new ConsistentRandomizer(-1, 1, 500).randomize(network);
		//System.out.println(network.dumpWeights());
		
		return network;
	}
}


