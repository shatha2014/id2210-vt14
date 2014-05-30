package common.simulation;

import java.util.List;

import se.sics.kompics.Event;

//**
// This class was added for the batch requests task
// Give me X machines --> is translated into an array list of 
// jobs, each with its own number of CPUs and Memory 
// for the same request id 
//**
public class BatchRequestResources extends Event {
	 
	// Main request id 
    private final long id;
    // X machines --> X jobs 
    private final List<Job> jobs;

    public BatchRequestResources(long requestId, List<Job> jobs) {
        this.id = requestId;
        this.jobs = jobs;
    }

    public long getRequestId() {
        return id;
    }
    
    public List<Job> getJobs()
    {
    	return jobs;
    }
}
