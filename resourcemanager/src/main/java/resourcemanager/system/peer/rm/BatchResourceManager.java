package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.BatchRequestResources;
import common.simulation.Job;
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
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import simulator.snapshot.PeerInfo;
import system.peer.RmPort;
import tman.system.peer.tman.TManPeerDescriptor;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Needed for Batch Requests Scenarios
 * @author jdowling
 */
public final class BatchResourceManager extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceManager.class);

	// a boolean value to decide the mode
	protected static final boolean TMAN = true;

	Positive<RmPort> indexPort = positive(RmPort.class);
	Positive<Network> networkPort = positive(Network.class);
	Positive<Timer> timerPort = positive(Timer.class);
	Negative<Web> webPort = negative(Web.class);
	Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);

	// Used for simulation statistics
	long requestTimestamp;
	// Keep the request id for two purposes
	// tracking statments for each handler and
	// to check if the request is finalized or not
	long requestId;
	// to keep time of holding a resource
	private int timeToHoldResource;
	// TODO should be configuration
	// currently change it manually here please
	private final int PROBESIZE = 4;
	// it should be equal to probe size, but
	// decreased when timeout
	private int requestProbeSize;
	private ArrayList<Job> jobs = new ArrayList<Job>();
	// Node neighbours which will be filed either
	// from Cyclon or from TMAN
	ArrayList<Address> neighbours = new ArrayList<Address>();
	// self address to be used when needed
	private Address self;
	private RmConfiguration configuration;
	Random random;
	// available resources of the node
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
	// Hash map that keeps the request with its jobs statuses
	private HashMap<String, ArrayList<Job>> requestsStatuses;

	// Handlers subscription
	public BatchResourceManager() {

		subscribe(handleInit, control);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleRequestResource, indexPort);
		subscribe(handleResourceAllocationRequest, networkPort);
		subscribe(handleResourceAllocationResponse, networkPort);
		subscribe(handleTManSample, tmanPort);
		subscribe(handleActualAllocationRequest, networkPort);
		subscribe(handleAvailableResourcesResponse, networkPort);
		subscribe(handleAllocateResourcesTimeout, timerPort);
		subscribe(handleRespondPeerTimeout, timerPort);
	}

	// initializations
	Handler<RmInit> handleInit = new Handler<RmInit>() {
		@Override
		public void handle(RmInit init) {
			self = init.getSelf();
			configuration = init.getConfiguration();
			random = new Random(init.getConfiguration().getSeed());
			availableResources = init.getAvailableResources();
			timeToHoldResource = 0;
			requestsStatuses = new HashMap<String, ArrayList<Job>>();
		}
	};

	// On receiving a request, the node will send a probe randomly to a
	// group of its neighbours asking them about the availability of the
	// requested resources in the scenario
	// [TMAN] in case it is TMAN mode then skip the probing
	// directly send to one of the neighbours that are already
	// sorted and ready
	private void initiateRequest(long requId, ArrayList<Job> jobs) {

		// if it is TMAN then no need for probing
		if (TMAN) {
			// send a resources allocation request for the node with the
			// highest number of free resources
			long seed = (long) (System.currentTimeMillis() * Math.random());
			Random objRandom = new Random(seed);

			// Send the request to the jobs related to the request
			for (int i = 0; i < jobs.size(); i++) {
				if (!jobs.get(i).getStatus()) // only if the job is not handled
											  // yet
				{
					System.out.println(" JOB ID " + jobs.get(i).getJobId());
					System.out.println(" Sending Request to a neighbour ");
					RequestResources.ActualAllocationRequest objActualAllocationRequest = new RequestResources.ActualAllocationRequest(
							self, neighbours.get(objRandom.nextInt(neighbours
									.size())), (int) jobs.get(i).getNumCpus(),
							(int) jobs.get(i).getMemoryInMbs(), requestId, jobs
									.get(i).getJobId());
					trigger(objActualAllocationRequest, networkPort);
				}
			}

		} else {
			// 1. Select PROBESIZE random peers from the current neighbors
			ArrayList<Address> selectedNeighbors;
			long seed;
			Random objRandom;

			for (int i = 0; i < neighbours.size(); i++) {
				System.out.println("------REQUEST ID----- " + requId
						+ " NEIGHBORS: " + neighbours.get(i).getId());
			}
            
			// Sending requests to jobs related to the request 
			for (int i = 0; i < jobs.size(); i++) {
				if (!jobs.get(i).getStatus()) {
					selectedNeighbors = new ArrayList<Address>();
					seed = (long) (System.currentTimeMillis() * Math.random());
					objRandom = new Random(seed);

					if (neighbours != null && neighbours.size() > 0) {
						// add random neighbours to be probed for their
						// resources
						int counter = 0;
						while (counter < PROBESIZE) {
							selectedNeighbors.add(neighbours.get(objRandom
									.nextInt(neighbours.size())));
							counter++;
						}

						System.out.println("TRACKING SELECTED NEIGHBOURS SIZE "
								+ selectedNeighbors.size());

						// Need to clear the responding peers array to avoid any
						// issues if there is a need to reinitiate the request again
						// in case it was not allocated resources
						respondingPeers.clear();
					}

					// 2. Send the Request to those selected neighbors
					if (selectedNeighbors != null) {
						for (Address objAddress : selectedNeighbors) {

							System.out
									.println("SENDING REQUEST IN FIRST APPROACH ");
							RequestResources.Request objRequest = new RequestResources.Request(
									self, objAddress, (int) jobs.get(i)
											.getNumCpus(), (int) jobs.get(i)
											.getMemoryInMbs(), requId, jobs
											.get(i).getJobId());
							trigger(objRequest, networkPort);

							// print out results for tracking reasons
							System.out
									.println("////"
											+ requId
											+ "////"
											+ "--- "
											+ selectedNeighbors.size()
											+ "["
											+ self.getId()
											+ "]"
											+ " sending a request to the following neighbor ["
											+ objAddress.getId() + "]");
						}
					}

					// to keep the status of the request, still it is not done
					// it will be done once the resources are allocated
					requestsStatuses.put(String.valueOf(requId), jobs);
				}
			}
		}

	}

	
	// Step 1: On receiving a request
	Handler<BatchRequestResources> handleRequestResource = new Handler<BatchRequestResources>() {

		@Override
		public void handle(BatchRequestResources event) {
			// setting variables
			jobs = (ArrayList<Job>)event.getJobs();
			requestId = event.getRequestId();

			System.out.println("..... HANDLE BATCH REQUESTS RESOUCES ..... ");
			System.out.println("..... JOB INFORMATION ..... ");
			for (Job j : jobs) {
				System.out.println("ID = " + j.getJobId() + " Request ID = "
						+ j.getRequestId() + " Number of CPUs = "
						+ j.getNumCpus() + " Amount of Memory = "
						+ j.getMemoryInMbs() + " Time to hold resourcse = "
						+ j.getTimeToHoldResource() + " Status = "
						+ j.getStatus());
			}
			System.out.println(" ............ ");

			// log new requests to statistic
			requestTimestamp = System.currentTimeMillis();
			Statistics.getSingleResourceInstance().incSpawnCount();
            
			// initiate the request with the jobs it has
			initiateRequest(requestId, jobs);

			// request probe size is needed to control the
			// case when we have less neighbours than the probe size
			requestProbeSize = PROBESIZE;
			if (PROBESIZE > neighbours.size())
				requestProbeSize = neighbours.size();
			// printing tracking statments
			System.out.println("TRACKING " + requestId + " // "
					+ " REQUEST PROBE SIZE IS SSSSS " + requestProbeSize);

			// Schedule a timeout, so that after a certain timeout
			// the request can be reinitialized again in case it was not
			// allocated resources
			ScheduleTimeout rst = new ScheduleTimeout(5000);
			rst.setTimeoutEvent(new RespondPeerTimeout(rst, requestId, jobs));
			trigger(rst, timerPort);

		}
	};

	// Step 2: On receiving a request, the node will send a response with its
	// current resources so that the original node can decide the nodes with
	// highest free
	// resources
	Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
		@Override
		public void handle(RequestResources.Request event) {

			// statistics gathering
			Statistics.getSingleResourceInstance().incRcvdRqstCount();

			// Printing out on the screen for tracking purpose
			System.out.println("////" + event.getRequestId() + "////"
					+ "HANDLE RESORUCE ALLOCATION REQUEST: " + "["
					+ event.getDestination().getId() + "]"
					+ " received a request from : ["
					+ event.getSource().getId() + "]"
					+ " and i have the following resources "
					+ availableResources.getNumFreeCpus() + " and "
					+ availableResources.getFreeMemInMbs() + " JOB ID is "
					+ event.getJobId());

			// when the node received a request to allocate resources, it should
			// return the number of free CPUs and memory it has
			RequestResources.AvailableResourcesResponse objAvailableResourcesResponse = new RequestResources.AvailableResourcesResponse(
					self, event.getSource(), event.getNumCpus(),
					event.getAmountMemInMb(), event.getRequestId(),
					event.getJobId());
			trigger(objAvailableResourcesResponse, networkPort);

		}
	};

	// On timeout if not TMAN mode, check if the request was not allocated
	// resources
	// then reinitialize the request again
	Handler<RespondPeerTimeout> handleRespondPeerTimeout = new Handler<RespondPeerTimeout>() {
		@Override
		public void handle(RespondPeerTimeout event) {

			// if no respond was received then reinitiate the request
			if (!TMAN) {
				if (requestsStatuses != null) {
					System.out.println(" TIMEOUT " + event.getRequestId()
							+ " RESPONDING PEER SIZE IS "
							+ respondingPeers.size());
					// clear needed to avoid issues when reinitiating the
					// request again
					respondingPeers.clear();
					initiateRequest(event.getRequestId(), event.getJobs());
				}

			}
		}
	};

	// Step 3: The original node will start collecting responses from the nodes
	// it probed
	// to decide the node with the highest number of resources
	private ArrayList<PeerInfo> respondingPeers = new ArrayList<PeerInfo>();
	Handler<RequestResources.AvailableResourcesResponse> handleAvailableResourcesResponse = new Handler<RequestResources.AvailableResourcesResponse>() {
		@Override
		public void handle(RequestResources.AvailableResourcesResponse event) {
			// statistics gathering
			Statistics.getSingleResourceInstance().incAvlblResCount();

			System.out.println("////" + requestId + "////" + " job id "
					+ event.getJobId()
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

			System.out.println("// " + requestId + " // "
					+ " REQUEST PROBE SIZE is " + requestProbeSize
					+ " AND NEIGHBORS are " + neighbours.size());

			// Needed to avoid the situation when you don't have
			// enough neighbours
			if (PROBESIZE > neighbours.size())
				requestProbeSize = neighbours.size();

			System.out
					.println("RESPONDING PEER SIZE " + respondingPeers.size());
			// if collected the needed amount of peers then sort them and choose
			// the best
			// Need to check if the request was not handled for
			// "Reinitiate Requests" cases
			if (respondingPeers.size() == requestProbeSize) {
			
				// tracking statment
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
						event.getNumCpus(), event.getAmountMemInMb(),
						requestId, event.getJobId());
				trigger(objActualAllocationRequest, networkPort);

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
			System.out
					.println("////"
							+ event.getRequestId()
							+ "////"
							+ " JOB ID "
							+ event.getJobId()
							+ "HANDLE ACTUAL ALLOCATION REQUEST - event source ["
							+ event.getSource().getId()
							+ "] and destination ["
							+ event.getDestination().getId()
							+ "] and we are checking if "
							+ "we have enough resources for the allocation so that we allocate them ");

			// when receiving this request, the node should check if it has
			// the requested resources, then allocate them and send a response
			// back
			boolean success = availableResources.allocate(event.getNumCpus(),
					event.getAmountMemInMb());

			if (success) {

				// statistics gathering
				Statistics.getSingleResourceInstance().incAllocReqCount();

				// trigger a response with success
				RequestResources.Response objResponse = new RequestResources.Response(
						self, event.getSource(), success, event.getNumCpus(),
						event.getAmountMemInMb(), event.getJobId());
				trigger(objResponse, networkPort);

				// Set the amounts of cpus and amount of memory so that they
				// can be released later
				numAllocatedCpus = event.getNumCpus();
				amountAllocatedMem = event.getAmountMemInMb();

				// Schedule a timeout to release the resources once timeout
				ScheduleTimeout rst = new ScheduleTimeout(timeToHoldResource);
				rst.setTimeoutEvent(new AllocateResourcesTimeout(rst, event
						.getSource(), event.getNumCpus(), event
						.getAmountMemInMb(), event.getRequestId(), event
						.getJobId()));
				// tracking
				System.out.println("////" + event.getRequestId() + "////"
						+ "sending Job to node " + event.getSource().getId());
				trigger(rst, timerPort);
				// tracking
				System.out.println("////" + event.getRequestId() + "////"
						+ ".... Sending timeout event to ... "
						+ event.getSource().getId());

			} else {
				// Send a response with failure in case the
				// resources were not allocated so that the node can send
				// the request again
				RequestResources.Response objResponse = new RequestResources.Response(
						self, event.getSource(), success, event.getNumCpus(),
						event.getAmountMemInMb(), event.getJobId());
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
							+ " JOB ID  "
							+ event.getJobId()
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

				// set the status of the request to be done and finalized
				for (Job j : jobs) {
					if (j.getJobId() == event.getJobId()) {
						j.setStatus(true);
					}
				}
				requestsStatuses.put(String.valueOf(requestId), jobs);

				// tracking statments
				System.out.println("SETTING THE REQUEST ID " + requestId
						+ " to true ... ");

				System.out.println("////" + requestId + "////"
						+ "RESOURCES HAVE BEEN ALLOCATED FOR [" + self.getId()
						+ "]" + " from " + event.getSource().getId());
			} else {

				// statistics gathering
				Statistics.getSingleResourceInstance().incReReqCount();
				// Reinitiate the request again in case the event was not
				// successful
				reinitiateJob(requestId, event.getJobId(), event.getNumCpus(),
						event.getAmountMemInMb());

			}
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

				for (int i = 0; i < neighbours.size(); i++) {
					System.out.println(" NEIGHBORS: "
							+ neighbours.get(i).getId());
				}

			}

		}
	};

	// After a certain timeout (timetoholdresources) release the resources again
	Handler<AllocateResourcesTimeout> handleAllocateResourcesTimeout = new Handler<AllocateResourcesTimeout>() {
		@Override
		public void handle(AllocateResourcesTimeout event) {
			// statistics gathering
			Statistics.getSingleResourceInstance().incReleaseResCount();
			// release resources
			availableResources.release(event.getNumCpus(),
					event.getAmountMemInMb());
			// tracking purposes
			System.out.println("////" + event.getRequestId() + "////"
					+ " job id " + event.getJobId() + "[" + self.getId() + "]"
					+ " is releasing resources " + event.getNumCpus() + " and "
					+ event.getAmountMemInMb());
		}
	};

	// Handling TMAN samples
	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
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
	
	// Reinitiate a specific job if it failed
	private void reinitiateJob(long requId, long jobId, int numCPUs,
			int amountMemory) {

		// if it is TMAN then no need for probing
		if (TMAN) {
			// send a resources allocation request for the node with the
			// highest number of free resources
			long seed = (long) (System.currentTimeMillis() * Math.random());
			Random objRandom = new Random(seed);

			RequestResources.ActualAllocationRequest objActualAllocationRequest = new RequestResources.ActualAllocationRequest(
					self, neighbours.get(objRandom.nextInt(neighbours.size())),
					numCPUs, amountMemory, requestId, jobId);
			trigger(objActualAllocationRequest, networkPort);

		} else {
			// 1. Select PROBESIZE random peers from the current neighbors
			ArrayList<Address> selectedNeighbors;
			long seed;
			Random objRandom;

			selectedNeighbors = new ArrayList<Address>();
			seed = (long) (System.currentTimeMillis() * Math.random());
			objRandom = new Random(seed);

			if (neighbours != null && neighbours.size() > 0) {
				// add random neighbours to be probed for their resources
				int counter = 0;
				while (counter < PROBESIZE) {
					selectedNeighbors.add(neighbours.get(objRandom
							.nextInt(neighbours.size())));
					counter++;
				}

				// Need to clear the responding peers array to avoid any issues
				// if there is a need to reinitiate the request again
				// in case it was not allocated resources
				respondingPeers.clear();
			}

			// 2. Send the Request to those selected neighbors
			if (selectedNeighbors != null) {
				for (Address objAddress : selectedNeighbors) {

					System.out.println("SENDING REQUEST IN FIRST APPROACH ");
					RequestResources.Request objRequest = new RequestResources.Request(
							self, objAddress, numCPUs, amountMemory, requId,
							jobId);
					trigger(objRequest, networkPort);

					// print out results for tracking reasons
					System.out.println("////" + requId + "////" + "--- "
							+ selectedNeighbors.size() + "[" + self.getId()
							+ "]"
							+ " sending a request to the following neighbor ["
							+ objAddress.getId() + "]");
				}
			}

		}

	}


}
