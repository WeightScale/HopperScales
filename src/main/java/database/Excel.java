package database;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class Excel {
    public static void export(Date fromDate, Date toDate) {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet("Отчёт");
            sheet.setColumnWidth(1, 2500);
            sheet.setColumnWidth(2, 5000);
            HSSFCellStyle style = workbook.createCellStyle();
            HSSFFont font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            int i = 0;
            Cell cell;
            Row row;
            row = sheet.createRow(i);
            cell = row.createCell(0, CellType.STRING); cell.setCellValue("Запись"); cell.setCellStyle(style);
            cell = row.createCell(1, CellType.STRING); cell.setCellValue("Терминал"); cell.setCellStyle(style);
            cell = row.createCell(2, CellType.STRING); cell.setCellValue("Дата"); cell.setCellStyle(style);
            cell = row.createCell(3, CellType.STRING); cell.setCellValue("Брутто"); cell.setCellStyle(style);
            cell = row.createCell(4, CellType.STRING); cell.setCellValue("Нетто"); cell.setCellStyle(style);
            try (Database database = new Database()) {
                for (Indication indication : database.selectIndications(fromDate, toDate)) {
                    i++;
                    row = sheet.createRow(i);
                    cell = row.createCell(0, CellType.NUMERIC); cell.setCellValue(indication.id);
                    cell = row.createCell(1, CellType.NUMERIC); cell.setCellValue(indication.node);
                    cell = row.createCell(2, CellType.STRING); cell.setCellValue(Database.DATE_FORMAT.format(indication.date));
                    cell = row.createCell(3, CellType.NUMERIC); cell.setCellValue(indication.gross);
                    cell = row.createCell(4, CellType.NUMERIC); cell.setCellValue(indication.net);
                }
            } catch (SQLException exception) {
                System.err.println("Проблема с базой данных: " + exception);
            }
            String date = Database.DATE_FORMAT.format(new Date()).replace(':', '_');
            File file = new File("reports/" + date + ".xls");
            if ((file.getParentFile().exists() && file.getParentFile().isDirectory()) || file.getParentFile().mkdirs()) {
                try (FileOutputStream outFile = new FileOutputStream(file)) {
                    workbook.write(outFile);
                } catch (IOException exception) {
                    System.err.println("Проблема с записью файла: " + exception);
                }
                System.out.println("Сгенерирован отчёт: " + file.getAbsolutePath());
            } else
                System.err.println("Не удалось создать папку для отчётов");
        } catch (IOException exception) {
            System.err.println("Проблема с формированием Excel: " + exception);
        }
    }
}
