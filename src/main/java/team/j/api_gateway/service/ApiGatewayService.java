package team.j.api_gateway.service;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApiGatewayService {

    public String home() {
        return "API Gateway is running";
    }

    public void saveCSV(MultipartFile csv) throws IOException {
        // String filePath = "./csv/" + csv.getOriginalFilename();
        File dir = new File("./csv/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // dir에 csv파일 저장
        String filePath = dir.getAbsolutePath() + File.separator + csv.getOriginalFilename();
        File file = new File(filePath);

        csv.transferTo(file);
    }
}
