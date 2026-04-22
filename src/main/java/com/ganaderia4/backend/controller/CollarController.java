package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.dto.DeviceSecretResponseDTO;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.service.CollarService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collars")
public class CollarController {

    private final CollarService collarService;

    public CollarController(CollarService collarService) {
        this.collarService = collarService;
    }

    @PostMapping
    public CollarResponseDTO createCollar(@Valid @RequestBody CollarRequestDTO requestDTO) {
        return collarService.createCollar(requestDTO);
    }

    @PutMapping("/{id}")
    public CollarResponseDTO updateCollar(@PathVariable Long id,
                                          @Valid @RequestBody CollarRequestDTO requestDTO) {
        return collarService.updateCollar(id, requestDTO);
    }

    @PatchMapping("/{id}/enable")
    public CollarResponseDTO enableCollar(@PathVariable Long id) {
        return collarService.enableCollar(id);
    }

    @PatchMapping("/{id}/disable")
    public CollarResponseDTO disableCollar(@PathVariable Long id) {
        return collarService.disableCollar(id);
    }

    @PatchMapping("/{id}/rotate-secret")
    public DeviceSecretResponseDTO rotateDeviceSecret(@PathVariable Long id) {
        return collarService.rotateDeviceSecret(id);
    }

    @PatchMapping("/{id}/assign/{cowId}")
    public CollarResponseDTO reassignCollar(@PathVariable Long id,
                                            @PathVariable Long cowId) {
        return collarService.reassignCollar(id, cowId);
    }

    @GetMapping
    public List<CollarResponseDTO> getAllCollars() {
        return collarService.getAllCollars();
    }

    @GetMapping("/page")
    public Page<CollarResponseDTO> getCollarsPage(
            @RequestParam(required = false) CollarStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return collarService.getCollarsPage(status, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public CollarResponseDTO getCollarById(@PathVariable Long id) {
        return collarService.getCollarById(id);
    }

    @GetMapping("/status/{status}")
    public List<CollarResponseDTO> getCollarsByStatus(@PathVariable CollarStatus status) {
        return collarService.getCollarsByStatus(status);
    }

    @GetMapping("/token/{token}")
    public CollarResponseDTO getCollarByToken(@PathVariable String token) {
        return collarService.getCollarByToken(token);
    }
}
