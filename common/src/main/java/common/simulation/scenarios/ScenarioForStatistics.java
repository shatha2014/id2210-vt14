package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class ScenarioForStatistics extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {
		{
			// IMPORTANT INFORAMTION
			// On Testing the Batch Requests scenarios , kindly apply the following
			// Navigate to resourcemanager project --> system.peer package --> Peer.java
			// Uncomment lines 136 and 137 , and comment out lines 138 and 139
			// same for lines 78 and 79
			//
			// we needed to separate the implementation of BatchRequests since
			// we changed the request and many other files, and the batch functionality
			// was implemented after we stabilized the first resource manager
			// so we didnt want to modify its code or change its logic to avoid 
			// any bugs or need for re-testing
			/////////////////////////////

			StochasticProcess process0 = new StochasticProcess() {
				{
					eventInterArrivalTime(constant(1000));
					raise(100, Operations.peerJoin(),
							uniform(0, Integer.MAX_VALUE), constant(8),
							constant(12000));
				}
			};

			StochasticProcess process1 = new StochasticProcess() {
				{
					eventInterArrivalTime(constant(100));
					raise(200, Operations.requestResources(),
							uniform(0, Integer.MAX_VALUE), constant(2),
							constant(2000), constant(1000 * 60 * 1) // 1 minute
					);
				}
			};

			// Batch Requests
			// 5000 requests : Give me 2 machines, each with 4 Cpus, and 3000 mem
			StochasticProcess batchRequestsProcess = new StochasticProcess() {
				{
					eventInterArrivalTime(constant(100));
					raise(5000, Operations.batchRequestResources(),
							uniform(0, Integer.MAX_VALUE), constant(4),
							constant(3000), constant(60000) // 1 minute
							, constant(2));
				}
			};

			StochasticProcess failPeersProcess = new StochasticProcess() {
				{
					eventInterArrivalTime(constant(100));
					raise(1, Operations.peerFail, uniform(0, Integer.MAX_VALUE));
				}
			};

			StochasticProcess terminateProcess = new StochasticProcess() {
				{
					eventInterArrivalTime(constant(100));
					raise(1, Operations.terminate);
				}
			};
			process0.start();
			process1.startAfterTerminationOf(2000, process0);
			
			//batchRequestsProcess.startAfterTerminationOf(2000, process0);
			
			failPeersProcess.startAfterTerminationOf(4000, process1);
			terminateProcess.startAfterTerminationOf(1000, failPeersProcess);
		}
	};

	// -------------------------------------------------------------------
	public ScenarioForStatistics() {
		super(scenario);
	}
}
