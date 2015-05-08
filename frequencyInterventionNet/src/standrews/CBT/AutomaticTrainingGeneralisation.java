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


public class AutomaticTrainingGeneralisation {
	
	private boolean isTest = false;
	
	public AutomaticTrainingGeneralisation(boolean isTest) {
		this.isTest = isTest;
	}
	
	public BasicNetwork runGeneralisation() {
		System.out.println("Generalisation process...");
		
		// create training data
		Util.setTraining();
		
		TrainedNet best_trained = findTrail();
		
		if (isTest) drawGraph(best_trained);
		System.out.println("-----------------------------------------");
		
		
		return best_trained.getNetwork();
	}
	
	private void drawGraph(TrainedNet best_trained) {
		double[] ep = new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1];
		
		//set epoch data
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			ep[e/Util.STEP_EPOCH] = e;
		}
		
		plotGraph pg = new plotGraph("Error Graph", "Epoch", "Error rate", 
				ep, trainNet(best_trained.getNeurons()), best_trained.validation, best_trained.getEpoch());
		pg.plot();
	}
	
	private double[] trainNet(int neurons) {
		double[] er = new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1];
		
		//System.out.println("-----------------------------------------");
		
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			BasicNetwork res_best_network = createNetwork(neurons);
			
			final ResilientPropagation train = new ResilientPropagation(res_best_network, Util.trainingSet);
			train.fixFlatSpot(false);
		
			int epoch = 0;
			do {
				train.iteration();
				epoch++;
			} while (epoch < e);
			
			er[e/Util.STEP_EPOCH] = train.getError();
			
			//System.out.println("Epoch: " + (e+1) + " | Training error: " + train.getError());
		}
		
		return er;
	}
	
	private TrainedNet findTrail() {
		TrainedNet trail_trained = new TrainedNet();
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
			
//			System.out.println("-----------------------------------------");
//			System.out.println("Trail: " + neuron_trained.getTrail() + 
//							   " | Best Neurons: " + neuron_trained.getNeurons() + 
//							   " | Best Epoch: " + neuron_trained.getEpoch() + 
//							   " | Best Validation error: " + neuron_trained.getError());
				
			if (neuron_trained.getError()<trail_trained.getError()) {
				trail_trained = neuron_trained.clone();
			}	

		}
		
		// test the neural network
		System.out.println("-----------------------------------------");
		System.out.println("Best Trail: " + trail_trained.getTrail() +
							" | Best Network Neurons: " + trail_trained.getNeurons() + 
							" | Best Epoch: " + trail_trained.getEpoch() + 
							" | Best Validation Error: " + trail_trained.getError());
		
		Encog.getInstance().shutdown();
		
		return trail_trained;
	}
	
	private TrainedNet stopEpoch(int k, int neurons) {
		TrainedNet epoch_trained = new TrainedNet();
		TrainedNet previous_trained = new TrainedNet();
		TrainedNet current_trained = new TrainedNet();
		current_trained.setValidation(new double[Util.MAX_EPOCH/Util.STEP_EPOCH+1]);
		double[] box_validation_error = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		double[] previous_box_validation_error;
		
		boolean isStop = false;
		for(int e=0; e<=Util.MAX_EPOCH; e+=Util.STEP_EPOCH) {
			previous_trained = current_trained.clone();
			
			current_trained = validateNet(previous_trained.getValidation(), k, neurons, e);
			
			previous_box_validation_error = box_validation_error.clone();
			box_validation_error[0] = box_validation_error[1];
			box_validation_error[1] = box_validation_error[2];
			box_validation_error[2] =  current_trained.getError();
			
			//System.out.println(getAvg(box_validation_error) + " " + getAvg(previous_box_validation_error));
			if (!isStop) {
				if (current_trained.getError() < Util.ERROR_THRESHOLD || (e+Util.STEP_EPOCH+1 > Util.MAX_EPOCH)) {
					epoch_trained = current_trained.clone();
					if (isTest) {
						isStop = true;
					} else { break; }
				} else if (getAvg(box_validation_error)<getAvg(previous_box_validation_error)) {
					epoch_trained = previous_trained.clone();
				} else {
					if ((e+1) > epoch_trained.getEpoch() + (3*Util.STEP_EPOCH)) {
						//System.out.println("Stop..." + epoch_trained.getEpoch());
						if (isTest) {
							isStop = true;
						} else { break; }
					}
				}
			} else {
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
	
	private TrainedNet validateNet(double[] prev_val, int k, int neurons, int e) {

		BasicNetwork res_best_network = createNetwork(neurons);
		
		final ResilientPropagation train = new ResilientPropagation(res_best_network, Util.trainingSets[k]);
		train.fixFlatSpot(false);
	
		int epoch = 0;
		do {
			train.iteration();
			epoch++;
		} while (epoch < e);
		
		double res_best_validation_error = getValidationError(Util.validationSets[k], res_best_network);
		
//		System.out.println("Trail: " + (k+1) + 
//						   " | Neurons: " + neurons + 
//						   " | Epoch: " + e + 
//						   " | Best Validation error: " + res_best_validation_error);
		
		prev_val[e/Util.STEP_EPOCH] = res_best_validation_error;			
		return new TrainedNet(res_best_network, prev_val, res_best_validation_error, e, neurons, (k+1));
	}
	
	private double getValidationError(MLDataSet validationSet, BasicNetwork network) {
		double avg_error = 0.0;
		double sum_square_error = 0.0;
		for (MLDataPair pair : validationSet) {
			final MLData output = network.compute(pair.getInput());
//			System.out.println(pair.getInput().getData(0) + ","
//				+ pair.getInput().getData(1) + ","
//				+ pair.getInput().getData(1) + ", actual="
//				+ output.getData(0) + ", ideal="
//				+ pair.getIdeal().getData(0));
			sum_square_error += Math.pow((output.getData(0))-(pair.getIdeal().getData(0)), 2);
		}
		avg_error = sum_square_error/validationSet.getInputSize();
		
		double[] weights = network.getFlat().getWeights();
		double weight_decay = 0.0;
		for (int j=0; j<weights.length; j++) {
			weight_decay += Math.pow(weights[j],2);
		}
		
		double learning_const = 1;
		while (weight_decay * learning_const > avg_error) {
			learning_const /= 10;
		}
		
//		System.out.println("Average Error: " + avg_error);
//		System.out.println("Weight Decay: " + weight_decay);
//		System.out.println("Learning Const: " + learning_const);
//		System.out.println("Sum Error: " + (learning_const * weight_decay + avg_error));
//		
		return (learning_const * weight_decay +  avg_error);
	}
	
	private double getAvg(double[] inp) {
		double sum = 0.0;
		for (int i=0; i<inp.length; i++) {
			if (sum != Double.MAX_VALUE) {
				sum += inp[i];
			} else {
				return Double.MAX_VALUE;
			}
		}
		return (sum/inp.length);
	}
	
	private BasicNetwork createNetwork(int n) {
		// create a neural network, without using a factory
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, Util.NUM_INPUT_BITS));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, n));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, Util.NUM_OUTPUT_BITS));
		network.getStructure().finalizeStructure();
		network.reset();
		
		//double[] weights = new double[network.getFlat().getWeights().length];
		//network.getFlat().setWeights(weights);
		
		new ConsistentRandomizer(-1, 1, 500).randomize(network);
		//System.out.println(network.dumpWeights());
		
		return network;
	}
}


