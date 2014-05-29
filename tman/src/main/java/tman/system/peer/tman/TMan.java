package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import java.util.ArrayList;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import java.util.Collections;
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
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(TMan.class);

	Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
	Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	Positive<Network> networkPort = positive(Network.class);
	Positive<Timer> timerPort = positive(Timer.class);

	private long period;
	private Address self;
	// changed private ArrayList<Address> tmanPartners;
	private List<TManPeerDescriptor> tmanPartners;
	private TManConfiguration tmanConfiguration;
	private Random r;
	private AvailableResources availableResources;
	// Shatha 
	private long tmanTimeout;
	private ArrayList<TManViewEntry> entries;
	int size = 10;
	int gradient_type = 2;
	// gradient type: 1 .. comparison based on free CPUs
	// gradient type: 2 .. comparison based on memory 
	// gradient type: 3 .. comparison based on combination of CPUs and memory

	public class TManSchedule extends Timeout {

		public TManSchedule(SchedulePeriodicTimeout request) {
			super(request);
		}

		public TManSchedule(ScheduleTimeout request) {
			super(request);
		}
	}

	public TMan() {
		tmanPartners = new ArrayList<TManPeerDescriptor>();

		subscribe(handleInit, control);
		subscribe(handleRound, timerPort);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleTManPartnersResponse, networkPort);
		subscribe(handleTManPartnersRequest, networkPort);
	}

	Handler<TManInit> handleInit = new Handler<TManInit>() {
		@Override
		public void handle(TManInit init) {
			self = init.getSelf();
			tmanConfiguration = init.getConfiguration();
			period = tmanConfiguration.getPeriod();
			r = new Random(tmanConfiguration.getSeed());
			availableResources = init.getAvailableResources();
			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new TManSchedule(rst));
			trigger(rst, timerPort);
			// Shatha Review 
			tmanTimeout = tmanConfiguration.getPeriod();

		}
	};

	Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
		@Override
		public void handle(TManSchedule event) {
			ArrayList<Address> partnersAddresses = new ArrayList<Address>();
			for (TManPeerDescriptor desc : tmanPartners)
				partnersAddresses.add(desc.getAddress());
			Snapshot.updateTManPartners(self, partnersAddresses);

			// Publish sample to connected components
			//System.out.println(".. TMAN ..");
			trigger(new TManSample(tmanPartners), tmanPort);
		}
	};


	// Shatha
	// Question the nodes from cyclon sample don't have the profile information
	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			// 1. Retrieve the list of samples from Cyclon
			ArrayList<cyclon.system.peer.cyclon.PeerDescriptor> cyclonPartners = event.getSample();
			//System.out.println("RETRIEVING THE LIST OF CYCLON SAMPLES WITH SIZE .. " + event.getSample().size());	
			
			// Merge cyclon partners in TManPartners
			//  merge is a set operation that keeps at most one descriptor
			//  for each node
			//tmanPartners.addAll(cyclonPartners);
			
			// 3. merge the buffer with a random sample of the nodes from the entire network (from cyclon)
			List<TManPeerDescriptor> randomDescriptors = tmanPartners;
			for(cyclon.system.peer.cyclon.PeerDescriptor a: cyclonPartners)
			{
				// check for uniquness
				if(!randomDescriptors.contains(a))
				randomDescriptors.add(new TManPeerDescriptor(a.getAddress(),a.getNumFreeCpus(), a.getFreeMemInMbs()));
			}
			// add the self descriptor to the view
			TManPeerDescriptor selfDescriptor = new TManPeerDescriptor(self, availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs());
			if(!randomDescriptors.contains(selfDescriptor))
				randomDescriptors.add(selfDescriptor);
			//System.out.println("MERGING DESCRIPTORS FROM CYCLON AND SELF DESCRIPTOR .. ");
			
			// 2. Pick a peer to send to it based on the ranking method
		    // which is the soft max preference function 
			TManPeerDescriptor selectedPeerDescriptor = getSoftMaxEntry(randomDescriptors); 
			//System.out.println("SELECTING PEER ACCORDING TO RANK METHOD , NUMBER OF CPUs.. " + selectedPeerDescriptor.getNumFreeCpus()
				//	+ " AMOUNT OF MEMORY IS .. " + selectedPeerDescriptor.getFreeMemInMbs());
					
			
			// 4. Schedule the timeout event 
			ScheduleTimeout rst = new ScheduleTimeout(tmanTimeout);
			rst.setTimeoutEvent(new ExchangeMsg.RequestTimeout(rst, selectedPeerDescriptor.getAddress()));
			UUID rTimeoutId = rst.getTimeoutEvent().getTimeoutId();
			
			
            // 5. Send the request to the selected peer
			TManDescriptorBuffer buffer = new TManDescriptorBuffer(self, randomDescriptors);
			ExchangeMsg.Request objRequest = new ExchangeMsg.Request(rTimeoutId, buffer, self,selectedPeerDescriptor.getAddress() );
			trigger(objRequest,networkPort);
			//System.out.println("SENDING BUFFER TO SELECTED PEER .. " + selectedPeerDescriptor.getAddress());

		}
	};

	Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
		@Override
		public void handle(ExchangeMsg.Request event) {
             // 1. receive buffer from the sender
			Address peer = event.getRandomBuffer().getFrom();
			TManDescriptorBuffer receivedRandomBuffer = event.getRandomBuffer();
			//System.out.println("HANDLE REQUEST - RECEIVING BUFFER FROM " + peer.getId());
			
			// 2. merge the received buffer with the current buffer
		    receivedRandomBuffer.addDescriptors(tmanPartners);
		   // System.out.println("HANDLE REQUEST - MERGING BUFFER WITH RECEIVED BUFFER .. ");
		    
		    
		    // 3. view = selectView(buff)
		    // Sort all nodes in buffer, and pick out c highest ranked nodes
		    //List<AvailableResources> bufferEntriesResources = new ArrayList<AvailableResources>();
		   // for(TManPeerDescriptor p : receivedRandomBuffer.getDescriptors())
		    //{
		    //	bufferEntriesResources.add(p.getResources());
		   // }
		    
		    List<TManPeerDescriptor> sortedDescriptor = receivedRandomBuffer.getDescriptors();
		    if(availableResources.getNumFreeCpus() > 0 && availableResources.getFreeMemInMbs() > 0)
		    Collections.sort( sortedDescriptor, new ComparatorByResources(new TManPeerDescriptor(self,availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs()), gradient_type));
		    //System.out.println("HANDLE REQUEST - SORTING NODES IN BUFFER AND CHOOSING HIGHEST NODES TO REMAIN ..");
		    
		    ArrayList<TManPeerDescriptor> remainingDescriptors = new ArrayList<TManPeerDescriptor>();
		    
		    for(int i=0 ; i < size && i < sortedDescriptor.size() ; i++) {

		    		remainingDescriptors.add(sortedDescriptor.get(i));
		    		
		    }
		    tmanPartners = remainingDescriptors;
		    TManDescriptorBuffer bufferToSend = new TManDescriptorBuffer(self, remainingDescriptors);
		   // System.out.println("HANDLE REQUEST - KEEPING C HIGHEST RANKED NODE ");
		    
		    // 4. Send response to the original node
		    ExchangeMsg.Response objResponse = new ExchangeMsg.Response(event.getRequestId(), bufferToSend, self, event.getSource());
		    trigger(objResponse, networkPort);
		    //System.out.println("HANDLE REQUEST - SENDING RESPONSE WITH BUFFER ...");
		}
	};

	Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
		@Override
		public void handle(ExchangeMsg.Response event) {
			  // 1. receive buffer from the sender
			Address peer = event.getSelectedBuffer().getFrom();
			TManDescriptorBuffer receivedRandomBuffer = event.getSelectedBuffer();
			//System.out.println("HANDLE RESPONSE - RECEIVED BUFFER FROM .. " + peer.getId());
			
			// 2. merge the received buffer with the current buffer
		    receivedRandomBuffer.addDescriptors(tmanPartners);
		    //System.out.println("HANDLE RESPONSE - MERGING BUFFER ..");
		    
		    // 3. view = selectView(buff)
		    // Sort all nodes in buffer, and pick out c highest ranked nodes
		   // List<AvailableResources> bufferEntriesResources = new ArrayList<AvailableResources>();
		   // for(TManPeerDescriptor p : receivedRandomBuffer.getDescriptors())
		   // {
		    //	bufferEntriesResources.add(p.getResources());
		   // }
		    if(availableResources.getNumFreeCpus() > 0 && availableResources.getFreeMemInMbs() > 0)
		    Collections.sort(receivedRandomBuffer.getDescriptors(), new ComparatorByResources(new TManPeerDescriptor(self,availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs()), gradient_type));
		    //System.out.println("HANDLE RESPONSE - SORTING ..");
		    
		    // buffer should be updated again
		    List<TManPeerDescriptor> remainingDescriptors = new ArrayList<TManPeerDescriptor>();
		    List<TManPeerDescriptor> sortedDescriptor = receivedRandomBuffer.getDescriptors();
		    
		    
		    for(int i=0 ; i < size  && i < sortedDescriptor.size(); i++) {

		    		remainingDescriptors.add(sortedDescriptor.get(i));
		    		
		    }
		    tmanPartners = remainingDescriptors;
		    
		    //System.out.println("HANDLE RESPONSE - KEEPING C HIGHEST RANKED NODES ..");
		    
		}
	};

	// Shatha - Review if we should use it or use the other comparators of resources
	
	// TODO - if you call this method with a list of entries, it will
	// return a single node, weighted towards the 'best' node (as defined by
	// ComparatorById) with the temperature controlling the weighting.
	// A temperature of '1.0' will be greedy and always return the best node.
	// A temperature of '0.000001' will return a random node.
	// A temperature of '0.0' will throw a divide by zero exception :)
	// Reference:
	// http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html
	public Address getSoftMaxAddress(List<Address> entries) {
		Collections.sort(entries, new ComparatorById(self));

		double rnd = r.nextDouble();
		double total = 0.0d;
		double[] values = new double[entries.size()];
		int j = entries.size() + 1;
		for (int i = 0; i < entries.size(); i++) {
			// get inverse of values - lowest have highest value.
			double val = j;
			j--;
			values[i] = Math.exp(val / tmanConfiguration.getTemperature());
			total += values[i];
		}

		for (int i = 0; i < values.length; i++) {
			if (i != 0) {
				values[i] += values[i - 1];
			}
			// normalise the probability for this entry
			double normalisedUtility = values[i] / total;
			if (normalisedUtility >= rnd) {
				return entries.get(i);
			}
		}
		return entries.get(entries.size() - 1);
	}
	
	
	public TManPeerDescriptor getSoftMaxEntry(List<TManPeerDescriptor> entries) {
		if(availableResources.getNumFreeCpus() > 0 && availableResources.getFreeMemInMbs() > 0)
		Collections.sort(entries, new ComparatorByResources(new TManPeerDescriptor(self, availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs()), gradient_type));

		double rnd = r.nextDouble();
		double total = 0.0d;
		double[] values = new double[entries.size()];
		int j = entries.size() + 1;
		for (int i = 0; i < entries.size(); i++) {
			// get inverse of values - lowest have highest value.
			double val = j;
			j--;
			values[i] = Math.exp(val / tmanConfiguration.getTemperature());
			total += values[i];
		}

		for (int i = 0; i < values.length; i++) {
			if (i != 0) {
				values[i] += values[i - 1];
			}
			// normalise the probability for this entry
			double normalisedUtility = values[i] / total;
			if (normalisedUtility >= rnd) {
				return entries.get(i);
			}
		}
		return entries.get(entries.size() - 1);
	}

	
		

}
