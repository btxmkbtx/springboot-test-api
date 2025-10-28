package com.kuangstudy.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {

    @Value("${file.uploadFolder}")
    private String uploadFolder;

    @Value("${file.staticPath}")
    private String staticPath;

    @Value("${file.staticPatternPath}")
    private String staticPatternPath;

    /**
     * MultipartFile 这个关键的对象是SpringMVC提供的文件上传的接受类
     * 它的底层自动会去和HttpServletRequest中的getInputStream()方法进行融合
     * 从而到达文件上传的效果。也就是告诉你一个道理：文件上传的底层原理就是「请求流」request.getInputStream()
     * @param multipartFile
     * @param dir
     * @return
     */
    public String uploadImg(MultipartFile multipartFile, String dir){

        try {

            //step1:生成上传后的唯一文件名，注意服务器上的文件名一定要用英文，这是世界标准
            String originalFileName = multipartFile.getOriginalFilename();
            String fileSuffix = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString()+fileSuffix;

            //step2:指定文件上传的目标目录
            String serverRootPath = uploadFolder;//注意：服务器上传文件的根目录需要在配置类中做资源映射，才能让外部访问被上传的资源
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateDir = dateFormat.format(new Date());
            File targetFileDir = new File(serverRootPath+dir, dateDir);
            if (!targetFileDir.exists()){
                targetFileDir.mkdirs();
            }

            // step3:将文件流中的内容输出到指定目录，生成被上传的文件
            File targetFile = new File(targetFileDir, uniqueFileName);
            multipartFile.transferTo(targetFile);

            String fileName = dir + "/" + dateDir + "/" + uniqueFileName;
            return staticPath + staticPatternPath.replace("*", "") + "/" + fileName;

        } catch (IOException e) {
            e.printStackTrace();
            return "upload failed";
        }

    }

    public Map<String, Object> uploadImgMap(MultipartFile multipartFile, String dir){

        try {

            //step1:生成上传后的唯一文件名，注意服务器上的文件名一定要用英文，这是世界标准
            String originalFileName = multipartFile.getOriginalFilename();
            String fileSuffix = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString()+fileSuffix;

            //step2:指定文件上传的目标目录
            String serverRootPath = uploadFolder;//注意：服务器上传文件的根目录需要在配置类中做资源映射，才能让外部访问被上传的资源
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateDir = dateFormat.format(new Date());
            File targetFileDir = new File(serverRootPath+dir, dateDir);
            if (!targetFileDir.exists()){
                targetFileDir.mkdirs();
            }

            // step3:将文件流中的内容输出到指定目录，生成被上传的文件
            File targetFile = new File(targetFileDir, uniqueFileName);
            multipartFile.transferTo(targetFile);

            String fileName = dir + "/" + dateDir + "/" + uniqueFileName;

            Map<String, Object> map = new HashMap<>();
            map.put("url", staticPath + staticPatternPath.replace("*", "") + "/" + fileName);
            map.put("size", multipartFile.getSize());
            //map.put("还想返回什么自己div吧", anything);
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
