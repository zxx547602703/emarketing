package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.wiko.emarket.dao.AttachmentInfoDao;
import com.wiko.emarket.entity.AttachmentInfo;
import com.wiko.emarket.vo.AttachmentInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * emarket 附件上传
 */
@RestController
@RequestMapping("/api/file")
@Slf4j
public class FileController {
    private static final String SERVER_FILE_UPLOAD_DIR = "emarket";
    @Value("${attachment_root}")
    private String attachmentRoot;

    @Value("${spring.servlet.multipart.location}")
    private String tmpDir;

    @Autowired
    private Environment environment;

    @Autowired
    private AttachmentInfoDao attachmentInfoDao;

    /**
     * 查询附件
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        String refId = (String) params.get("refId");
        Map<String, Object> mapperParams = new HashMap<>();
        mapperParams.put("ref_id", refId);
        List<AttachmentInfo> fileList = attachmentInfoDao.selectByMap(mapperParams);
        if (CollectionUtils.isEmpty(fileList)) {
            return R.ok().put("page", Collections.emptyList());
        }
        fileList = fileList.stream()
                .sorted(Comparator.comparing(AttachmentInfo::getCreatedDate).reversed()).collect(Collectors.toList());
        return R.ok().put("page", fileList);
    }

    /**
     * 上传附件
     *
     * @param multipartFile MultipartFile
     * @return R
     */
    @RequestMapping("/upload")
    public R upload(@RequestParam(value = "file", required = false) MultipartFile multipartFile) {
        // 创建File对象
        log.info("获取配置， 临时路径:{}", tmpDir);
        File file1 = new File(tmpDir);
        if(file1.isDirectory()) { // 判断File对象对应的目录是否存在
            String[] names = file1.list(); // 获得目录下的所有文件的文件名
            if (names != null) {
                for (String name : names) {
                    log.info("上传临时文件：{}", name);  //输出文件名
                }
            }
        } else {
            log.error("上传临时文件, 目录不存在");
        }
        try {
            R checkRes = checkUpload(multipartFile);
            if (checkRes !=null) {
                return checkRes;
            }

            String originalFilename = multipartFile.getOriginalFilename();
            // 文件前缀
            String prefix = FilenameUtils.getBaseName(originalFilename);
            // 文件后缀
            String suffix = FilenameUtils.getExtension(originalFilename);
            // 重构文件名， 防止重复
            String filename = prefix + "_" + System.currentTimeMillis() + "." + suffix;

            //服务器保存文件路径template, 如果子目录不存在，则创建
            String filePath = attachmentRoot + "/"  + SERVER_FILE_UPLOAD_DIR;
            File pfile = new File(filePath);
            if (!pfile.exists()) {
                pfile.mkdirs();
            }

            //获取文件字节数组
            byte[] bytes = multipartFile.getBytes();

            //写入指定文件夹
            File file = new File(pfile, filename);
            FileOutputStream fop = new FileOutputStream(file);
            fop.write(bytes);
            fop.flush();
            fop.close();

            // 返回文件信息
            AttachmentInfoVo attachmentInfoVo = buildAttachmentInfo(originalFilename, filename);
            return R.ok().put("attachment", attachmentInfoVo);
        } catch (Exception e) {
            log.error("upload error", e);
            return R.error(500, "文件上传失败");
        }
    }

    private R checkUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.error(500, "文件为空");
        }
        if (file.getOriginalFilename().length() > 50) {
            return R.error(500, "文件名不能超过50长度");
        }
        return null;
    }

    private AttachmentInfoVo buildAttachmentInfo(String originalFilename, String filename) {
        AttachmentInfoVo attachmentInfoVo = new AttachmentInfoVo();
        attachmentInfoVo.setAttachmentName(originalFilename);
        attachmentInfoVo.setFilePath(attachmentRoot + "/"  + SERVER_FILE_UPLOAD_DIR + "/" + filename);
        return attachmentInfoVo;
    }

    @RequestMapping("/download")
    private void downloadFile(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        String originalFilename = URLDecoder.decode(request.getParameter("originalFilename"),"UTF-8");
        String serveFilepath = URLDecoder.decode(request.getParameter("serveFilepath"),"UTF-8");

        ServletOutputStream out = null;
        FileInputStream ips = null;
        try {
            // 获取服务器上保存的文件
            File file = new File(serveFilepath);
            // 获取文件名称
            String filename = file.getName();
            ips = new FileInputStream(file);

            //设置Http响应头告诉浏览器下载这个附件,下载的文件名也是在这里设置的
            originalFilename = java.net.URLEncoder.encode(originalFilename,"UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + originalFilename);

            //设置字符集
            response.setCharacterEncoding("UTF-8");
            // form表单则会自动下载
            response.setContentType("multipart/form-data");
            out = response.getOutputStream();
            //读取文件流
            int len = 0;
            byte[] buffer = new byte[1024 * 10];
            while ((len = ips.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("downloadFile error", e);
        } finally {
            closeStream(out, ips);
        }
    }

    private void closeStream(ServletOutputStream out, FileInputStream ips) {
        try {
            if (out != null) {
                out.close();
            }
            if (ips != null) {
                ips.close();
            }
        } catch (IOException e) {
            log.error("closeStream error", e);
        }
    }
}
