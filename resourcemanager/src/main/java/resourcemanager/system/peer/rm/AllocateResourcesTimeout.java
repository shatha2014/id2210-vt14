package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class AllocateResourcesTimeout extends Timeout {

	private final Address peer;

	public AllocateResourcesTimeout(ScheduleTimeout request, Address peer) {
		super(request);
		this.peer = peer;
	}

	public Address getPeer() {
		return peer;
	}
}
