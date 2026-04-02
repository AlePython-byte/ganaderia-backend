package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.service.CollarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collars")
@Tag(name = "Collares", description = "Gestión de collares")
public class CollarController {

    private final CollarService collarService;

    public CollarController(CollarService collarService) {
        this.collarService = collarService;
    }

    @PostMapping
    @Operation(summary = "Crear collar")
    public CollarResponseDTO createCollar(@Valid @RequestBody CollarRequestDTO requestDTO) {
        return collarService.createCollar(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Listar collares")
    public List<CollarResponseDTO> getAllCollars() {
        return collarService.getAllCollars();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar collar por id")
    public CollarResponseDTO getCollarById(@PathVariable Long id) {
        return collarService.getCollarById(id);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar collares por estado")
    public List<CollarResponseDTO> getCollarsByStatus(@PathVariable CollarStatus status) {
        return collarService.getCollarsByStatus(status);
    }

    @GetMapping("/identifier/{identifier}")
    @Operation(summary = "Buscar collar por identificador")
    public CollarResponseDTO getCollarByIdentifier(@PathVariable String identifier) {
        return collarService.getCollarByIdentifier(identifier);
    }
}