package cloud.pposiraegi.ecommerce.global.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String uploadImage(MultipartFile file, String directory);
}
