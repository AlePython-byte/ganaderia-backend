package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.service.CollarService;
import jakarta.validation.Valid;
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

    @GetMapping
    public List<CollarResponseDTO> getAllCollars() {
        return collarService.getAllCollars();
    }

    @GetMapping("/{id}")
    public CollarResponseDTO getCollarById(@PathVariable Long id) {
        return collarService.getCollarById(id);
    }

    @GetMapping("/status/{status}")
    public List<CollarResponseDTO> getCollarsByStatus(@PathVariable String status) {
        return collarService.getCollarsByStatus(status);
    }

    @GetMapping("/identifier/{identifier}")
    public CollarResponseDTO getCollarByIdentifier(@PathVariable String identifier) {
        return collarService.getCollarByIdentifier(identifier);
    }
}