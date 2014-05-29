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

        private final int numCpus;
        private final int amountMemInMb;
        private final long requestId;

        public Request(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
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

        public AvailableResourcesResponse(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId = requestId;
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

    }

    // Shatha Review
    public static class ActualAllocationRequest extends Message {

        private final int numCpus;
        private final int amountMemInMb;
        private final long requestId;

        public ActualAllocationRequest(Address source, Address destination, int numCpus, int amountMemInMb, long requestId) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.requestId=  requestId;
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


    }
    
    
    public static class Response extends Message {

        private final boolean success;
        private final int numCpus;
        private final int amountMemInMb;
        public Response(Address source, Address destination, boolean success, int numAllocatedCpus, int numAllocatedMem) {
            super(source, destination);
            this.success = success;
            this.numCpus = numAllocatedCpus;
            this.amountMemInMb = numAllocatedMem;
        }
        
        // Shatha - Review
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
    }
    
    // Shatha - Review 
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
