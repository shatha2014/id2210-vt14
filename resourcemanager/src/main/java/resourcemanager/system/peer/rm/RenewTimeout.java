package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

// Unused anymore TO be deleted
public class RenewTimeout extends Timeout {

	public RenewTimeout(SchedulePeriodicTimeout request) {
		super(request);
	}
	
	public RenewTimeout(ScheduleTimeout request) {
		super(request);
	}

}
