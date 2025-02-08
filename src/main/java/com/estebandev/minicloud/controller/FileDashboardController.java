package com.estebandev.minicloud.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileDashboardController {
    private final UserService userService;
    private final FileManagerService fileManagerService;

    @GetMapping()
    public String showDashboard(Model model) {
        return "redirect:/files/action/createIfNotExistPersonalDirectory";
    }

    @GetMapping("/action/createIfNotExistPersonalDirectory")
    public String createIfNotExistPersonalDirectory() throws IOException {
        User user = userService.getUserFromAuth();

        try {
            fileManagerService.makeDirectory("./" + user.getEmail());
            return "redirect:/files";
        } catch (FileAlreadyExistsException e) {
            return "redirect:/files/action/go/mydir";
        }
    }

    @GetMapping("/action/go/mydir")
    public String redirectToUserDirectory(RedirectAttributes redirectAttributes) {
        User user = userService.getUserFromAuth();
        redirectAttributes.addAttribute("path", user.getEmail());
        return "redirect:/files/action/go/dir";
    }

    @GetMapping("/action/go/dir")
    public String goToDir(@RequestParam(defaultValue = ".", name = "path") String pathString,
            RedirectAttributes redirectAttributes,
            Model model,
            WebRequest webRequest)
            throws IOException, FileNotFoundException {

        try {
            List<FileData> fileList = fileManagerService.listFiles(pathString);
            Path path = fileManagerService.getRoot().resolve(pathString).normalize();
            Path backPath = fileManagerService.getRoot().relativize(path).getParent();
            model.addAttribute("fileList", fileList);
            model.addAttribute("fileData", fileManagerService.findFileData(pathString));
            model.addAttribute("path", fileManagerService.getRoot().relativize(path).toString());
            model.addAttribute("backPath", backPath == null ? path.relativize(path) : backPath);
            return "files_listdirectory";
        } catch (FileIsNotDirectoryException e) {
            redirectAttributes.addAttribute("path", pathString);
            return "redirect:/files/action/go/file";
        }
    }

    @GetMapping("/action/go/file")
    public String goToFile(@RequestParam(name = "path", required = true) String pathString,
            Model model,
            WebRequest webRequest,
            RedirectAttributes redirectAttributes)
            throws IOException {

        Path path = fileManagerService.getRoot().resolve(pathString).normalize();
        Path backPath = fileManagerService.getRoot().relativize(path).getParent();
        try {
            FileData fileData = fileManagerService.findFileData(pathString);
            model.addAttribute("path", fileManagerService.getRoot().relativize(path).toString());
            model.addAttribute("backPath", backPath == null ? path.relativize(path) : backPath);
            model.addAttribute("filePath", pathString);
            model.addAttribute("fileData", fileData);
            return "files_readFile";
        } catch (FileNotFoundException e) {
            redirectAttributes.addAttribute("path", backPath);
            return "redirect:/files/action/go/dir";
        }

    }

    @GetMapping("/action/read")
    public ResponseEntity<Resource> readFile(
            @RequestParam(required = true, name = "path") String pathString,
            WebRequest webRequest,
            RedirectAttributes redirectAttributes) throws IOException {

        try {
            Resource resource = fileManagerService.findFile(pathString);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileManagerService.getMimeType(pathString)))
                    .body(resource);
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    @GetMapping("/action/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(required = true, name = "path") String pathString) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        Resource resource;
        try {
            resource = fileManagerService.findFile(pathString);
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        }
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename());

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @PostMapping("/action/upload")
    public String uploadFilePost(@RequestParam("file") MultipartFile file,
            @RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes) throws IOException, FileNotFoundException {
        try {
            fileManagerService.uploadFile(file, pathString);
            redirectAttributes.addAttribute("path", pathString);
            return "redirect:/files/action/go/dir";
        } catch (FileIsNotDirectoryException e) {
            redirectAttributes.addAttribute("path", pathString);
            return "redirect:/files/action/go/file";
        }
    }

    @PostMapping("/action/mkdir")
    public String mkdir(
            @RequestParam @Valid @Pattern(regexp = "^[a-zA-Z0-9_!#$%^&()@+.-]+$", message = "Illegal symbols") String name,
            @RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes)
            throws IOException, IllegalArgumentException {

        fileManagerService.makeDirectory(pathString, name);
        redirectAttributes.addAttribute("path", pathString);
        return "redirect:/files/action/go/dir";
    }

    @PostMapping("/action/delete")
    public String delete(@RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes)
            throws IOException, FileNotFoundException {

        fileManagerService.delete(pathString);
        redirectAttributes.addAttribute("path", FileManagerUtils.getParent(pathString).toString());
        return "redirect:/files/action/go/dir";
    }

    @PostMapping("/action/rename")
    public String rename(@RequestParam(required = true, name = "path") String pathString,
            @RequestParam(required = true) @Valid @Pattern(regexp = "^[a-zA-Z0-9_!#$%^&()@+.-]+$", message = "Illegal symbols") String newName,
            RedirectAttributes redirectAttributes)
            throws IOException, FileNotFoundException {

        Path newPath = fileManagerService.rename(pathString, newName);
        redirectAttributes.addAttribute("path", newPath.toString());
        return "redirect:/files/action/go/dir";
    }

    @GetMapping("/error")
    public String showErrorPopup() {
        return "files_error";
    }
}
