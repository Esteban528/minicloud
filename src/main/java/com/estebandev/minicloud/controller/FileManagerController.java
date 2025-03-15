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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileSecurityService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.ServiceException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/files/action")
public class FileManagerController {
    private final UserService userService;
    private final FileManagerService fileManagerService;
    private final FileSecurityService fileSecurityService;

    @GetMapping("/createIfNotExistPersonalDirectory")
    public String createIfNotExistPersonalDirectory() throws IOException {
        User user = userService.getUserFromAuth();

        try {
            fileManagerService.makeDirectory("./" + user.getEmail());
            return "redirect:/files";
        } catch (FileAlreadyExistsException e) {
            return "redirect:/files/action/go/mydir";
        }
    }

    @GetMapping("/go/mydir")
    public String redirectToUserDirectory(RedirectAttributes redirectAttributes) {
        User user = userService.getUserFromAuth();
        redirectAttributes.addAttribute("path", user.getEmail());
        return "redirect:/files/action/go/dir";
    }

    @GetMapping("/go/myshortcuts")
    public String showDirectoriesWithAccess(Model model) {
        User user = userService.getUserAllDataFromAuth();
        List<FileData> files = fileSecurityService.getFileListUserHasAccess(user);
        model.addAttribute("showrtcuts", true);
        model.addAttribute("files", files);
        return "files_shortcuts";
    }

    @GetMapping("/go/dir")
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

    @GetMapping("/go/file")
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
            model.addAttribute("backPath", (backPath == null ? path.relativize(path) : backPath));
            model.addAttribute("filePath", pathString);
            model.addAttribute("fileData", fileData);
            return "files_readFile";
        } catch (FileNotFoundException e) {
            redirectAttributes.addAttribute("path", backPath.toString());
            return "redirect:/files/action/go/dir";
        }

    }

    @GetMapping("/read")
    public ResponseEntity<Resource> readFile(
            @RequestParam(required = true, name = "path") String pathString,
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

    @GetMapping("/download")
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

    @PostMapping("/upload")
    public String uploadFilePost(@RequestParam("file") MultipartFile file,
            @RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes) throws IOException, FileNotFoundException {
        try {
            fileManagerService.uploadFile(pathString, file);
            redirectAttributes.addAttribute("path", pathString);
            return "redirect:/files/action/go/dir";
        } catch (FileIsNotDirectoryException e) {
            redirectAttributes.addAttribute("path", pathString);
            return "redirect:/files/action/go/file";
        }
    }

    @PostMapping("/mkdir")
    public String mkdir(
            @RequestParam @Valid @Pattern(regexp = "^[a-zA-Z0-9_!#$%^&()@+.-]+$", message = "Illegal symbols") String name,
            @RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes)
            throws IOException, IllegalArgumentException {

        fileManagerService.makeDirectory(pathString, name);
        redirectAttributes.addAttribute("path", pathString);
        return "redirect:/files/action/go/dir";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam(required = true, name = "path") String pathString,
            RedirectAttributes redirectAttributes)
            throws IOException, FileNotFoundException {

        fileManagerService.delete(pathString);
        redirectAttributes.addAttribute("path", FileManagerUtils.getParent(pathString).toString());
        return "redirect:/files/action/go/dir";
    }

    @PostMapping("/rename")
    public String rename(@RequestParam(required = true, name = "path") String pathString,
            @RequestParam(required = true) @Valid @Pattern(regexp = "^[a-zA-Z0-9_!#$%^&()@+.-]+$", message = "Illegal symbols") String newName,
            RedirectAttributes redirectAttributes)
            throws IOException, FileNotFoundException {

        Path newPath = fileManagerService.rename(pathString, newName);
        redirectAttributes.addAttribute("path", newPath.toString());
        return "redirect:/files/action/go/dir";
    }

    @GetMapping("/manage/access")
    public String accesDashboard(
            @RequestParam(required = true, name = "path") String pathString,
            Model model)
            throws FileIsNotDirectoryException, IOException {
        var usersWithAccess = fileSecurityService.getUserWithAccessTo(pathString);

        model.addAttribute("onboardMaccess", true);
        model.addAttribute("usersWithAccess", usersWithAccess);
        return "files_accessManager";
    }

    @PostMapping("/manage/access/grant")
    public String grantAccessTo(
            @RequestParam(required = true, name = "path") String pathString,
            @RequestParam(required = true) String email,
            RedirectAttributes redirectAttributes,
            Model model) throws FileIsNotDirectoryException, IOException {

        try {
            fileSecurityService.grantAccess(pathString, email);
        } catch (UsernameNotFoundException e) {
            model.addAttribute("error", "The user is not registered or does not exist.");
        } catch (ServiceException e) {
            model.addAttribute("error", e.getMessage());
        }
        return accesDashboard(pathString, model);
    }

    @PostMapping("/manage/access/revoke")
    public String revokeAccessTo(
            @RequestParam(required = true, name = "path") String pathString,
            @RequestParam(required = true) String email,
            RedirectAttributes redirectAttributes,
            Model model) throws FileIsNotDirectoryException, IOException {

        try {
            fileSecurityService.revokeAccess(pathString, email);
        } catch (UsernameNotFoundException | ServiceException e) {
            model.addAttribute("error", e.getMessage());
        }
        return accesDashboard(pathString, model);
    }

}
