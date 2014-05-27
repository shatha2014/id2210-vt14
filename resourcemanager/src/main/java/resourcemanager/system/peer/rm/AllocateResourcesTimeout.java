package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class AllocateResourcesTimeout extends Timeout {

	private final Address peer;
	private final int numCpus;
	private final int amountMemInMb;

	public AllocateResourcesTimeout(ScheduleTimeout request, Address peer,int numCpus, int amountMemInMb) {
		super(request);
		this.peer = peer;
		this.numCpus = numCpus;
		this.amountMemInMb = amountMemInMb;
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
}
