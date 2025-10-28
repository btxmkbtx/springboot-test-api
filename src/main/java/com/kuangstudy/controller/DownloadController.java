package com.kuangstudy.controller;

import com.kuangstudy.dto.TestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DownloadController {

    @PostMapping("/download/csv")
    @ResponseBody
    public ResponseEntity<byte[]> downloadCsv(
            @RequestBody TestDto testDt) throws Exception{
        HttpHeaders header = new HttpHeaders();
        header.add("Content-Type", "text/csv; charset=MS932");

        header.setContentDispositionFormData("filename", testDt.getParam1()+".csv");
        String csvData = null;
        try {
            StringBuffer csv = new StringBuffer();
            csv.append("氏名");
            csv.append(",性別");
            for (int i = 0;i<1000000;i++) {
                csv.append("\r\n");
                csv.append("千手柱間"+i);
                csv.append(",男性");
            }
            csvData=csv.toString();
        }catch (Exception e){
            System.out.println(e);
            return new ResponseEntity<>(null, header, HttpStatus.INTERNAL_SERVER_ERROR);
        };
        return new ResponseEntity<>(csvData.getBytes("MS932"), header, HttpStatus.OK);
    }
}
