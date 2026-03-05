package cloud.pposiraegi.ecommerce.global.common.service;

import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Profile("!prod")
public class LocalImageStorageService implements ImageStorageService {
    @Value("${storage.location}")
    private String uploadPath;

    @Override
    public String uploadImage(MultipartFile file, String directory) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(uploadPath, directory).toAbsolutePath().normalize();

        try {
            Files.createDirectories(targetPath);
            Files.copy(file.getInputStream(), targetPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "/images/%s/%s".formatted(directory, fileName);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
