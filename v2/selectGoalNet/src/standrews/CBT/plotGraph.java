package standrews.CBT;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/** This class generates is used to plot the graph using ApplicationFrame in Java.
 * 
 * @author Patomporn Loungvara
 *
 */
public class plotGraph  extends ApplicationFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Creates a new demo. (Constructor)
     *
     * @param	title 	the frame title.
     * @param	ax		x-axis caption
     * @param	ay		y-axis caption
     * @param	x		series values of x
     * @param	y		series values of y
     */
    public plotGraph(final String title, String ax, String ay, double[] x, double[] y) {

        super(title);

        final XYDataset dataset = createDataset(x, y);
        final JFreeChart chart = createChart(dataset, title, ax, ay);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1200, 500));
        setContentPane(chartPanel);

    }
    
    /**
     * Creates a new demo. (Constructor)
     *
     * @param	title 	the frame title.
     * @param	ax		x-axis caption
     * @param	ay		y-axis caption
     * @param	x		series values of x
     * @param	y		series values of y for line 1
     * @param	z		series values of y for line 2
     * @param	s		stopped point
     */
    public plotGraph(final String title, String ax, String ay, double[] x, double[] y, double[] z, double s) {

        super(title);

        final XYDataset dataset = createDataset(x, y, z, s);
        final JFreeChart chart = createChart(dataset, title, ax, ay);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1200, 500));
        setContentPane(chartPanel);

    }
    
    /**
     * Creates a sample dataset.
     * 
     * @return a sample dataset.
     */
    private XYDataset createDataset(double[] x, double[] y) {
        
        final XYSeries series1 = new XYSeries("line");
	
        if (x == null && y != null) {
        	for (int i=0; i<y.length; i++) series1.add(i, y[i]);
        } else if (x != null && y == null) {
        	for (int i=0; i<x.length; i++) series1.add(x[i], i);
        } else if (x != null && y != null) {
        	for (int i=0; i<x.length; i++) series1.add(x[i], y[i]);
        }
    
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
                
        return dataset;
        
    }
    
    /**
     * Creates a sample dataset.
     * 
     * @return a sample dataset.
     */
    private XYDataset createDataset(double[] x, double[] y, double[] z, double s) {
        
        final XYSeries series1 = new XYSeries("Trainging");
        final XYSeries series2 = new XYSeries("Validation");
        final XYSeries series3 = new XYSeries("Stop");
	
        if (x == null && y != null) {
        	for (int i=0; i<y.length; i++) series1.add(i, y[i]);
        } else if (x != null && y == null) {
        	for (int i=0; i<x.length; i++) series1.add(x[i], i);
        } else if (x != null && y != null) {
        	for (int i=0; i<x.length; i++) series1.add(x[i], y[i]);
        }
        
        if (x == null && z != null) {
        	for (int i=0; i<z.length; i++) series2.add(i, z[i]);
        } else if (x != null && z == null) {
        	for (int i=0; i<x.length; i++) series2.add(x[i], i);
        } else if (x != null && z != null) {
        	for (int i=0; i<x.length; i++) series2.add(x[i], z[i]);
        }
        
        if (y == null && z != null) {
        	series3.add(s, 0);
        	for (int i=0; i<z.length; i++) series3.add(s, z[i]);
        } else if (y != null && z == null) {
        	series3.add(s, 0);
        	for (int i=0; i<y.length; i++) series3.add(s, y[i]);
        } else if (y != null && z != null) {
        	series3.add(s, 0);
        	for (int i=0; i<y.length; i++) series3.add(s, y[i]);
        	for (int i=0; i<z.length; i++) series3.add(s, z[i]);
        }
        
    
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);
                
        return dataset;
        
    }
    
    /**
     * Creates a chart.
     * 
     * @param dataset  the data for the chart.
     * 
     * @return a chart.
     */
    private JFreeChart createChart(final XYDataset dataset, String title, String ax, String ay) {
        
        // create the chart...
        final JFreeChart chart = ChartFactory.createXYLineChart(
            title,      				// chart title
            ax,                      	// x axis label
            ay,                      	// y axis label
            dataset,                  	// data
            PlotOrientation.VERTICAL,
            true,                     	// include legend
            true,                     	// tooltips
            false                     	// urls
        );

        chart.setBackgroundPaint(Color.white);

        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesShapesVisible(2, false);
        plot.setRenderer(renderer);

        // change the auto tick unit selection to integer units only...
        //final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        //rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        // OPTIONAL CUSTOMISATION COMPLETED.
                        
        return chart;
        
    }

    //--- Main Methods
    public void plot() {
    	this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }
    
}
