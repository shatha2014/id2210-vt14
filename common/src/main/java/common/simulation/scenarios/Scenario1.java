package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
                
		StochasticProcess process0 = new StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			// Shatha modification - it was 100 - 6 - Success 7 - Success 100 - 300
			raise(300, Operations.peerJoin(), 
                                uniform(0, Integer.MAX_VALUE), 
                                constant(8), constant(12000)
                             );
//			raise(300, Operations.peerJoin(), 
//                    uniform(0, Integer.MAX_VALUE), 
//                    uniform(2, 8), uniform(4000,16000)
//                 );
		}};
                
		
		
		StochasticProcess process1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			// Shatha modification, it was 100
//			raise(150, Operations.requestResources(), 
//                                uniform(0, Integer.MAX_VALUE),
//                                uniform(2, 8), uniform(1000, 12000),
//                                constant(1000*60*1) // 1 minute
//                                );
			// was 150 - 2 - Successs 3 - Success 150 - 500
			raise(5000, Operations.requestResources(), 
					uniform(0, Integer.MAX_VALUE),
					constant(4), constant(3000),
					constant(60000) // 1 minute
					);
		}};
	
          
		
		StochasticProcess failPeersProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(2, Operations.peerFail, 
                                uniform(0, Integer.MAX_VALUE));
		}};
                
		StochasticProcess terminateProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		process0.start();
		process1.startAfterTerminationOf(2000, process0);
//		failPeersProcess.startAfterTerminationOf(4000, process1);
//		 
//		terminateProcess.startAfterTerminationOf(1000, failPeersProcess);
         terminateProcess.startAfterTerminationOf(300*1000, process1); // was 200000
		
			
	}};

	// -------------------------------------------------------------------
	public Scenario1() {
		super(scenario);
	}
}
