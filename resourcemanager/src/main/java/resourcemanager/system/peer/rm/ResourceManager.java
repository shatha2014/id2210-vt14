package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import common.simulation.scenarios.Statistics;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import tman.system.peer.tman.ExchangeMsg;
import tman.system.peer.tman.TManPeerDescriptor;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

	protected static final boolean TMAN = true;
    
    Positive<RmPort> indexPort = positive(RmPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
    
    long requestTimestamp;
    long requestId;
    private  int timeToHoldResource;
    private int PROBESIZE = 5; 
    private int requestedCPUs;
    private int requestedMemory;
    
    
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

    private void initiateRequest()
    {
    	 System.out.println("HANDLE REQUEST RESOURCE: Sending Allocate resources: " + requestedCPUs + " + " + requestedMemory);
         
         requestTimestamp = System.currentTimeMillis();
         Statistics.getSingleResourceInstance().incSpawnCount();
         
         // Shatha - Review
         // 1. Select PROBESIZE random peers from the current neighbors
			ArrayList<Address> selectedNeighbors = new ArrayList<Address>();
			 long seed = System.currentTimeMillis();
			 seed = (long) (seed * Math.random());
			Random objRandom = new Random(seed);
			if (neighbours != null && neighbours.size() > 0) {
				for (int i = 0; i < PROBESIZE; i++) {
					selectedNeighbors.add(neighbours.get(objRandom
							.nextInt(neighbours.size())));	
				}
			}
			
			// 2. Send the Request to those selected neighbors
			if (selectedNeighbors != null) {
				for (Address objAddress : selectedNeighbors) {
					RequestResources.Request objRequest = new RequestResources.Request(
							self, objAddress, requestedCPUs,
							requestedMemory);
					trigger(objRequest, networkPort);
					
					// print out results for tracking reasons
					System.out.println("[" + self.getId() + "]" + " sending a request to the following neighbor [" + objAddress.getId() + "]");
				}
			}
			
    }

    // Step 1: on receiving a request, the node will send a probe randomly to a group
    // of its neighbours asking them about the availability of the requested resources in the scenario
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        

		@Override
        public void handle(RequestResource event) {
            requestedCPUs = event.getNumCpus();
            requestedMemory = event.getMemoryInMbs();
            timeToHoldResource = event.getTimeToHoldResource();
            requestId = event.getId();
            
            initiateRequest();
        }
    };
    
    
    
    // Step 2: On receiving a request, the node will send a response with its current resources 
    // so that the original node can decide the nodes with highest free resources 
    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {
        	
        	Statistics.getSingleResourceInstance().incRcvdRqstCount();
        	
        	// Printing out on the screen for tracking purpose   
        	System.out.println("HANDLE RESORUCE ALLOCATION REQUEST: " +
                 "[" + event.getDestination().getId() + "]" + " received a request from : [" + event.getSource().getId() + "]" +
                 " and i have the following resources " + availableResources.getNumFreeCpus() + " and " + availableResources.getFreeMemInMbs());
        	  
			// Shatha - Review
			// when the node received a request to allocate resources, it should
			// return the number of free CPUs and memory it has
			RequestResources.AvailableResourcesResponse objAvailableResourcesResponse = new RequestResources.AvailableResourcesResponse(
					self, event.getSource(),
					event.getNumCpus(),
					event.getAmountMemInMb());
			trigger(objAvailableResourcesResponse, networkPort);
        	
        }
    };
    
    // Step 3: The original node will start collecting responses from the nodes it probed
    // to decide the node with the highest number of resources
    private ArrayList<ResourcesComparator> grouping = new ArrayList<ResourcesComparator>();
    private ArrayList<Address> sentRequestsAddresses = new ArrayList<Address>();
    Handler<RequestResources.AvailableResourcesResponse> handleAvailableResourcesResponse = new Handler<RequestResources.AvailableResourcesResponse>() {
        @Override
        public void handle(RequestResources.AvailableResourcesResponse event) {
         /// printing out for tracking reasons .. 
        	Statistics.getSingleResourceInstance().incAvlblResCount();
        	
        	System.out.println("HANDLE AVAILABLE RESOURCES RESPONSE, event source is [" + event.getSource().getId() + "]" +
        			" and event destination is [" + event.getDestination().getId() + "]" + " and i will try to compare to find the node with " +
        			" the highest number of free resources ... " );
        	
        	grouping.add(new ResourcesComparator(event.getNumCpus(), event
					.getAmountMemInMb(), event.getSource()));
			// you received responses from all nodes
			// but what if one of them didn't reply, should handle this ..
			int refSize = grouping.size();
//			if(refSize < PROBESIZE && refSize > 0)
//				refSize++;
			// Shatha to do --> handle timeout
			if (refSize == PROBESIZE) {
				// passing the resource comparator
				Collections.sort(grouping);
				//Object[] objArr = grouping.toArray();
				// send a resources allocation request for two nodes with the
				// highest number of free resources
				RequestResources.ActualAllocationRequest objActualAllocationRequest = new RequestResources.ActualAllocationRequest(
						self,
						grouping.get(0).getNodeAddress(),
						event.getNumCpus(), event.getAmountMemInMb());
				trigger(objActualAllocationRequest, networkPort);
				
				// send to the next highest one
//				if(PROBESIZE > 1)
//				{
//				RequestResources.ActualAllocationRequest objActualAllocationSecondRequest = new RequestResources.ActualAllocationRequest(
//						self,
//						grouping.get(1).getNodeAddress(),
//						event.getNumCpus(), event.getAmountMemInMb());
//				trigger(objActualAllocationSecondRequest, networkPort);
//				}
				// Add the addresses of the nodes to which you send so that 
				// you can cancel after receiving a success from any of them
				sentRequestsAddresses.add(grouping.get(0).getNodeAddress());
				if(PROBESIZE > 1) sentRequestsAddresses.add(grouping.get(1).getNodeAddress());

			}
        }
    };
    
    // Step 4: On receiving an allocation request, the node should check first if the request is not canceled
    // since the original node might cancel the request if it has been allocated resources from another node 
    // and then if the resources were available, the node will allocate them for the original node
    private int numAllocatedCpus = 0;
    private int amountAllocatedMem = 0;
    Handler<RequestResources.ActualAllocationRequest> handleActualAllocationRequest = new Handler<RequestResources.ActualAllocationRequest>() {
        @Override
        public void handle(RequestResources.ActualAllocationRequest event) {
			
        	// Printing out results for tracking reasons 
        	Statistics.getSingleResourceInstance().incAllocReqCount();
        	
        	System.out.println("HANDLE ACTUAL ALLOCATION REQUEST - event source [" + 
        	event.getSource().getId() + "] and destination [" + event.getDestination().getId() + "] and we are checking if "
        	+  "we have enough resources for the allocation so that we allocate them " +
        	" is canceled is " + isCanceled);
        	
        	
        	
        	// when receiving this request, the node should check if it has
			// the requested resources, then allocate them and send a response
			// back
        	
        	// before allocation
        	boolean success = availableResources.allocate(
        			event.getNumCpus(), event.getAmountMemInMb());
        	
//			if (!isCanceled && availableResources.isAvailable(event.getNumCpus(),
        	
        	if (success) {
				

				// trigger a response with success
				RequestResources.Response objResponse = new RequestResources.Response(
						self, event.getSource(), success, event.getNumCpus(), event.getAmountMemInMb());
				trigger(objResponse, networkPort);					  
			    
			    // Set the amounts of cpus and amount of memory so that they can be released later
			    numAllocatedCpus = event.getNumCpus();
			    amountAllocatedMem = event.getAmountMemInMb();
			    
			    // Printing out results for tracking
//			   System.out.println("Printing the success result " + success );
			   
			} else {
				RequestResources.Response objResponse = new RequestResources.Response(
						self, event.getSource(), success, event.getNumCpus(), event.getAmountMemInMb());
				trigger(objResponse, networkPort);	
			}
        }
    };
    
    // Step 5: when the original node receives a response from other node, if the result is success
    // then it has to send cancel request for the other node if it sent it to another node
    // otherwise print out a message that the resources have been allocated
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {
         // print out results for tracking reasons 
        	System.out.println("HANDLE RESOURCE ALLOCATION RESPONSE - trying to know if the event succeeded or not "
        		  + " event source [" + event.getSource().getId() + "] and the event destination is [" + event.getDestination().getId() +
        		  "] and the success result is " + event.getSuccess() );
        	
        	// if you receive a success from one of the nodes you sent to then
           // you should cancel the request for the other node
        	if(event.getSuccess()) {
        		//log scheduling delay
        		long delay = System.currentTimeMillis() - requestTimestamp;
        		Statistics.getSingleResourceInstance().addTime(delay);
 			   System.out.println("RESOURCES HAVE BEEN ALLOCATED FOR [" + self.getId() + "]");
 			   
        		
        		// Check to whom you should send the cancel request
//        		for(Address objAddress : sentRequestsAddresses)
//        		{
//        			if(objAddress != event.getSource())
//        			{
//        				// Send a cancel request
//        				RequestResources.CancelRequest objCancel = new RequestResources.CancelRequest(self,
//        						objAddress);
//        				trigger(objCancel, networkPort);
//        				
//        				
//        				// print out to track the results 
//        				System.out.println("triggering cancel request to [" + objAddress.getId() + "]");
//        			}
//        		}
        		
        		// Schedule a periodic time event to ensure that the sender doesn't reclaim the resources from you
        	  //  SchedulePeriodicTimeout objPeriodicTimeout = new SchedulePeriodicTimeout(configuration.getPeriod(), timeToHoldResource);
        	   // objPeriodicTimeout.setTimeoutEvent(new RenewTimeout(objPeriodicTimeout));
        	    //trigger(objPeriodicTimeout, timerPort);
        	    
        	    ScheduleTimeout rst = new ScheduleTimeout(timeToHoldResource);
        		rst.setTimeoutEvent(new AllocateResourcesTimeout(rst));
        		
        		// TODO: no periodic scheduling , once done , just reclaim the resources 
        		// time to hold resources 
        	}
        	else
        	{
        		
					Statistics.getSingleResourceInstance().incReReqCount();
                    initiateRequest();    
				
        	}
        }
    };
    
    // Step 6: if a node received a cancel request, it should set the variable isCanceled, that can 
    // be used later to decide if it should allocate resources or not 
    private boolean isCanceled = false;
    Handler<RequestResources.CancelRequest> handleCancelRequest= new Handler<RequestResources.CancelRequest>() {
        @Override
        public void handle(RequestResources.CancelRequest event) {
           isCanceled = true;
           System.out.println("[" + self.getId() + "] received a cancel request from [" + event.getSource().getId() + "]");
        }
    };
    
    // Handling cyclon samples and printing out a message
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
          //  System.out.println("Received samples: " + event.getSample().size());
           
        	if(!TMAN) {
        		
	            // receive a new list of neighbors
	            neighbours.clear();
	            
	            // changed
	            ArrayList<PeerDescriptor> partnersDescriptors = event.getSample();
	            ArrayList<Address> partners = new ArrayList<Address>();
	    		for (PeerDescriptor desc : partnersDescriptors)
	    			partners.add(desc.getAddress());
	            neighbours.addAll(partners);
	       	}

        }
    };
	

    // If the node receives a renew timeout from the holder of resources 
    // then it can renew, otherwise it should release the resources 
    /*
    private boolean isActive = false;
    Handler<RenewTimeout> handleRenewTimeout = new Handler<RenewTimeout>() {
        @Override
        public void handle(RenewTimeout event) {
           // 
           isActive = true;
        }
    };
    */
    
    
    Handler<AllocateResourcesTimeout> handleAllocateResourcesTimeout= new Handler<AllocateResourcesTimeout>() {
        @Override
        public void handle(AllocateResourcesTimeout event) {
         // if(!isActive)
          //{
        	  availableResources.release(numAllocatedCpus, amountAllocatedMem);
        	  System.out.println("[" + self.getId() + "]" + " is releasing resources ");
          //}
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
    
    
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
//        	System.out.println("Received TMan samples: " + event.getSample().size());
            
        	if(TMAN) {
        		
	            // receive a new list of neighbors
	            neighbours.clear();
	            
	            // changed
	            List<TManPeerDescriptor> partnersDescriptors = event.getSample();
	            List<Address> partners = new ArrayList<Address>();
	    		for (TManPeerDescriptor desc : partnersDescriptors)
	    			partners.add(desc.getAddress());
	            neighbours.addAll(partners);
        	}
        }
    };

}
