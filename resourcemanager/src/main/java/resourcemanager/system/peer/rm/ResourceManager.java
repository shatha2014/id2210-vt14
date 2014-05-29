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
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
import simulator.snapshot.PeerInfo;
import system.peer.RmPort;
import tman.system.peer.tman.TManPeerDescriptor;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 * 
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceManager.class);

	protected static final boolean TMAN = false;

	Positive<RmPort> indexPort = positive(RmPort.class);
	Positive<Network> networkPort = positive(Network.class);
	Positive<Timer> timerPort = positive(Timer.class);
	Negative<Web> webPort = negative(Web.class);
	Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);

	long requestTimestamp;
	long requestId;
	private int timeToHoldResource;
	private final int PROBESIZE = 4;
	private int requestedCPUs;
	private int requestedMemory;
	// it should be equal to probe size, but
	// decreased when timeout
	private int requestProbeSize;

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
	private HashMap<String, Boolean> requestStatuses;
	private boolean done = false;

	public ResourceManager() {

		subscribe(handleInit, control);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleRequestResource, indexPort);
		subscribe(handleResourceAllocationRequest, networkPort);
		subscribe(handleResourceAllocationResponse, networkPort);
		subscribe(handleTManSample, tmanPort);
		// Shatha Review
		subscribe(handleActualAllocationRequest, networkPort);
		subscribe(handleAvailableResourcesResponse, networkPort);
		subscribe(handleCancelRequest, networkPort);
		subscribe(handleAllocateResourcesTimeout, timerPort);
		subscribe(handleRespondPeerTimeout, timerPort);

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
			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new UpdateTimeout(rst));
			trigger(rst, timerPort);
			// Shatha added
			timeToHoldResource = 0;
			requestStatuses = new HashMap<String, Boolean>();
		}
	};

	// On receiving a request, the node will send a probe randomly to a
	// group of its neighbours asking them about the availability of the
	// requested
	// resources in the scenario
	private void initiateRequest(long requId) {
		System.out.println("////" + requId + "////"
				+ "HANDLE REQUEST RESOURCE : Sending Allocate resources: "
				+ requestedCPUs + " + " + requestedMemory);

		// 1. Select PROBESIZE random peers from the current neighbors
		ArrayList<Address> selectedNeighbors = new ArrayList<Address>();
		long seed = (long) (System.currentTimeMillis() * Math.random());
		Random objRandom = new Random(seed);
		if (neighbours != null && neighbours.size() > 0) {
			for (int i = 0; i < neighbours.size(); i++) {
				System.out.println("------REQUEST ID----- " + requId
						+ " NEIGHBORS: " + neighbours.get(i).getId());
			}

			// to ensure unique nodes
			Address nodeAddress;
			/*
			 * for (int i = 0; i < PROBESIZE; i++) { nodeAddress =
			 * neighbours.get(objRandom.nextInt(neighbours .size())); if
			 * (!selectedNeighbors.contains(nodeAddress) &&
			 * !selectedAddresses.contains(nodeAddress))
			 * selectedNeighbors.add(nodeAddress); else {
			 * System.out.println("SHAZA...."); --i; } }
			 */

			int counter = 0;
			while (counter < PROBESIZE) {
				nodeAddress = neighbours.get(objRandom.nextInt(neighbours
						.size()));
				// if (!selectedNeighbors.contains(nodeAddress))
				// {
				selectedNeighbors.add(nodeAddress);
				counter++;
				// }

			}

			respondingPeers.clear();

		}

		// 2. Send the Request to those selected neighbors
		if (selectedNeighbors != null) {
			for (Address objAddress : selectedNeighbors) {
				RequestResources.Request objRequest = new RequestResources.Request(
						self, objAddress, requestedCPUs, requestedMemory,
						requId);
				trigger(objRequest, networkPort);

				// print out results for tracking reasons
				System.out.println("////" + requId + "////" + "--- "
						+ selectedNeighbors.size() + "[" + self.getId() + "]"
						+ " sending a request to the following neighbor ["
						+ objAddress.getId() + "]");
			}
		}

		// to keep the status of the request
		requestStatuses.put(String.valueOf(requId), false);

	}

	// Step 1: On receiving a request
	Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {

		@Override
		public void handle(RequestResource event) {
			requestedCPUs = event.getNumCpus();
			requestedMemory = event.getMemoryInMbs();
			timeToHoldResource = event.getTimeToHoldResource();
			requestId = event.getId();

			// log new requests to statistic
			requestTimestamp = System.currentTimeMillis();
			Statistics.getSingleResourceInstance().incSpawnCount();

			initiateRequest(requestId);

			requestProbeSize = PROBESIZE;
			if (PROBESIZE > neighbours.size())
				requestProbeSize = neighbours.size();
			System.out.println("//" + requestId + " // "
					+ " REQUEST PROBE SIZE IS SSSSS " + requestProbeSize);

			// Test REVIEWWWW
			// Schedule a timeout for the probe size to decrease it in case
			// one of the nodes didn't reply within time
			ScheduleTimeout rst = new ScheduleTimeout(180000); // 30000
			rst.setTimeoutEvent(new RespondPeerTimeout(rst, requestId));
			trigger(rst, timerPort);

		}
	};

	// Step 2: On receiving a request, the node will send a response with its
	// current resources
	// so that the original node can decide the nodes with highest free
	// resources
	Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
		@Override
		public void handle(RequestResources.Request event) {

			Statistics.getSingleResourceInstance().incRcvdRqstCount();

			// Printing out on the screen for tracking purpose
			System.out.println("////" + event.getRequestId() + "////"
					+ "HANDLE RESORUCE ALLOCATION REQUEST: " + "["
					+ event.getDestination().getId() + "]"
					+ " received a request from : ["
					+ event.getSource().getId() + "]"
					+ " and i have the following resources "
					+ availableResources.getNumFreeCpus() + " and "
					+ availableResources.getFreeMemInMbs());

			// when the node received a request to allocate resources, it should
			// return the number of free CPUs and memory it has
			RequestResources.AvailableResourcesResponse objAvailableResourcesResponse = new RequestResources.AvailableResourcesResponse(
					self, event.getSource(), event.getNumCpus(),
					event.getAmountMemInMb(), event.getRequestId());
			trigger(objAvailableResourcesResponse, networkPort);

		}
	};

	// Test REVIEWWWW
	// On timeout , reset the requestProbeSize to be equal to the current
	// incremented
	// size to avoid any delays
	Handler<RespondPeerTimeout> handleRespondPeerTimeout = new Handler<RespondPeerTimeout>() {
		@Override
		public void handle(RespondPeerTimeout event) {
			// requestProbeSize = PROBESIZE;
			// requestProbeSize--;

			// if no respond was received then reinitiate the request
			if (!requestStatuses.get(String.valueOf(event.getRequestId()))) {
				System.out.println(" TIMEOUT " + event.getRequestId()
						+ " RESPONDING PEER SIZE IS " + respondingPeers.size());
				respondingPeers.clear();
				initiateRequest(event.getRequestId());
			}
			System.out.println("// " + requestId + " // "
					+ "Request Probe Size became " + requestProbeSize);
		}
	};

	// Step 3: The original node will start collecting responses from the nodes
	// it probed
	// to decide the node with the highest number of resources
	// ToDelete
	private ArrayList<ResourcesComparator> grouping = new ArrayList<ResourcesComparator>();
	private ArrayList<Address> sentRequestsAddresses = new ArrayList<Address>();
	private ArrayList<PeerInfo> respondingPeers = new ArrayList<PeerInfo>();
	Handler<RequestResources.AvailableResourcesResponse> handleAvailableResourcesResponse = new Handler<RequestResources.AvailableResourcesResponse>() {
		@Override
		public void handle(RequestResources.AvailableResourcesResponse event) {
			// / printing out for tracking reasons ..
			Statistics.getSingleResourceInstance().incAvlblResCount();

			System.out.println("////" + requestId + "////"
					+ "HANDLE AVAILABLE RESOURCES RESPONSE, event source is ["
					+ event.getSource().getId() + "]"
					+ " and event destination is ["
					+ event.getDestination().getId() + "]"
					+ " and i will try to compare to find the node with "
					+ " the highest number of free resources ... ");

			// add the responding peer to the group
			PeerInfo peer = new PeerInfo(new AvailableResources(
					event.getNumCpus(), event.getAmountMemInMb()));
			peer.setAddress(event.getSource());
			respondingPeers.add(peer);

			System.out.println("The value of request id " + requestId + " is "
					+ requestStatuses.get(String.valueOf(requestId)));

			System.out.println("// " + requestId + " // "
					+ " REQUEST PROBE SIZE is " + requestProbeSize
					+ " AND NEIGHBORS are " + neighbours.size());
			System.out.println("TIME " + System.currentTimeMillis());
			if (PROBESIZE > neighbours.size())
				requestProbeSize = neighbours.size();

			System.out
					.println("RESPONDING PEER SIZE " + respondingPeers.size());
			if (respondingPeers.size() == requestProbeSize &&
					!requestStatuses.get(String.valueOf(event.getRequestId()))) {
				System.out.println("// " + requestId + " // "
						+ " NOW SIZE AND REQUESTPROBESIZE ARE "
						+ requestProbeSize);

				// passing the resource comparator
				Collections
						.sort(respondingPeers, new ResourcesComparator(peer));
                 
				// send a resources allocation request for the node with the
				// highest number of free resources
				RequestResources.ActualAllocationRequest objActualAllocationRequest = new RequestResources.ActualAllocationRequest(
						self, respondingPeers.get(0).getAddress(),
						event.getNumCpus(), event.getAmountMemInMb(), requestId);
				trigger(objActualAllocationRequest, networkPort);

				// Add the addresses of the nodes to which you send so that
				// you can cancel after receiving a success from any of them
				sentRequestsAddresses.add(respondingPeers.get(0).getAddress());

			}
		}
	};

	// Step 4: On receiving an allocation
	// if the resources were available, the node will allocate them for
	// the original node
	private int numAllocatedCpus = 0;
	private int amountAllocatedMem = 0;
	Handler<RequestResources.ActualAllocationRequest> handleActualAllocationRequest = new Handler<RequestResources.ActualAllocationRequest>() {
		@Override
		public void handle(RequestResources.ActualAllocationRequest event) {

			// Printing out results for tracking reasons

			done = true;

			System.out
					.println("////"
							+ event.getRequestId()
							+ "////"
							+ "HANDLE ACTUAL ALLOCATION REQUEST - event source ["
							+ event.getSource().getId()
							+ "] and destination ["
							+ event.getDestination().getId()
							+ "] and we are checking if "
							+ "we have enough resources for the allocation so that we allocate them "
							+ " is canceled is " + isCanceled);

			// when receiving this request, the node should check if it has
			// the requested resources, then allocate them and send a response
			// back

			// before allocation
			
				boolean success = availableResources.allocate(
						event.getNumCpus(), event.getAmountMemInMb());

				if (success) {
					
					Statistics.getSingleResourceInstance().incAllocReqCount();

					// trigger a response with success
					RequestResources.Response objResponse = new RequestResources.Response(
							self, event.getSource(), success,
							event.getNumCpus(), event.getAmountMemInMb());
					trigger(objResponse, networkPort);

					// Set the amounts of cpus and amount of memory so that they
					// can
					// be released later
					numAllocatedCpus = event.getNumCpus();
					amountAllocatedMem = event.getAmountMemInMb();

					// Schedule a timeout to release the resources once timeout
					ScheduleTimeout rst = new ScheduleTimeout(
							timeToHoldResource);
					rst.setTimeoutEvent(new AllocateResourcesTimeout(rst, event
							.getSource(), event.getNumCpus(), event
							.getAmountMemInMb(), event.getRequestId()));
					System.out.println("////" + event.getRequestId() + "////"
							+ "sending Job to node "
							+ event.getSource().getId());
					trigger(rst, timerPort);
					System.out.println("////" + event.getRequestId() + "////"
							+ ".... Sending timeout event to ... "
							+ event.getSource().getId());

				} else {
					// Send a response with failure in case the
					// resources were not allocated so that the node can send
					// the request again
					RequestResources.Response objResponse = new RequestResources.Response(
							self, event.getSource(), success,
							event.getNumCpus(), event.getAmountMemInMb());
					trigger(objResponse, networkPort);
				}
			
		}
	};

	// Step 5: when the original node receives a response from other node, if
	// the result is success
	// print out a message that the resources have been allocated
	// otherwise send the request again
	Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
		@Override
		public void handle(RequestResources.Response event) {
			// print out results for tracking reasons
			System.out
					.println("////"
							+ requestId
							+ "////"
							+ "HANDLE RESOURCE ALLOCATION RESPONSE - trying to know if the event succeeded or not "
							+ " event source [" + event.getSource().getId()
							+ "] and the event destination is ["
							+ event.getDestination().getId()
							+ "] and the success result is "
							+ event.getSuccess());

			if (event.getSuccess()) {

				// log scheduling delay
				long delay = System.currentTimeMillis() - requestTimestamp;
				Statistics.getSingleResourceInstance().addTime(delay);

				requestStatuses.put(String.valueOf(requestId), true);
				System.out.println("SETTING THE REQUEST ID " + requestId
						+ " to true ... ");

				System.out.println("////" + requestId + "////"
						+ "RESOURCES HAVE BEEN ALLOCATED FOR [" + self.getId()
						+ "]" + " from " + event.getSource().getId());
			} else {

				Statistics.getSingleResourceInstance().incReReqCount();
				// Reinitiate the request again
				initiateRequest(requestId);

			}
		}
	};

	// UnUsed Currently
	// Step 6: if a node received a cancel request, it should set the variable
	// isCanceled, that can
	// be used later to decide if it should allocate resources or not
	private boolean isCanceled = false;
	Handler<RequestResources.CancelRequest> handleCancelRequest = new Handler<RequestResources.CancelRequest>() {
		@Override
		public void handle(RequestResources.CancelRequest event) {
			isCanceled = true;
			System.out.println("////" + requestId + "////" + "[" + self.getId()
					+ "] received a cancel request from ["
					+ event.getSource().getId() + "]");
		}
	};

	// Handling cyclon samples and printing out a message
	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			// System.out.println("Received Cyclon samples: " +
			// event.getSample().size());

			if (!TMAN) {

				// receive a new list of neighbors
				neighbours.clear();

				// changed
				ArrayList<PeerDescriptor> partnersDescriptors = event
						.getSample();
				ArrayList<Address> partners = new ArrayList<Address>();
				for (PeerDescriptor desc : partnersDescriptors)
					partners.add(desc.getAddress());
				neighbours.addAll(partners);

			}

		}
	};

	Handler<AllocateResourcesTimeout> handleAllocateResourcesTimeout = new Handler<AllocateResourcesTimeout>() {
		@Override
		public void handle(AllocateResourcesTimeout event) {
			Statistics.getSingleResourceInstance().incReleaseResCount();
			availableResources.release(event.getNumCpus(),
					event.getAmountMemInMb());
			System.out.println("////" + event.getRequestId() + "////" + "["
					+ self.getId() + "]" + " is releasing resources "
					+ event.getNumCpus() + " and " + event.getAmountMemInMb());
		}
	};

	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			// System.out.println("Received TMan samples: " +
			// event.getSample().size());

			if (TMAN) {

				// receive a new list of neighbors
				neighbours.clear();

				// changed
				List<TManPeerDescriptor> partnersDescriptors = event
						.getSample();
				List<Address> partners = new ArrayList<Address>();
				for (TManPeerDescriptor desc : partnersDescriptors)
					partners.add(desc.getAddress());
				neighbours.addAll(partners);
			}
		}
	};

}
