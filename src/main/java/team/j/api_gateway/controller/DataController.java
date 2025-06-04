package team.j.api_gateway.controller;

import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import team.j.api_gateway.dto.RegisteredDataDTO;
import team.j.api_gateway.service.DataService;

import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/data")
public class DataController {

    private final DataService service;

    public DataController(DataService service) {
        this.service = service;
    }

    @GetMapping("/datasets")
    public ResponseEntity<?> getRegisteredDatasetes(HttpServletRequest request) throws IOException {
        try {
            List<RegisteredDataDTO> datasets = service.getRegisteredDatasets();
            return ResponseEntity.ok(datasets);
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }

    @GetMapping("/columns/{datasetInfo}")
    public ResponseEntity<?> getColumns(@PathVariable String datasetInfo, HttpServletRequest request) {
        try {
            List<String> columns = service.getColumns(datasetInfo);
            if (columns == null || columns.isEmpty()) {
                throw new Exception("해당 데이터셋의 컬럼 정보가 없습니다.");
            }

            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .body(request.getRequestURI() + ": " + e.getMessage());
        }
    }
}
