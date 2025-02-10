package com.estebandev.minicloud.service.utils;

import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileData {
    String fileName;
    Path path;
    String mediaType;
    double size;
    boolean directory; 
    boolean editable;
}
