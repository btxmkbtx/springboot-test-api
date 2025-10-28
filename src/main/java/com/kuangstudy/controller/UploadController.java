package com.kuangstudy.controller;

import com.kuangstudy.Service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class UploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 文件异步上传的具体实现1
     * @param multipartFile
     * @param request
     * @return 返回上传后的资源映射链接
     */
    @PostMapping("/upload/file")
    @ResponseBody
    public String upload(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request){
        if(multipartFile.isEmpty()){
            return "文件有误";
        }

        //1:获取用户指定文件夹。
        // 这个文件夹为什么要从页面上传递过来呢？
        // 原因是：做业务隔离，不同业务文件放在不同目录下
        String dir = request.getParameter("dir");

        return uploadService.uploadImg(multipartFile, dir);
    }

    /**
     * 文件异步上传的具体实现2
     * @param multipartFile
     * @param request
     * @return 返回json格式的上传结果信息
     */
    @PostMapping("/upload/file2")
    @ResponseBody
    @CrossOrigin
    public Map<String, Object> uploadMap(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request){
        if(multipartFile.isEmpty()){
            return null;
        }

        //1:获取用户指定文件夹。
        // 这个文件夹为什么要从页面上传递过来呢？
        // 原因是：做业务隔离，不同业务文件放在不同目录下
        String dir = request.getParameter("dir");

        return uploadService.uploadImgMap(multipartFile, dir);
    }
}
