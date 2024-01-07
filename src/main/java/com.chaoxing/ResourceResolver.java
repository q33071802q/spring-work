package com.chaoxing;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ResourceResolver {
    Logger logger = LoggerFactory.getLogger(getClass());
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 扫描class文件 获取所有的类
     * @param mapper
     * @return
     * @param <R>
     */
    public <R> List<R> scan(Function<Resource,R> mapper) {
        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        List<R> collector = new ArrayList<>();
        try {
            scan0(basePackagePath,path,collector,mapper);
            return collector;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        logger.info("scan path:{}",path);
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        System.out.println(en);
        while (en.hasMoreElements()){
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uriToString(uri));
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")){
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriBaseStr.startsWith("jar:")){
                scanFile(true,uriBaseStr,jarUriToPath(basePackagePath,uri),collector,mapper);
            }else {
                scanFile(false,uriBaseStr, Paths.get(uri),collector,mapper);
            }
        }
    }

    private <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file->{
            Resource res = null;
            if (isJar){
                res = new Resource(baseDir,removeLeadingSlash(file.toString()));
            }else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:"+path,name);
            }
            logger.info("found resource:{}",res);
            R r = mapper.apply(res);
            if (r!=null){
                collector.add(r);
            }
        });
    }

    /**
     * 去除头部斜杠
     * @param s
     * @return
     */
    String removeLeadingSlash(String s){
        if (s.startsWith("/") || s.startsWith("\\")){
            s = s.substring(1);
        }
        return s;
    }

    private Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        Path path = FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
        return path;
    }

    String uriToString(URI uri){
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    ClassLoader getContextClassLoader(){
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null){
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    /**
     * 去除尾部斜杠
     * @param s
     * @return
     */
    String removeTrailingSlash(String s){
        if (s.endsWith("/") || s.endsWith("\\")){
            s = s.substring(0,s.length()-1);
        }
        return s;
    }
}
