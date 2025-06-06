package team.j.api_gateway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import team.j.api_gateway.service.CSVService;


@RestController
@RequestMapping("/csv")
public class CSVController {

    private final CSVService service;

    public CSVController(CSVService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerCSV(@RequestParam("file") MultipartFile csv, HttpServletRequest request) {
        String uri = request.getRequestURI();
        try {
            if (csv == null || csv.isEmpty()) {
                throw new Exception("csv 파일이 비어있습니다.");
            }
    
            String datasetInfo = service.registerCSV(csv); 
            System.out.println("[debug] " + datasetInfo + " 등록 완료");

            return ResponseEntity.ok(datasetInfo);
        } catch (Exception e) {
            return ResponseEntity
            .badRequest()
            .body(uri + ": " + e.getMessage());
        }
    }

    @PostMapping("/column-values/{datasetInfo}")
    public ResponseEntity<?> getColumnValues(
        @PathVariable String datasetInfo,
        @RequestBody List<String> columns,
        HttpServletRequest request
    ) {
        String uri = request.getRequestURI();
        try {
            if (columns == null || columns.isEmpty()) {
                throw new Exception("columns가 비어있거나 공백임");
            }

            Map<String, Object> columnData = service.getColumnValues(datasetInfo, columns);
            System.out.println("[debug] " + datasetInfo + "의 컬럼 데이터 요청 완료");
            
            return ResponseEntity.ok().body(columnData);
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(uri + ": " + e.getMessage());
        }
    }


    @GetMapping("/all-values/{datasetInfo}")
    public ResponseEntity<String> getAllValues(@PathVariable String datasetInfo, HttpServletRequest request) {
        try {
            String all_values = service.getAllvalues(datasetInfo);
            System.out.println("[debug] " + datasetInfo + "의 전체 데이터 요청 완료");

            return ResponseEntity.ok().body(all_values);
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }

    
    @GetMapping("/cardinality-ratio/{datasetInfo}/{column}")
    public ResponseEntity<String> getMethodName(
        @PathVariable String datasetInfo,
        @PathVariable String column,
        HttpServletRequest request
    ) {
        try {
            String cardinalityRatio = service.getCardinalityRatio(datasetInfo, column);
            System.out.println("테스트: " + cardinalityRatio);
            return ResponseEntity.ok(cardinalityRatio);
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }
    
}
