/**
 * 
 */
package simulator.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author jasperh
 *
 */
public class Statistics {

	private static Statistics sinlgeResourceInstance;
	
	private static Statistics batchResourceInstance;
	
	private List<Long> allocationTimes;
	
	
	public Statistics() {
		this.allocationTimes = new ArrayList<Long>();
	}
	
	private void sort(){
		Collections.sort(this.allocationTimes);
	}
	
	
	public static Statistics getSingleResourceInstance() {
		if(sinlgeResourceInstance == null)
			sinlgeResourceInstance = new Statistics();
		return sinlgeResourceInstance;
	}
	
	public static Statistics getBatchResourceInstance() {
		if(batchResourceInstance == null)
			batchResourceInstance = new Statistics();
		return batchResourceInstance;
	}
	
	public void addTime(Long time) {
		this.allocationTimes.add(time);
//		sort();
	}
	
	public Long getAvg(){
		Long sum = new Long(0);
		  if(!allocationTimes.isEmpty()) {
		    for (Long time : allocationTimes) {
		        sum += time;
		    }
		    return sum / allocationTimes.size();
		  }
		  return sum;
	}
	
	public Long getNinetyNinth() {
		Collections.sort(this.allocationTimes);
		Long sum = new Long(0);
		  if(!allocationTimes.isEmpty()) {
			int amount = allocationTimes.size()/100;
		    for (int i=0; i < amount; i++) {
		    	Long time = allocationTimes.get(i);
		        sum += time;
		    }
			return sum / amount;
		  }
		  return sum;
	}
	
	
}
