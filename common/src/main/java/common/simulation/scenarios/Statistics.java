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
		if (sinlgeResourceInstance == null) {
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

	public int getAmountOfMeasurements() {
		return this.allocationTimes.size();
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
		List<Long> listToSort = new ArrayList<Long>(allocationTimes);
		Collections.sort(listToSort);
		Long percentile = new Long(0);
		int pos = (int) ((listToSort.size()/ 100.0) * 99);
		

		return listToSort.get(pos);
	}

	public void write() {
		try {
			FileOutputStream fileOut = new FileOutputStream(resultTitle
					+ System.currentTimeMillis() + ".xls");
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet worksheet = workbook.createSheet("Statistics Worksheet");

			short rowCount = 0;
			HSSFRow row1 = worksheet.createRow(rowCount);
			rowCount++;

			HSSFCell cellA1 = row1.createCell((short) 1);
			cellA1.setCellValue("Scheduling Delay");
			
			HSSFCell cellA2 = row1.createCell((short) 2);
			cellA2.setCellValue("Average");
			
			HSSFCell cellA3 = row1.createCell((short) 3);
			cellA3.setCellValue("99th Percentile");

			for (Long value : this.allocationTimes) {
				HSSFRow row = worksheet.createRow(rowCount);
				if(rowCount == 1) {
					//add avg and 99th
					HSSFCell cellB2 = row.createCell((short) 2);
					cellB2.setCellValue(getAvg());
					
					HSSFCell cellB3 = row.createCell((short) 3);
					cellB3.setCellValue(getNinetyNinth());
				}
				HSSFCell noCell = row.createCell((short) 0);
				noCell.setCellValue(rowCount);
				HSSFCell valCell = row.createCell((short) 1);
				valCell.setCellValue(value);
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
