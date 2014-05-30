package resourcemanager.system.peer.rm;

import java.util.ArrayList;

import common.simulation.Job;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;


public class RespondPeerTimeout extends Timeout {

	private long requestId;
	private ArrayList<Job> jobs; // Needed for batch requests task
	
	public RespondPeerTimeout(ScheduleTimeout request, long requestId) {
		super(request);
		this.requestId = requestId;
	}
	
	// Needed for Batch Requests Task
	public RespondPeerTimeout(ScheduleTimeout request, long requestId, ArrayList<Job> jobs) {
		super(request);
		this.requestId = requestId;
		this.jobs = jobs;
	}
	
	public long getRequestId()
	{
		return this.requestId;
	}
	
	public ArrayList<Job> getJobs()
	{
		return this.jobs;
	}
}
