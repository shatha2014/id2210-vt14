package tman.system.peer.tman;

import java.util.UUID;

import cyclon.system.peer.cyclon.DescriptorBuffer;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

	// Shatha Review
	// Changes to replace DescriptorBuffer with TManDescriptorBuffer
    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
        private final UUID requestId;
        private final TManDescriptorBuffer randomBuffer;


        public Request(UUID requestId, TManDescriptorBuffer randomBuffer, Address source, 
                Address destination) {
            super(source, destination);
            this.requestId = requestId;
            this.randomBuffer = randomBuffer;
        }


        public UUID getRequestId() {
            return requestId;
        }

        
        public TManDescriptorBuffer getRandomBuffer() {
            return randomBuffer;
        }


        public int getSize() {
            return 0;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
        private final UUID requestId;
        private final TManDescriptorBuffer selectedBuffer;


        public Response(UUID requestId, TManDescriptorBuffer selectedBuffer, Address source, Address destination) {
            super(source, destination);
            this.requestId = requestId;
            this.selectedBuffer = selectedBuffer;
        }


        public UUID getRequestId() {
            return requestId;
        }


        public TManDescriptorBuffer getSelectedBuffer() {
            return selectedBuffer;
        }


        public int getSize() {
            return 0;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final Address peer;


        public RequestTimeout(ScheduleTimeout request, Address peer) {
            super(request);
            this.peer = peer;
        }


        public Address getPeer() {
            return peer;
        }
    }
}