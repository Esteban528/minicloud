package com.estebandev.minicloud.service.utils;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileData {
    String fileName;
    Path path;
    String mediaType;
    double size;
    boolean directory; 
    boolean editable;
}
