package resourcemanager.system.peer.rm;

import java.util.List;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * User: jdowling
 */
public class RequestResources  {

    public static class Request extends Message {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7739878945391947572L;
		private final int numCpus;
        private final int amountMemInMb;
        private final long requestId;
        private long jobId; // Needed for Batch Requests Task

        public Request(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
        }
        
        // Needed for Batch Requests Task
        public Request(Address source, Address destination, int numCpus, int amountMemInMb, long requestId,long jobId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
            this.jobId = jobId;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
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
    
    // Shatha - Review 
    public static class AvailableResourcesResponse extends Message {

        /**
		 * This class is added to handle the message that should be 
		 * sent in a response of an allocation request to indicate the
		 * number of free cpus and messages the node has, then 
		 * the requesting node will be able to decide the node 
		 * with highest number of cpus and memory
		 */
		private static final long serialVersionUID = -6796641553813578244L;
		private final int numCpus;
        private final int amountMemInMb;
        private final long requestId;
        private long jobId; //Needed for batch requests task

        public AvailableResourcesResponse(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
        }
        
        // Needed for Batch Requests Task
        public AvailableResourcesResponse(Address source, Address destination, int numCpus, int amountMemInMb, long requestId,long jobId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
            this.jobId = jobId;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
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

    // Shatha Review
    public static class ActualAllocationRequest extends Message {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7933801285791383845L;
		private final int numCpus;
        private final int amountMemInMb;
        private final long requestId;
        private long jobId; //Needed for batch requests task

        public ActualAllocationRequest(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId=  requestId;
        }

        // Needed for batch requests task
        public ActualAllocationRequest(Address source, Address destination, int numCpus, int amountMemInMb, long requestId, long jobId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId=  requestId;
            this.jobId = jobId;
        }

        
        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
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
    
    
    public static class Response extends Message {

        /**
		 * 
		 */
		private static final long serialVersionUID = -5600906797397889357L;
		private final boolean success;
        private final int numCpus;
        private final int amountMemInMb;
        private long jobId;//Needed for batch requests task
        
        public Response(Address source, Address destination, boolean success, int numAllocatedCpus, int numAllocatedMem) {
            super(source, destination);
            this.success = success;
            this.numCpus = numAllocatedCpus;
            this.amountMemInMb = numAllocatedMem;
        }
        
        // Needed for batch requests task
        public Response(Address source, Address destination, boolean success, int numAllocatedCpus, int numAllocatedMem, long jobId) {
            super(source, destination);
            this.success = success;
            this.numCpus = numAllocatedCpus;
            this.amountMemInMb = numAllocatedMem;
            this.jobId=  jobId;
        }
        
        public boolean getSuccess()
        {
        	return this.success;
        }
        
        public int getNumCpus()
        {
        	return this.numCpus;
        }
        
        public int getAmountMemInMb()
        {
        	return this.amountMemInMb;
        }
        
        public long getJobId()
        {
        	return this.jobId;
        }
    }

    // To Be Deleted - not used anymore
    public static class CancelRequest extends Message {

        /**
		 * This message should be used to cancel any active request
		 * after receiving a response from one of the nodes
		 */
		private static final long serialVersionUID = -4694274366562594138L;

		public CancelRequest(Address source, Address destination) {
            super(source, destination);
        }
    }
    
    public static class RequestTimeout extends Timeout {
        private final Address destination;
        RequestTimeout(ScheduleTimeout st, Address destination) {
            super(st);
            this.destination = destination;
        }

        public Address getDestination() {
            return destination;
        }
    }
    
   
}
