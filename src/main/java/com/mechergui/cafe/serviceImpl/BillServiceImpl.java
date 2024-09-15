package com.mechergui.cafe.serviceImpl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64;
import com.mechergui.cafe.JWT.JwtFilter;
import com.mechergui.cafe.POJO.Bill;
import com.mechergui.cafe.constants.CafeConstants;
import com.mechergui.cafe.dao.BillDao;
import com.mechergui.cafe.service.BillService;
import com.mechergui.cafe.utils.CafeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.DOMException;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class BillServiceImpl implements BillService {

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    BillDao billDao;

    @Override
    public ResponseEntity<String> generateReport(Map<String, Object> requestMap) {
        log.info("Inside generate report");
        try {
            String fileName;
            if (validateRequestMap(requestMap)) {
                if (requestMap.containsKey("isGenerate") && !(Boolean) requestMap.get("isGenerate")) {
                    fileName = (String) requestMap.get("uuid");
                } else {
                    fileName = CafeUtils.getUUID();
                    requestMap.put("uuis", fileName);
                    insertBill(requestMap);
                }
                String data = "Name: " + requestMap.get("name") + "\n" +
                        "ContactNumber: " + requestMap.get("contactNumber") + "\n" +
                        "Email: " + requestMap.get("email") + "\n" +
                        "Payement Method: " + requestMap.get("payementMethod");

                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                PdfWriter.getInstance(document, new FileOutputStream(CafeConstants.STORE_LOCATION + "\\" + fileName + ".pdf"));
                document.open();
                setRectangleInPdf(document);

                Paragraph chunk = new Paragraph("Cafe Management System", getFont("Header"));
                chunk.setAlignment(Element.ALIGN_CENTER);
                document.add(chunk);
                Paragraph paragraph = new Paragraph(data + "\n \n", getFont("Data"));
                document.add(paragraph);
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                addTableHeaders(table);
                JSONArray jsonArray = CafeUtils.getJsonArrayFromString((String) requestMap.get("productDetails"));
                for (int i = 0; i < jsonArray.length(); i++) {
                   addRows(table,CafeUtils.getMapFromJson(jsonArray.getString(i)));
                }
                document.add(table);
                Paragraph footer=new Paragraph("Totam: " + requestMap.get("totalAmount") + "\n"
                + "Thank you for visiting ! visit again" ,getFont("Data"));
                document.add(footer);
                document.close();
                return new ResponseEntity<>("{\"uuid\":\""+fileName+"\"}",HttpStatus.OK);


            }
            return CafeUtils.getResponseEntity("requered data not found", HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<Bill>> getBills() {
        List<Bill> bills=new ArrayList<>();
        if (jwtFilter.isAdmin()){
           bills=billDao.getAllBills();
        }else {
           bills=billDao.getBillsByUserName(jwtFilter.getCurrentUser());
        }
        return new ResponseEntity<>(bills,HttpStatus.OK);
    }

    @Override
    public ResponseEntity<byte[]> getPdf(Map<String, Object> requestMap) {
        log.info("inside get pdf : requestMap {}",requestMap);
        try {
          byte[] byteArray=new byte[0];
          if (!requestMap.containsKey("uuid") && validateRequestMap(requestMap))
              return new ResponseEntity<>(byteArray,HttpStatus.BAD_REQUEST);
          String filePath=CafeConstants.STORE_LOCATION+ "\\" +(String) requestMap.get("uuid")+".pdf";
          if (CafeUtils.isFileExist(filePath)){
              byteArray=getByteArray(filePath);
              return new ResponseEntity<>(byteArray,HttpStatus.OK);
          }else {
              requestMap.put("isGenerate",false);
              generateReport(requestMap);
              byteArray=getByteArray(filePath);
              return new ResponseEntity<>(byteArray,HttpStatus.OK);
          }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;

    }

    @Override
    public ResponseEntity<String> deleteBill(Integer id) {
        try {
            Optional optional=billDao.findById(id);
            if (!optional.isEmpty()){
              billDao.deleteById(id);
                return CafeUtils.getResponseEntity("Bill deleted successfully", HttpStatus.OK);

            }
            return CafeUtils.getResponseEntity("Bill id does not exist",HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    private byte[]  getByteArray(String filePath) throws Exception {
        File file=new File(filePath);
        InputStream stream=new FileInputStream(file);

        byte[] bytes= IOUtils.toByteArray(stream);
        stream.close();
        return bytes;
    }

    private void addRows(PdfPTable table, Map<String, Object> data) {
        log.info("inside add rows");
        table.addCell((String) data.get("name"));
        table.addCell((String) data.get("category"));
        table.addCell((String) data.get("quantity"));
        table.addCell((Double.toString((Double)data.get("price"))));
        table.addCell((Double.toString((Double)data.get("total"))));

    }

    private void addTableHeaders(PdfPTable table) {
        log.info("inside add table header");
        Stream.of("Name", "Category", "Quantity", "Price", "Sub Total")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    header.setBackgroundColor(BaseColor.YELLOW);
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    header.setVerticalAlignment(Element.ALIGN_CENTER);
                    table.addCell(header);


                });
    }

    private Font getFont(String type) {
        log.info("inside get font");
        switch (type) {
            case "Header":
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 18, BaseColor.BLACK);
                headerFont.setStyle(Font.BOLD);
                return headerFont;
            case "Data":
                Font dataFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, BaseColor.BLACK);
                dataFont.setStyle(Font.BOLD);
                return dataFont;
            default:
                return new Font();
        }
    }

    private void setRectangleInPdf(Document document) throws DocumentException {
        log.info("Inside setRectangleInPdf");
        Rectangle rect = new Rectangle(577, 825, 18, 15);
        rect.enableBorderSide(1);
        rect.enableBorderSide(2);
        rect.enableBorderSide(4);
        rect.enableBorderSide(8);
        rect.setBorderColor(BaseColor.BLACK);
        rect.setBorderWidth(1);
        document.add(rect);
    }

    private void insertBill(Map<String, Object> requestMap) {
        try {
            Bill bill = new Bill();
            bill.setUuid((String) requestMap.get("uuid"));
            bill.setName((String) requestMap.get("name"));
            bill.setEmail((String) requestMap.get("email"));
            bill.setContactNumber((String) requestMap.get("contactNumber"));
            bill.setPayementMethod((String) requestMap.get("payementMethod"));
            bill.setTotal(Integer.parseInt((String) requestMap.get("totalAmount")));
            bill.setProductDetails((String) requestMap.get("productDetails"));
            bill.setCreatedBy(jwtFilter.getCurrentUser());
            billDao.save(bill);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateRequestMap(Map<String, Object> requestMap) {
        return requestMap.containsKey("name") &&
                requestMap.containsKey("contactNumber") &&
                requestMap.containsKey("email") &&
                requestMap.containsKey("payementMethod") &&
                requestMap.containsKey("productDetails") &&
                requestMap.containsKey("totalAmount");
    }
}
