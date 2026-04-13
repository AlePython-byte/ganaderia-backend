package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.model.CowStatus;
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

    @PutMapping("/{id}")
    public CowResponseDTO updateCow(@PathVariable Long id,
                                    @Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.updateCow(id, requestDTO);
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
    public List<CowResponseDTO> getCowsByStatus(@PathVariable CowStatus status) {
        return cowService.getCowsByStatus(status);
    }

    @GetMapping("/token/{token}")
    public CowResponseDTO getCowByToken(@PathVariable String token) {
        return cowService.getCowByToken(token);
    }
}