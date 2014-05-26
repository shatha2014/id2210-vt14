package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
                
		StochasticProcess process0 = new StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			// Shatha modification - it was 3
			raise(3, Operations.peerJoin(), 
                                uniform(0, Integer.MAX_VALUE), 
                                constant(8), constant(12000)
                             );
		}};
                
		StochasticProcess process1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			// Shatha modification, it was 100
			raise(10, Operations.requestResources(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000),
                                constant(1000*60*1) // 1 minute
                                );
		}};
	
                
		StochasticProcess failPeersProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.peerFail, 
                                uniform(0, Integer.MAX_VALUE));
		}};
                
		StochasticProcess terminateProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		process0.start();
		process1.startAfterTerminationOf(2000, process0);
		failPeersProcess.startAfterTerminationOf(4000, process1);
		 
		terminateProcess.startAfterTerminationOf(1000, failPeersProcess);
//                terminateProcess.startAfterTerminationOf(100*1000, process1);
		
			Statistics singleResourceInstance = Statistics
					.getSingleResourceInstance();
			System.out.println("Average delay: "
					+ singleResourceInstance.getAvg());
			System.out.println("99th percentile delay: " + singleResourceInstance.getNinetyNinth());
	}};

	// -------------------------------------------------------------------
	public Scenario1() {
		super(scenario);
	}
}
