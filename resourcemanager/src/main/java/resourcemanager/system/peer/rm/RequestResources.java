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

        public Request(Address source, Address destination, int numCpus, int amountMemInMb) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
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

        public AvailableResourcesResponse(Address source, Address destination, int numCpus, int amountMemInMb) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

    }

    // Shatha Review
    public static class ActualAllocationRequest extends Message {

        private final int numCpus;
        private final int amountMemInMb;

        public ActualAllocationRequest(Address source, Address destination, int numCpus, int amountMemInMb) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

    }
    
    
    public static class Response extends Message {

        private final boolean success;
        public Response(Address source, Address destination, boolean success) {
            super(source, destination);
            this.success = success;
        }
        
        // Shatha - Review
        public boolean getSuccess()
        {
        	return this.success;
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
    
    // Shatha Review
    public class RenewTimeout extends Timeout {

    	public RenewTimeout(SchedulePeriodicTimeout request) {
    		super(request);
    	}

    }
}
