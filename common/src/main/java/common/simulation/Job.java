package common.simulation;

// In Batch Requests Task , we have to handle 
// the following : give me X machines,  each with Y Cpus and Z memory
// so the machine is interpreted as a job 
public class Job {

	    // main request id 
	    private final long requestId;
	    // information about the job itself 
	    private final long jobId;
	    private  long numCpus;
	    private  long memoryInMbs;
	    private  long timeToHoldResource;
	    private  boolean status; //has its resources allocated or still not 
	    
	    public Job(long requestId,long jobId, long numCpus, long memoryInMbs, long timeToHoldResource, boolean status) {
	        this.requestId = requestId;
	        this.jobId = jobId;
	        this.numCpus = numCpus;
	        this.memoryInMbs = memoryInMbs;
	        this.timeToHoldResource = timeToHoldResource;
	        this.status = status;
	    }
	    
	    public long getRequestId() {
	        return requestId;
	    }

	    public long getTimeToHoldResource() {
	        return timeToHoldResource;
	    }

	    public long getMemoryInMbs() {
	        return memoryInMbs;
	    }

	    public long getNumCpus() {
	        return numCpus;
	    }
	    
	    public long getJobId()
	    {
	    	return jobId;
	    }
	    
	    public boolean getStatus()
	    {
	    	return status;
	    }
	    
	    public void setStatus(boolean val)
	    {
	    	this.status = val;
	    }
}
