package common.simulation.scenarios;

import java.util.ArrayList;
import java.util.Random;

import common.simulation.BatchRequestResources;
import common.simulation.Job;
import common.simulation.PeerFail;
import common.simulation.PeerJoin;
import common.simulation.RequestResource;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation3;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation4;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation5;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;

@SuppressWarnings("serial")
public class Operations {

    public static Operation3<PeerJoin, Long, Long, Long> peerJoin() {
        return new Operation3<PeerJoin, Long, Long, Long>() {
            @Override
            public PeerJoin generate(Long id, Long numCpus, Long memInMbs) {
                return new PeerJoin(id, numCpus.intValue(), memInMbs.intValue());
            }
        };
    }

    public static Operation1<PeerFail, Long> peerFail = new Operation1<PeerFail, Long>() {
        @Override
        public PeerFail generate(Long id) {
            return new PeerFail(id);
        }
    };

    public static Operation<TerminateExperiment> terminate = new Operation<TerminateExperiment>() {
        @Override
        public TerminateExperiment generate() {
        	Statistics singleResourceInstance = Statistics.getSingleResourceInstance(); 
        	System.out.println("Amount of spawned Requests: " + singleResourceInstance.getSpawnCount());
        	System.out.println("Amount of received Requests: " + singleResourceInstance.getRcvdRqstCount());
        	System.out.println("Amount of available Resources Msgs: " + singleResourceInstance.getAvlblResCount());
        	System.out.println("Amount of actual Allocations: " + singleResourceInstance.getAllocReqCount());
        	System.out.println("Amount of Resource releases: " + singleResourceInstance.getReleaseResCount());
        	System.out.println("Amount of reRequests: " + singleResourceInstance.getReReqCount());
        	System.out.println("Amount of Measurements: " + singleResourceInstance.getAmountOfMeasurements());
			System.out.println("Average delay: " + singleResourceInstance.getAvg());
			System.out.println("99th percentile delay: " + singleResourceInstance.getNinetyNinth());
			singleResourceInstance.write();
            return new TerminateExperiment();
        }
    };

    public static Operation4<RequestResource, Long, Long, Long, Long> requestResources() {
        return new Operation4<RequestResource, Long, Long, Long, Long>() {
            @Override
            public RequestResource generate(Long id, Long numCpus, Long memInMbs,
                    Long timeToHoldResourceInMilliSecs) {
                return new RequestResource(id, numCpus.intValue(),
                        memInMbs.intValue(),
                        timeToHoldResourceInMilliSecs.intValue());
            }
        };
    }
    
    // New Operation for Batch Requests Task
    public static Operation5<BatchRequestResources, Long, Long, Long, Long, Long> batchRequestResources() {
        return new Operation5<BatchRequestResources, Long, Long, Long, Long,Long>() {
            @Override
            public BatchRequestResources generate(Long id, Long numCpus, Long memInMbs,
                    Long timeToHoldResourceInMilliSecs, Long numOfMachines) {

            	// Give me X machines,  each with y Cpus and Z memory
            	// machine == job 
            	ArrayList<Job> jobs = new ArrayList<Job>();
            	Job j;
            	long jobId;
            	long seed = (long) (System.currentTimeMillis() * Math.random());
    			Random objRandom = new Random(seed);
    			
            	for(int i=0;i<numOfMachines; i++)
            	{ 
        			jobId = objRandom.nextInt(1000000); //random job id 
            		j=new Job(id,jobId,numCpus, memInMbs, timeToHoldResourceInMilliSecs, false);
            		jobs.add(j);
            	}
                return new BatchRequestResources(id,jobs);
            }
        };
    }
}
