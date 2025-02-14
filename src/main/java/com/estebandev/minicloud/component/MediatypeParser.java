package com.estebandev.minicloud.component;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediatypeParser {
    static Logger logger = LoggerFactory.getLogger(MediatypeParser.class);
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("css", "text/css"),
            Map.entry("js", "text/javascript"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("xml", "text/xml"),
            Map.entry("json", "text/json"),
            Map.entry("java", "text/x-java-source"),
            Map.entry("py", "text/x-python"),
            Map.entry("rb", "text/x-ruby"),
            Map.entry("php", "text/x-php"),
            Map.entry("nix", "text/nix"),
            Map.entry("c", "text/x-csrc"),
            Map.entry("cpp", "text/x-c++src"),
            Map.entry("cs", "text/x-csharp"),
            Map.entry("swift", "text/x-swift"),
            Map.entry("ts", "text/typescript"),
            Map.entry("sql", "text/x-sql"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("webp", "image/webp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("flac", "audio/flac"),
            Map.entry("aac", "audio/aac"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/x-rar-compressed"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("exe", "application/x-msdownload"),
            Map.entry("dll", "application/x-msdownload"),
            Map.entry("dir", "files/directory"),
            Map.entry("vue", "text/x-vue"),
            Map.entry("svelte", "text/x-svelte"),
            Map.entry("astro", "text/x-astro"),
            Map.entry("scss", "text/x-scss"),
            Map.entry("sass", "text/x-sass"),
            Map.entry("less", "text/x-less"),
            Map.entry("md", "text/markdown"),
            Map.entry("mdx", "text/x-mdx"),
            Map.entry("jsx", "text/jsx"),
            Map.entry("tsx", "text/tsx"),
            Map.entry("toml", "text/x-toml"),
            Map.entry("yaml", "text/yaml"),
            Map.entry("yml", "text/yaml"),
            Map.entry("properties", "text/x-java-properties"),
            Map.entry("ini", "text/x-ini"),
            Map.entry("bat", "text/x-bat"),
            Map.entry("sh", "text/x-shellscript"),
            Map.entry("zsh", "text/x-shellscript"),
            Map.entry("fish", "text/x-shellscript"),
            Map.entry("dockerfile", "text/x-dockerfile"),
            Map.entry("env", "text/x-env"),
            Map.entry("gitignore", "text/x-gitignore"),
            Map.entry("gitattributes", "text/x-gitattributes"),
            Map.entry("editorconfig", "text/x-editorconfig"),
            Map.entry("lock", "text/x-lockfile"),
            Map.entry("lua", "text/lua"),
            Map.entry("slua", "text/lua"),
            Map.entry("bin", "application/octet-stream"));

    public static String getMediaType(String extension) {
        logger.info("Extension {}", extension);
        if (extension == null) {
            throw new IllegalArgumentException("Extension cannot be null");
        }
        return TYPES.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

}
