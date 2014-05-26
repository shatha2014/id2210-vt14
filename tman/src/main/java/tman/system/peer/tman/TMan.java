package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import common.peer.PeerDescriptor;

import java.util.ArrayList;

import cyclon.system.peer.cyclon.Cache;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.DescriptorBuffer;
import cyclon.system.peer.cyclon.ShuffleTimeout;
import cyclon.system.peer.cyclon.ViewEntry;

import java.util.Arrays;
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
	private ArrayList<TManPeerDescriptor> tmanPartners;
	private TManConfiguration tmanConfiguration;
	private Random r;
	private AvailableResources availableResources;
	// Shatha 
	private TManDescriptorBuffer buffer;
	private long tmanTimeout;
	private ArrayList<TManViewEntry> entries;

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
				
			
			// 2. Merge cyclon partners in TManPartners
			//  merge is a set operation that keeps at most one descriptor
			//  for each node
			//tmanPartners.addAll(cyclonPartners);
			
			// Shatha review if TManDescriptor and the other added files are really needed or not
			// 3. merge the buffer with a random sample of the nodes from the entire network (from cyclon)
			ArrayList<TManPeerDescriptor> randomDescriptors = tmanPartners;
			for(cyclon.system.peer.cyclon.PeerDescriptor a: cyclonPartners)
			{
				randomDescriptors.add(new TManPeerDescriptor(a.getAddress(),a.getNumFreeCpus(), a.getFreeMemInMbs()));
			}
			// add the self descriptor to the view
			randomDescriptors.add(new TManPeerDescriptor(self, availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs()));
			
			// 2. Pick a peer to send to it based on the ranking method
			// which is the soft max preference function 
		     TManPeerDescriptor selectedPeerDescriptor = getSoftMaxEntry(randomDescriptors);
					
			
			// 4. Schedule the timeout event 
			ScheduleTimeout rst = new ScheduleTimeout(tmanTimeout);
			rst.setTimeoutEvent(new ExchangeMsg.RequestTimeout(rst, selectedPeerDescriptor.getAddress()));
			UUID rTimeoutId = rst.getTimeoutEvent().getTimeoutId();
			
            // 5. Send the request to the selected peer
			buffer = new TManDescriptorBuffer(self, randomDescriptors);
			ExchangeMsg.Request objRequest = new ExchangeMsg.Request(rTimeoutId, buffer, self,selectedPeerDescriptor.getAddress() );
			trigger(objRequest,networkPort);

		}
	};

	Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
		@Override
		public void handle(ExchangeMsg.Request event) {
             // 1. receive buffer from the sender
			Address peer = event.getRandomBuffer().getFrom();
			TManDescriptorBuffer receivedRandomBuffer = event.getRandomBuffer();
			// 2. merge the received buffer with the current buffer
		    receivedRandomBuffer.addDescriptors(buffer.getDescriptors());
		    // 3. view = selectView(buff)
		    // Sort all nodes in buffer, and pick out c highest ranked nodes
		    //List<AvailableResources> bufferEntriesResources = new ArrayList<AvailableResources>();
		   // for(TManPeerDescriptor p : receivedRandomBuffer.getDescriptors())
		    //{
		    //	bufferEntriesResources.add(p.getResources());
		   // }
		    Collections.sort( receivedRandomBuffer.getDescriptors(), new ComparatorByResources(new TManPeerDescriptor(self,availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs())));
		    // Shatha - Question
		    // based on what we should choose highest ranked nodes ?
		    // buffer should be updated again
		    
		    // 4. Send response to the original node
		    ExchangeMsg.Response objResponse = new ExchangeMsg.Response(event.getRequestId(), buffer, self, event.getSource());
		    trigger(objResponse, networkPort);
		}
	};

	Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
		@Override
		public void handle(ExchangeMsg.Response event) {
			  // 1. receive buffer from the sender
			Address peer = event.getSelectedBuffer().getFrom();
			TManDescriptorBuffer receivedRandomBuffer = event.getSelectedBuffer();
			// 2. merge the received buffer with the current buffer
		    receivedRandomBuffer.addDescriptors(buffer.getDescriptors());
		    // 3. view = selectView(buff)
		    // Sort all nodes in buffer, and pick out c highest ranked nodes
		   // List<AvailableResources> bufferEntriesResources = new ArrayList<AvailableResources>();
		   // for(TManPeerDescriptor p : receivedRandomBuffer.getDescriptors())
		   // {
		    //	bufferEntriesResources.add(p.getResources());
		   // }
		    Collections.sort(receivedRandomBuffer.getDescriptors(), new ComparatorByResources(new TManPeerDescriptor(self,availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs())));
		    // Shatha - Question
		    // based on what we should choose highest ranked nodes ?
		    // buffer should be updated again
		    
		    // 4. Send response to the original node
		    ExchangeMsg.Response objResponse = new ExchangeMsg.Response(event.getRequestId(), buffer, self, event.getSource());
		    trigger(objResponse, networkPort);

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
		Collections.sort(entries, new ComparatorByResources(new TManPeerDescriptor(self, availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs())));

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
