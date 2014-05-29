package tman.system.peer.tman;

import java.util.Comparator;

// Added File
// Needed to compare resources according to the gradient type
// either by CPUs , or Memory , or a metric (explained in the report)
public class ComparatorByResources implements Comparator<TManPeerDescriptor> {
	TManPeerDescriptor objTManPeerDescriptor;
	int gradientType;

	public ComparatorByResources(TManPeerDescriptor objDescriptor, int type) {
		this.objTManPeerDescriptor = objDescriptor;
		this.gradientType = type;
	}

	// Choose the comparison based on gradient type
	public int compare(TManPeerDescriptor o1, TManPeerDescriptor o2) {
		int compareResult;
		switch (gradientType) {
		case 1:
			compareResult = compare_cpus(o1, o2);
			break;
		case 2:
			compareResult = compare_memory(o1, o2);
			break;
		case 3:
			compareResult = compare_combinations(o1, o2);
			break;
		default:
			compareResult = compare_cpus(o1, o2);
			break;
		}
		return compareResult;

	}

	// Compare based on CPUs
	private int compare_cpus(TManPeerDescriptor o1, TManPeerDescriptor o2) {
		int numFreeCPUs_o1 = o1.getNumFreeCpus();
		int numFreeCPUs_o2 = o2.getNumFreeCpus();
		int numFreeCPUs_self = objTManPeerDescriptor.getNumFreeCpus();
         
		// defensive action
		if(numFreeCPUs_o1 < 0 || numFreeCPUs_o2 < 0 || numFreeCPUs_self <0)
			return -1;
		assert (numFreeCPUs_o1 == numFreeCPUs_o2);
		if (numFreeCPUs_o1 < numFreeCPUs_self
				&& numFreeCPUs_o2 > numFreeCPUs_self) {
			return 1;
		} else if (numFreeCPUs_o2 < numFreeCPUs_self
				&& numFreeCPUs_o1 > numFreeCPUs_self) {
			return -1;
		} else if (Math.abs(numFreeCPUs_o1 - numFreeCPUs_self) < Math
				.abs(numFreeCPUs_o2 - numFreeCPUs_self)) {
			return -1;
		}
		return -1;
	}

	// Compare based on Memory
	private int compare_memory(TManPeerDescriptor o1, TManPeerDescriptor o2) {
		int numMemoryAmount_o1 = o1.getFreeMemInMbs();
		int numMemoryAmount_o2 = o2.getFreeMemInMbs();
		int numMemoryAmount_self = objTManPeerDescriptor.getFreeMemInMbs();
		
		// defensive action
		if(numMemoryAmount_o1 < 0 || numMemoryAmount_o2 < 0 || numMemoryAmount_self <0)
			return -1;
				
		assert (numMemoryAmount_o1 == numMemoryAmount_o2);
		if (numMemoryAmount_o1 < numMemoryAmount_self
				&& numMemoryAmount_o2 > numMemoryAmount_self) {
			return 1;
		} else if (numMemoryAmount_o2 < numMemoryAmount_self
				&& numMemoryAmount_o1 > numMemoryAmount_self) {
			return -1;
		} else if (Math.abs(numMemoryAmount_o1 - numMemoryAmount_self) < Math
				.abs(numMemoryAmount_o2 - numMemoryAmount_self)) {
			return -1;
		}
		return -1;
	}

	// Compare based on a metric explained in report
	private int compare_combinations(TManPeerDescriptor o1,
			TManPeerDescriptor o2) {
		int value_o1 = 0;
		int value_o2 = 0;
		int value_self = 0;
		
		int numFreeCPUs_o1 = o1.getNumFreeCpus();
		int numFreeCPUs_o2 = o2.getNumFreeCpus();
		int numAmountMemory_o1 = o1.getFreeMemInMbs();
		int numAmountMemory_o2 = o2.getFreeMemInMbs();
		int numFreeCPUs_self = objTManPeerDescriptor.getNumFreeCpus();
		int numAmountMemory_self = objTManPeerDescriptor.getFreeMemInMbs();
		
		// defensive action
		if(numFreeCPUs_o1 < 0 || numFreeCPUs_o2 < 0 || numAmountMemory_o1 <0 ||
		   numAmountMemory_o2 < 0 || 	numFreeCPUs_self < 0 || numAmountMemory_self < 0)
					return -1;

		value_o1 += numFreeCPUs_o1 - 4;
		value_o1 += (numAmountMemory_o1 - 6000) / 1000;

		value_o2 += numFreeCPUs_o2 - 4;
		value_o2 += (numAmountMemory_o2 - 6000) / 1000;

		value_self += numFreeCPUs_self - 4;
		value_self += (numAmountMemory_self - 6000) / 1000;

		assert (value_o1 == value_o2);
		if (value_o1 < value_self && value_o2 > value_self)
			return 1;
		if (value_o2 < value_self && value_o1 > value_self)
			return -1;
		else if (Math.abs(value_o1 - value_self) < Math.abs(value_o2
				- value_self))
			return -1;

		return -1;
	}

}
