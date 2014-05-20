package tman.system.peer.tman;

import java.util.Comparator;

public class ComparatorByResources implements Comparator<TManPeerDescriptor> {
	TManPeerDescriptor objTManPeerDescriptor;

	public ComparatorByResources(TManPeerDescriptor objDescriptor) {
		this.objTManPeerDescriptor = objDescriptor;
	}

	@Override
	public int compare(TManPeerDescriptor o1, TManPeerDescriptor o2) {
		if(o1.getResources() == null || o2.getResources() == null || objTManPeerDescriptor.getResources() == null) return -1;
		int numFreeCPUs_o1 = o1.getResources().getNumFreeCpus();
		int numFreeCPUs_o2 = o2.getResources().getNumFreeCpus();
		int numFreeCPUs_self = objTManPeerDescriptor.getResources().getNumFreeCpus();
		
		assert (numFreeCPUs_o1== numFreeCPUs_o2);
		if (numFreeCPUs_o1 < numFreeCPUs_self
				&& numFreeCPUs_o2 > numFreeCPUs_self) {
			return 1;
		} else if (numFreeCPUs_o2 < numFreeCPUs_self
				&& numFreeCPUs_o1 > numFreeCPUs_self) {
			return -1;
		} else if (Math.abs(numFreeCPUs_o1
				- numFreeCPUs_self) < Math.abs(numFreeCPUs_o2 - numFreeCPUs_self)) {
			return -1;
		}
		return 1;
	}
	
	
	public int compare_metric(TManPeerDescriptor o1, TManPeerDescriptor o2)
	{
		int value_o1 = 0; int value_o2 = 0; int value_self = 0;
		if(o1.getResources() == null || o2.getResources() == null || objTManPeerDescriptor.getResources() == null) return -1;
		int numFreeCPUs_o1 = o1.getResources().getNumFreeCpus();
		int numFreeCPUs_o2 = o2.getResources().getNumFreeCpus();
		int numAmountMemory_o1 = o1.getResources().getFreeMemInMbs();
		int numAmountMemory_o2 = o2.getResources().getFreeMemInMbs();
		int numFreeCPUs_self = objTManPeerDescriptor.getResources().getNumFreeCpus();
		int numAmountMemory_self = objTManPeerDescriptor.getResources().getFreeMemInMbs();
		
		value_o1 += numFreeCPUs_o1 - 4;
		value_o1 += (numAmountMemory_o1 - 6000) / 1000;
		
		value_o2 += numFreeCPUs_o2 - 4;
		value_o2 += (numAmountMemory_o2 - 6000) / 1000;
		
		value_self += numFreeCPUs_self - 4;
		value_self += (numAmountMemory_self - 6000) / 1000;
		
		assert (value_o1 == value_o2);
		if(value_o1 < value_self &&
				value_o2 > value_self) return 1;
		if(value_o2 < value_self && value_o1 > value_self)
			return -1;
		else if(Math.abs(value_o1 - value_self) < Math.abs(value_o2 - value_self))
				return -1;
		
			return 1;
	}
	
}
