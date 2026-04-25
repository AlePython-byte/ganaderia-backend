package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.service.CowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cows")
public class CowController {

    private final CowService cowService;

    public CowController(CowService cowService) {
        this.cowService = cowService;
    }

    @PostMapping
    public CowResponseDTO createCow(@Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.createCow(requestDTO);
    }

    @PutMapping("/{id}")
    public CowResponseDTO updateCow(@PathVariable Long id,
                                    @Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.updateCow(id, requestDTO);
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    public List<CowResponseDTO> getAllCows() {
        return cowService.getAllCows();
    }

    @GetMapping("/page")
    public Page<CowResponseDTO> getCowsPage(
            @RequestParam(required = false) CowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return cowService.getCowsPage(status, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public CowResponseDTO getCowById(@PathVariable Long id) {
        return cowService.getCowById(id);
    }

    @GetMapping("/status/{status}")
    @Deprecated(since = "2.5", forRemoval = false)
    public List<CowResponseDTO> getCowsByStatus(@PathVariable CowStatus status) {
        return cowService.getCowsByStatus(status);
    }

    @GetMapping("/token/{token}")
    public CowResponseDTO getCowByToken(@PathVariable String token) {
        return cowService.getCowByToken(token);
    }
}
