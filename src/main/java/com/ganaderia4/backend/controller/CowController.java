package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.service.CowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cows")
@Tag(name = "Vacas", description = "Gestión de vacas")
public class CowController {

    private final CowService cowService;

    public CowController(CowService cowService) {
        this.cowService = cowService;
    }

    @PostMapping
    @Operation(summary = "Crear vaca")
    public CowResponseDTO createCow(@Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.createCow(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Listar vacas")
    public List<CowResponseDTO> getAllCows() {
        return cowService.getAllCows();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vaca por id")
    public CowResponseDTO getCowById(@PathVariable Long id) {
        return cowService.getCowById(id);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar vacas por estado")
    public List<CowResponseDTO> getCowsByStatus(@PathVariable CowStatus status) {
        return cowService.getCowsByStatus(status);
    }

    @GetMapping("/identifier/{identifier}")
    @Operation(summary = "Buscar vaca por identificador")
    public CowResponseDTO getCowByIdentifier(@PathVariable String identifier) {
        return cowService.getCowByIdentifier(identifier);
    }
}