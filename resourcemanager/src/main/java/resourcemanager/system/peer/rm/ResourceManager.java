package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.RequestResources.RenewTimeout;
import resourcemanager.system.peer.rm.RequestResources.RequestTimeout;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    Positive<RmPort> indexPort = positive(RmPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
    ArrayList<Address> neighbours = new ArrayList<Address>();
    private Address self;
    private RmConfiguration configuration;
    Random random;
    private AvailableResources availableResources;
    Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
        @Override
        public int compare(PeerDescriptor t, PeerDescriptor t1) {
            if (t.getAge() > t1.getAge()) {
                return 1;
            } else {
                return -1;
            }
        }
    };
    // Shatha Review
    private  int timeToHoldResource;

	
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleUpdateTimeout, timerPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(handleTManSample, tmanPort);
        // Shatha Review
        subscribe(handleActualAllocationRequest, networkPort);
        subscribe(handleAvailableResourcesResponse, networkPort);
        subscribe(handleCancelRequest, networkPort);
        
    }
	
    Handler<RmInit> handleInit = new Handler<RmInit>() {
        @Override
        public void handle(RmInit init) {
            self = init.getSelf();
            configuration = init.getConfiguration();
            random = new Random(init.getConfiguration().getSeed());
            availableResources = init.getAvailableResources();
            long period = configuration.getPeriod();
            availableResources = init.getAvailableResources();
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateTimeout(rst));
            trigger(rst, timerPort);
            // Shatha added
            timeToHoldResource = 0;
        }
    };


    // Shatha question - discuss this with jasper
    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
            if (neighbours.isEmpty()) {
                return;
            }
            Address dest = neighbours.get(random.nextInt(neighbours.size()));
        }
    };
    
    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {
			// Shatha - Review
			// when the node received a request to allocate resources, it should
			// return the number of free CPUs and memory it has
			RequestResources.AvailableResourcesResponse objAvailableResourcesResponse = new RequestResources.AvailableResourcesResponse(
					self, event.getSource(),
					availableResources.getNumFreeCpus(),
					availableResources.getFreeMemInMbs());
			trigger(objAvailableResourcesResponse, networkPort);
        	
        }
    };
    
    // Shatha - Review 
    private ArrayList<ResourcesComparator> grouping = new ArrayList<ResourcesComparator>();
    private ArrayList<Address> sentRequestsAddresses = new ArrayList<Address>();
    Handler<RequestResources.AvailableResourcesResponse> handleAvailableResourcesResponse = new Handler<RequestResources.AvailableResourcesResponse>() {
        @Override
        public void handle(RequestResources.AvailableResourcesResponse event) {
			grouping.add(new ResourcesComparator(event.getNumCpus(), event
					.getAmountMemInMb(), event.getSource()));
			// you received responses from all nodes
			// but what if one of them didn't reply, should handle this ..
			if (grouping.size() == 4) {
				Object[] objArr = grouping.toArray();
				Arrays.sort(objArr);

				// send a resources allocation request for two nodes with the
				// highest number of free resources
				RequestResources.ActualAllocationRequest objActualAllocationRequest = new RequestResources.ActualAllocationRequest(
						self,
						((ResourcesComparator) objArr[0]).getNodeAddress(),
						event.getNumCpus(), event.getAmountMemInMb());
				trigger(objActualAllocationRequest, networkPort);
				
				// send to the next highest one
				RequestResources.ActualAllocationRequest objActualAllocationSecondRequest = new RequestResources.ActualAllocationRequest(
						self,
						((ResourcesComparator) objArr[1]).getNodeAddress(),
						event.getNumCpus(), event.getAmountMemInMb());
				trigger(objActualAllocationSecondRequest, networkPort);
				
				// Add the addresses of the nodes to which you send so that 
				// you can cancel after receiving a success from any of them
				sentRequestsAddresses.add(((ResourcesComparator) objArr[0]).getNodeAddress());
				sentRequestsAddresses.add(((ResourcesComparator) objArr[1]).getNodeAddress());
			}
        }
    };
    
    // Shatha Review
    private int numAllocatedCpus = 0;
    private int amountAllocatedMem = 0;
    Handler<RequestResources.ActualAllocationRequest> handleActualAllocationRequest = new Handler<RequestResources.ActualAllocationRequest>() {
        @Override
        public void handle(RequestResources.ActualAllocationRequest event) {
			// when receiving this request, the node should check if it has
			// the requested resources, then allocate them and send a response
			// back
			if (!isCanceled && availableResources.isAvailable(event.getNumCpus(),
					event.getAmountMemInMb())) {
				// Shatha - question
				// how does the node ensure that the resources are allocated for
				// the exact requesting node ?? or is it just a simulation for the
				// situation ?
				boolean success = availableResources.allocate(
						event.getNumCpus(), event.getAmountMemInMb());

				// trigger a response with success
				RequestResources.Response objResponse = new RequestResources.Response(
						self, event.getSource(), success);
				trigger(objResponse, networkPort);					  
			    
			    // Set the amounts of cpus and amount of memory so that they can be released later
			    numAllocatedCpus = event.getNumCpus();
			    amountAllocatedMem = event.getAmountMemInMb();
			}
        }
    };
    

    
    
    // Shatha Review 
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {
           // if you receive a success from one of the nodes you sent to then
           // you should cancel the request for the other node
        	if(event.getSuccess())
        	{
        		// Check to whom you should send the cancel request
        		for(Address objAddress : sentRequestsAddresses)
        		{
        			if(objAddress != event.getSource())
        			{
        				// Send a cancel request
        				RequestResources.CancelRequest objCancel = new RequestResources.CancelRequest(self,
        						objAddress);
        				trigger(objCancel, networkPort);
        			}
        		}
        		
        		// Schedule a periodic time event to ensure that the sender doesn't reclaim the resources from you
        	  //  SchedulePeriodicTimeout objPeriodicTimeout = new SchedulePeriodicTimeout(timeToHoldResource, configuration.getPeriod());
        	  //  objPeriodicTimeout.setTimeoutEvent(new RenewTimeout(objPeriodicTimeout));
        	  //  trigger(objPeriodicTimeout, timerPort);
                       	   
        	
        	}
        }
    };
    
    // Shatha Review 
    private boolean isCanceled = false;
    Handler<RequestResources.CancelRequest> handleCancelRequest= new Handler<RequestResources.CancelRequest>() {
        @Override
        public void handle(RequestResources.CancelRequest event) {
           isCanceled = true;
        }
    };
    
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            System.out.println("Received samples: " + event.getSample().size());
            
            // receive a new list of neighbors
            neighbours.clear();
            neighbours.addAll(event.getSample());

        }
    };
	
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {
            
            System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs());
            // TODO: Ask for resources from neighbors
            // by sending a ResourceRequest
            // RequestResources.Request req = new RequestResources.Request(self, dest,
            // event.getNumCpus(), event.getAmountMem());
            // trigger(req, networkPort);
            
            
            // Shatha - Review
            // 1. Select 4 random peers from the current neighbors
			ArrayList<Address> selectedNeighbors = new ArrayList<Address>();
			Random objRandom = new Random();
			if (neighbours != null) {
				for (int i = 0; i < 4; i++) {
					selectedNeighbors.add(neighbours.get(objRandom
							.nextInt(neighbours.size())));
				}
			}
			// 2. Send the Request to those selected four neighbors
			if (selectedNeighbors != null) {
				for (Address objAddress : selectedNeighbors) {
					RequestResources.Request objRequest = new RequestResources.Request(
							self, objAddress, event.getNumCpus(),
							event.getMemoryInMbs());
					trigger(objRequest, networkPort);
				}
			}
			// 3. Set the time to hold resource value from the event
			timeToHoldResource = event.getTimeToHoldResource();
			
        }
    };
    
    
    
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // TODO: 
        }
    };

}