package capstone.design.api_gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiGatewayControllerTest {
    
    @Autowired
    private MockMvc mock;

    @Test
    void convertFileTest() throws Exception {
        File file = new File("/Users/jaehyeok34/Desktop/capstone-design/api_gateway/user_information_car_phone.csv");
        try (FileInputStream in = new FileInputStream(file)) {
            MockMultipartFile mf = new MockMultipartFile("file", file.getName(), "text/plain", in);

            MvcResult result = mock.perform(MockMvcRequestBuilders.multipart("/convert").file(mf))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

            ObjectMapper om = new ObjectMapper();
            Map<?, ?> responseBody = om.readValue(result.getResponse().getContentAsString(), Map.class);

            /*
             * csv 파일을 markdown 파일로 변환한 결과가 최소 500자 이상은 될 것으로 예상되기 때문에
             * 길이로 결과 검증
             */
            assertTrue(responseBody.containsKey("markdown"));
            assertTrue(((String) responseBody.get("markdown")).length() > 500);
        }
    }
}
