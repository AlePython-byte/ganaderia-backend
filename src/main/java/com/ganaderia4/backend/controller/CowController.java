package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.service.CowService;
import jakarta.validation.Valid;
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

    @GetMapping
    public List<CowResponseDTO> getAllCows() {
        return cowService.getAllCows();
    }

    @GetMapping("/{id}")
    public CowResponseDTO getCowById(@PathVariable Long id) {
        return cowService.getCowById(id);
    }

    @GetMapping("/status/{status}")
    public List<CowResponseDTO> getCowsByStatus(@PathVariable String status) {
        return cowService.getCowsByStatus(status);
    }

    @GetMapping("/identifier/{identifier}")
    public CowResponseDTO getCowByIdentifier(@PathVariable String identifier) {
        return cowService.getCowByIdentifier(identifier);
    }
}