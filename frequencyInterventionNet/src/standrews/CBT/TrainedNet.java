package standrews.CBT;
import org.encog.neural.networks.BasicNetwork;

public class TrainedNet {
	private BasicNetwork network = null;
	double[] validation = null;
	private double error = Double.MAX_VALUE;
	private int epoch = 0;
	private int neurons = 0;
	private int trail = 0;
	
	public TrainedNet() {
		this.network = new BasicNetwork();
	}
	
	public TrainedNet(BasicNetwork net, double[] val, double error, int epoch, int neurons, int trail) {
		this.network = net;
		this.validation = val;
		this.error = error;
		this.epoch = epoch;
		this.neurons = neurons;
		this.trail = trail;
	}
	
	public void setValidation(double[] val) {
		this.validation = val;
	}
	public void setValidationIdx(int idx, double val) {
		if (idx<this.validation.length) {
			this.validation[idx] = val;
		}
	}
	
	public BasicNetwork getNetwork() {
		return this.network;
	}
	public double[] getValidation() {
		return this.validation;
	}
	public double getError() {
		return this.error;
	}
	public int getEpoch() {
		return this.epoch;
	}
	public int getNeurons() {
		return this.neurons;
	}
	public int getTrail() {
		return this.trail;
	}
	
	/***
	 * clone this TrainedNet object - aims to avoid the problem from copy reference
	 * @return - new TrainedNet object that has the same information with this TrainedNet
	 */
	public TrainedNet clone() {
		return new TrainedNet(this.network, this.validation, this.error, this.epoch, this.neurons, this.trail);
	}
}
