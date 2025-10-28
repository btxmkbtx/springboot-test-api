package com.kuangstudy.controller;

import com.kuangstudy.dto.TestDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Controller
public class PageController {
    @GetMapping("/upload")
    public String upload(){
        return "upload";
    }

    @PostMapping("/ping/post")
    @ResponseBody
    public String testPing() {
        System.out.println("post from axios success");
        return "";
    }

    @PostMapping("/ping/post/param")
    @ResponseBody
    public TestDto testPostWithParam(@RequestBody TestDto testDto) {
        System.out.println("post with param from axios success");
        System.out.println("param is:" + testDto.toString());
        testDto.setParam2("I got it");
        testDto.setParamDate(new Date());
        testDto.setParamInt(1);
        return testDto;
    }
}
