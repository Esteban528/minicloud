//package com.estebandev.minicloud.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//import java.io.IOException;
//import java.nio.file.FileAlreadyExistsException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//
//import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
//import com.estebandev.minicloud.service.exception.FileNotFoundException;
//
//@SpringBootTest
//public class FileManagerServiceTest {
//
//    // @InjectMocks
//    @Autowired
//    private FileManagerService fileManagerService;
//
//    @BeforeEach
//    public void createFile() throws IOException {
//        fileManagerService.verifyRootDirectory();
//    }
//
//    @AfterEach
//    public void deleteAllFiles() throws IOException {
//        Files.deleteIfExists(fileManagerService.getRoot());
//    }
//
//    @Test
//    public void testVerifyFileManager() throws IOException {
//        fileManagerService.verifyRootDirectory();
//        Path path = fileManagerService.getRoot();
//
//        assertThat(Files.exists(path)).isTrue();
//    }
//
//    @Test
//    public void listFilesTest() throws FileNotFoundException, FileIsNotDirectoryException, IOException {
//        String fileName = "testFile.txt";
//        Path tmpPath = fileManagerService.getRoot().resolve(fileName);
//        if (!Files.exists(tmpPath))
//            Files.createFile(tmpPath);
//
//        List<Path> fmList = fileManagerService.listFiles(".");
//
//        Path path = fmList.stream()
//                .map(x -> x.getFileName())
//                .findFirst().get();
//        assertThat(path.toString()).isEqualTo(fileName);
//
//        Files.deleteIfExists(tmpPath);
//        Files.deleteIfExists(fileManagerService.getRoot());
//    }
//
//    @Test
//    public void makeDirectoryTest() throws FileAlreadyExistsException, IOException {
//        String dirNameToCreate = "testDirectory";
//
//        fileManagerService.makeDirectory(dirNameToCreate);
//
//        assertThat(Files.exists(fileManagerService.getRoot().resolve("testDirectory"))).isTrue();
//
//        Files.deleteIfExists(fileManagerService.getRoot().resolve("testDirectory"));
//    }
//
//    @Test
//    void uploadFileTest() throws IOException, FileIsNotDirectoryException, FileNotFoundException {
//        String fileName = "test-file.txt";
//        MockMultipartFile multipartFile = new MockMultipartFile(
//                "file",
//                fileName,
//                "text/plain",
//                "This is a test file".getBytes());
//
//        fileManagerService.uploadFile(multipartFile, ".");
//
//        assertThat(Files.exists(fileManagerService.getRoot().resolve(fileName))).isTrue();
//        Files.deleteIfExists(fileManagerService.getRoot().resolve(fileName));
//    }
//
//    @Test
//    public void verifyRootDirectoryWhenRootIsFile() throws IOException {
//        Path root = fileManagerService.getRoot();
//        // Eliminar el directorio creado por @BeforeEach y crear un archivo
//        Files.deleteIfExists(root);
//        Files.createFile(root);
//
//        fileManagerService.verifyRootDirectory();
//
//        assertThat(Files.isDirectory(root)).isTrue();
//    }
//
//    @Test
//    public void listFilesShouldThrowWhenPathNotExists() {
//        assertThatThrownBy(() -> fileManagerService.listFiles("nonExistentPath"))
//                .isInstanceOf(FileNotFoundException.class);
//    }
//
//    @Test
//    public void listFilesShouldThrowWhenPathIsNotDirectory() throws IOException {
//        String fileName = "file.txt";
//        Path filePath = fileManagerService.getRoot().resolve(fileName);
//        Files.createFile(filePath);
//
//        assertThatThrownBy(() -> fileManagerService.listFiles(fileName))
//                .isInstanceOf(FileIsNotDirectoryException.class);
//
//        Files.deleteIfExists(filePath);
//    }
//
//    @Test
//    public void makeDirectoryShouldThrowWhenAlreadyExists() throws IOException {
//        String dirName = "existingDir";
//        fileManagerService.makeDirectory(dirName);
//
//        assertThatThrownBy(() -> fileManagerService.makeDirectory(dirName))
//                .isInstanceOf(FileAlreadyExistsException.class);
//
//        Files.deleteIfExists(fileManagerService.getRoot().resolve(dirName));
//    }
//
//    @Test
//    public void uploadFileShouldThrowWhenDirectoryNotExists() {
//        String dirName = "nonExistentDir";
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "content".getBytes());
//
//        assertThatThrownBy(() -> fileManagerService.uploadFile(file, dirName))
//                .isInstanceOf(FileNotFoundException.class);
//    }
//
//    @Test
//    public void uploadFileShouldThrowWhenPathIsNotDirectory() throws IOException {
//        String fileName = "file.txt";
//        Path filePath = fileManagerService.getRoot().resolve(fileName);
//        Files.createFile(filePath);
//
//        MockMultipartFile multipartFile = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "content".getBytes());
//
//        assertThatThrownBy(() -> fileManagerService.uploadFile(multipartFile, fileName))
//                .isInstanceOf(FileIsNotDirectoryException.class);
//
//        Files.deleteIfExists(filePath);
//    }
//}
