package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class RespondPeerTimeout extends Timeout {

	private long requestId;
	
	public RespondPeerTimeout(ScheduleTimeout request, long requestId) {
		super(request);
		this.requestId = requestId;
	}
	
	public long getRequestId()
	{
		return this.requestId;
	}
}
