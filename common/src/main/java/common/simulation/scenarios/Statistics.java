/**
 * 
 */
package common.simulation.scenarios;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * @author jasperh
 * 
 */
public class Statistics {

	private static Statistics sinlgeResourceInstance;

	private static Statistics batchResourceInstance;

	private List<Long> allocationTimes;

	private String resultTitle;

	public Statistics(String title) {
		this.resultTitle = title;
		this.allocationTimes = new ArrayList<Long>();
	}

	private void sort() {
		Collections.sort(this.allocationTimes);
	}

	public static Statistics getSingleResourceInstance() {
		if (sinlgeResourceInstance == null){
			sinlgeResourceInstance = new Statistics("singleResourceSimulation");
		}
		return sinlgeResourceInstance;
	}

	public static Statistics getBatchResourceInstance() {
		if (batchResourceInstance == null)
			batchResourceInstance = new Statistics("batchResourceSimulation");
		return batchResourceInstance;
	}

	public void addTime(Long time) {
		this.allocationTimes.add(time);
		// sort();
	}

	public Long getAvg() {
		Long sum = new Long(0);
		if (!allocationTimes.isEmpty()) {
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
		if (!allocationTimes.isEmpty()) {
			int amount = allocationTimes.size() / 100;
			for (int i = 0; i < amount; i++) {
				Long time = allocationTimes.get(i);
				sum += time;
			}
			return sum / amount;
		}
		return sum;
	}

	private void write() {
		try {
			FileOutputStream fileOut = new FileOutputStream(resultTitle + System.currentTimeMillis() + ".xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet worksheet = workbook.createSheet("Statistics Worksheet");

			short rowCount = 0;
			HSSFRow row1 = worksheet.createRow(rowCount);
			rowCount++;

			HSSFCell cellA1 = row1.createCell((short) 0);
			cellA1.setCellValue("Scheduling Delay");
			
			for(Long value: this.allocationTimes){
				HSSFRow row = worksheet.createRow(rowCount);
				HSSFCell cell = row1.createCell((short) 0);
				cell.setCellValue(value);
				rowCount++;
				
			}
			

			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();
			System.out.println("written file: " + fileOut);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
