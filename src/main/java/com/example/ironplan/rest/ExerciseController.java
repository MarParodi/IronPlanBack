package com.example.ironplan.rest;

import com.example.ironplan.model.Exercise;
import com.example.ironplan.rest.dto.ExerciseCreateReq;
import com.example.ironplan.rest.dto.ExerciseUpdateReq;
import com.example.ironplan.service.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }




    @GetMapping("/{id}")
    public Exercise getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Page<Exercise> listOrSearch(@RequestParam(value = "q", required = false) String q,
                                       Pageable pageable) {
        if (q == null || q.isBlank()) {
            return service.list(pageable);
        }
        return service.searchByName(q, pageable); // o service.searchWide(q, pageable)
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise create(@Valid @RequestBody ExerciseCreateReq request) {
        return service.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Exercise update(@PathVariable Long id, @Valid @RequestBody ExerciseUpdateReq request) {
        return service.update(id, request);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam("q") String q) {
        return service.suggestNames(q);
    }

}
