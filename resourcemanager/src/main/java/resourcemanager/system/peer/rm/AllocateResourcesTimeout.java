package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class AllocateResourcesTimeout extends Timeout {

	private final Address peer;
	private final int numCpus;
	private final int amountMemInMb;
	private final long requestId;
	private long jobId; //needed in batch requests scenarios

	public AllocateResourcesTimeout(ScheduleTimeout request, Address peer,int numCpus, int amountMemInMb,long requestId) {
		super(request);
		this.peer = peer;
		this.numCpus = numCpus;
		this.amountMemInMb = amountMemInMb;
		this.requestId=  requestId;
	}
	
	// Needed for Batch Requests Scenarios
	public AllocateResourcesTimeout(ScheduleTimeout request, Address peer,int numCpus, int amountMemInMb,long requestId,long jobId) {
		super(request);
		this.peer = peer;
		this.numCpus = numCpus;
		this.amountMemInMb = amountMemInMb;
		this.requestId=  requestId;
		this.jobId = jobId;
	}

	public Address getPeer() {
		return peer;
	}
	
	public int getNumCpus()
	{
		return numCpus;
	}
	
	public int getAmountMemInMb()
	{
		return amountMemInMb;
	}
	
	public long getRequestId()
	{
		return requestId;
	}
	
	public long getJobId()
	{
		return jobId;
	}
}
